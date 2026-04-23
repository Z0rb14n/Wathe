package dev.doctor4t.wathe.stats;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates per-round and global per-player stats. Hook points:
 * <ul>
 *   <li>{@link #startRound(ServerWorld, List)} from {@link GameFunctions#initializeGame(ServerWorld)}</li>
 *   <li>{@link #recordKill(PlayerEntity, PlayerEntity, Identifier)} from {@link GameFunctions#killPlayer}</li>
 *   <li>{@link #endRound(ServerWorld, GameFunctions.WinStatus)} from
 *       {@code GameRoundEndComponent.setRoundEndData}</li>
 *   <li>{@link #recordEarned(PlayerEntity, int)} from money credit paths</li>
 *   <li>{@link #recordPurchase(PlayerEntity, Identifier, int)} from shop buy</li>
 * </ul>
 *
 * Files are written under {@code config/wathe/stats/}:
 * <ul>
 *   <li>{@code rounds/<timestamp>_<map>_<gamemode>.json} — one per round</li>
 *   <li>{@code players/<uuid>.json} — running totals per player</li>
 * </ul>
 */
public final class WatheStats {
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC);

    private static final Path STATS_ROOT =
            FabricLoader.getInstance().getConfigDir().resolve("wathe").resolve("stats");
    private static final Path ROUNDS_DIR = STATS_ROOT.resolve("rounds");
    private static final Path PLAYERS_DIR = STATS_ROOT.resolve("players");

    private static final com.google.gson.Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<RegistryKey<World>, RoundStats> ACTIVE = new ConcurrentHashMap<>();

    private WatheStats() {
    }

    /**
     * Initializes round tracking for {@code world}, snapshotting each player's role
     * after roles have been assigned.
     */
    public static void startRound(@NotNull ServerWorld world, @NotNull List<ServerPlayerEntity> players) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Identifier gameModeId = game.getGameMode() == null ? null : game.getGameMode().identifier;
        Identifier mapEffectId = game.getMapEffect() == null ? null : game.getMapEffect().identifier;
        RoundStats round = new RoundStats(
                world.getRegistryKey().getValue().toString(),
                gameModeId,
                mapEffectId,
                world.getTime()
        );
        for (ServerPlayerEntity player : players) {
            Role role = game.getRole(player);
            if (role == null) continue;
            round.registerPlayer(player.getUuid(), player.getGameProfile().getName(), role);
        }
        ACTIVE.put(world.getRegistryKey(), round);
    }

    /**
     * Records a kill that just occurred. Call after the victim has been confirmed-killed
     * (i.e. after their gamemode swap). Both {@code killer} and {@code victim} may live in
     * the same world; if no active round is tracked there the call is a no-op.
     */
    public static void recordKill(@NotNull PlayerEntity victim, @Nullable PlayerEntity killer, @NotNull Identifier reason) {
        if (!(victim.getWorld() instanceof ServerWorld serverWorld)) return;
        RoundStats round = ACTIVE.get(serverWorld.getRegistryKey());
        if (round == null) return;
        UUID killerUuid = killer == null ? null : killer.getUuid();
        String killerName = killer == null ? null : killer.getGameProfile().getName();
        round.recordKill(killerUuid, killerName, victim.getUuid(),
                victim.getGameProfile().getName(), reason, serverWorld.getTime());
    }

    public static void recordEarned(@NotNull PlayerEntity player, int amount) {
        if (amount <= 0) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        RoundStats round = ACTIVE.get(serverWorld.getRegistryKey());
        if (round == null) return;
        round.recordEarned(player.getUuid(), amount);
    }

    public static void recordPurchase(@NotNull PlayerEntity player, @NotNull Identifier item, int price) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        RoundStats round = ACTIVE.get(serverWorld.getRegistryKey());
        if (round == null) return;
        round.recordPurchase(player.getUuid(), item, price, serverWorld.getTime());
    }

    /**
     * Finalizes the active round in {@code world}, writes the per-round file and updates
     * each tracked player's global file. Safe to call multiple times — only the first call
     * per round actually writes.
     */
    public static void endRound(@NotNull ServerWorld world, @NotNull GameFunctions.WinStatus winStatus) {
        RoundStats round = ACTIVE.remove(world.getRegistryKey());
        if (round == null) return;

        UUID looseEndWinner = GameWorldComponent.KEY.get(world).getLooseEndWinner();
        round.finalizeRound(winStatus, looseEndWinner, world.getTime());

        try {
            writeRoundFile(round);
        } catch (IOException e) {
            Wathe.LOGGER.error("Failed to write round stats file: {}", e.getMessage());
        }

        for (RoundStats.PlayerEntry entry : round.getEntries().values()) {
            try {
                PlayerGlobalStats global = loadOrCreatePlayer(entry.uuid);
                global.applyRound(entry);
                writePlayerFile(global);
            } catch (IOException e) {
                Wathe.LOGGER.error("Failed to update global stats for {}: {}", entry.uuid, e.getMessage());
            }
        }
    }

    /**
     * Discards an in-progress round without writing files. Used if the game is force-stopped
     * before any winner is determined.
     */
    public static void discardRound(@NotNull ServerWorld world) {
        ACTIVE.remove(world.getRegistryKey());
    }

    /**
     * Mirrors the win logic in {@code GameRoundEndComponent.didWin} but operates on a
     * {@link Role} directly so we can resolve it from authoritative game state.
     */
    public static boolean didPlayerWin(@NotNull Role role, @NotNull UUID uuid,
                                       @NotNull GameFunctions.WinStatus winStatus,
                                       @Nullable UUID looseEndWinner) {
        return switch (winStatus) {
            case NONE -> false;
            case KILLERS -> role == WatheRoles.KILLER;
            case PASSENGERS, TIME -> role != WatheRoles.KILLER;
            case LOOSE_END -> looseEndWinner != null && looseEndWinner.equals(uuid);
        };
    }

    private static void writeRoundFile(RoundStats round) throws IOException {
        Files.createDirectories(ROUNDS_DIR);
        String time = FILE_TIMESTAMP.format(round.getStartedAt());
        String map = sanitize(round.getWorldId());
        String mode = round.getGameMode() == null ? "unknown" : sanitize(round.getGameMode().getPath());
        Path file = ROUNDS_DIR.resolve(time + "_" + map + "_" + mode + ".json");
        Files.writeString(file, GSON.toJson(round.toJson()));
    }

    private static PlayerGlobalStats loadOrCreatePlayer(UUID uuid) throws IOException {
        Files.createDirectories(PLAYERS_DIR);
        Path file = PLAYERS_DIR.resolve(uuid + ".json");
        if (!Files.exists(file)) return new PlayerGlobalStats(uuid);
        try {
            JsonObject parsed = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return PlayerGlobalStats.fromJson(uuid, parsed);
        } catch (Exception e) {
            Wathe.LOGGER.warn("Could not parse stats for {}, starting fresh: {}", uuid, e.getMessage());
            return new PlayerGlobalStats(uuid);
        }
    }

    private static void writePlayerFile(PlayerGlobalStats stats) throws IOException {
        Files.createDirectories(PLAYERS_DIR);
        Path file = PLAYERS_DIR.resolve(stats.uuid + ".json");
        Files.writeString(file, GSON.toJson(stats.toJson()));
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Test/debug aid — exposed for unit testing of timestamp formatting. */
    static String formatTimestamp(Instant instant) {
        return FILE_TIMESTAMP.format(instant);
    }
}
