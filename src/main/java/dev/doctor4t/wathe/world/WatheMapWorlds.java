package dev.doctor4t.wathe.world;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.WatheMapEffects;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.mixin.MinecraftServerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;

public class WatheMapWorlds {
    public static final String MAP_KEY_NAMESPACE = "wathe";
    public static final String MAP_KEY_PREFIX = "map/";
    private static final Path STATE_FILE = FabricLoader.getInstance().getConfigDir().resolve("wathe").resolve("server-state.json");
    private static final Gson GSON = new Gson();

    private static String currentMapName = null;

    private static final WorldGenerationProgressListener NOOP_LISTENER = new WorldGenerationProgressListener() {
        @Override
        public void start(ChunkPos spawnPos) {
        }

        @Override
        public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    /**
     * Called on SERVER_STARTED. Loads the last used map world and teleports all players there.
     * If no last map is recorded, does nothing and players stay in the hub.
     */
    public static void autoLoad(MinecraftServer server) {
        String lastName = readLastMapName();
        if (lastName == null) return;
        File folder = server.getRunDirectory().resolve(lastName).toFile();
        if (!folder.isDirectory() || !new File(folder, "level.dat").exists()) {
            Wathe.LOGGER.warn("Last map world '{}' no longer exists, skipping auto-load.", lastName);
            return;
        }
        try {
            ServerWorld world = load(server, lastName);
            currentMapName = lastName;
            teleportAll(server, world);

            // Initialize lobby map effects (visuals, time of day, etc.)
            WatheMapEffects.HARPY_EXPRESS_LOBBY.initializeMapEffects(world, world.getPlayers());
        } catch (Exception e) {
            Wathe.LOGGER.error("Failed to auto-load last map world '{}': {}", lastName, e.getMessage());
        }
    }

    /**
     * Scans the server root and returns folder names of available (not necessarily loaded) map worlds, including the hub world.
     */
    public static List<String> scan(MinecraftServer server) {
        File[] dirs = server.getRunDirectory().toFile().listFiles(f -> f.isDirectory() && new File(f, "level.dat").exists());
        if (dirs == null) return List.of();
        return Arrays.stream(dirs).map(File::getName).sorted().toList();
    }

    /**
     * Loads a world folder from the server root as a new ServerWorld on demand.
     */
    public static ServerWorld load(MinecraftServer server, String folderName) throws IOException {
        RegistryKey<World> worldKey = keyFor(folderName);
        Map<RegistryKey<World>, ServerWorld> worlds = ((MinecraftServerAccessor) server).getWorlds();

        if (worlds.containsKey(worldKey)) return worlds.get(worldKey);

        Executor workerExecutor = ((MinecraftServerAccessor) server).getWorkerExecutor();
        LevelStorage.Session session = LevelStorage.create(server.getRunDirectory()).createSessionWithoutSymlinkCheck(folderName);

        ServerWorld overworld = server.getOverworld();
        DimensionOptions dimensionOptions = new DimensionOptions(overworld.getDimensionEntry(), overworld.getChunkManager().getChunkGenerator());

        ServerWorld mapWorld = new ServerWorld(server, workerExecutor, session, new MapWorldProperties(folderName, overworld.getGameRules(), server.getSaveProperties().getDifficulty()), worldKey, dimensionOptions, NOOP_LISTENER, false, overworld.getSeed(), Collections.emptyList(), true, null);

        worlds.put(worldKey, mapWorld);

        // Force-load spawn chunks, same as vanilla does for the overworld.
        // This keeps the train template, scenery, and play area chunks always loaded.
        mapWorld.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(mapWorld.getSpawnPos()), 11, Unit.INSTANCE);

        Wathe.LOGGER.info("Loaded map world '{}'.", folderName);
        return mapWorld;
    }

    /**
     * Saves and unloads a map world, freeing its memory. Does nothing for non-map worlds.
     */
    public static void unload(MinecraftServer server, ServerWorld world) {
        RegistryKey<World> key = world.getRegistryKey();
        if (!isMapWorldKey(key)) return;

        Map<RegistryKey<World>, ServerWorld> worlds = ((MinecraftServerAccessor) server).getWorlds();
        worlds.remove(key);
        try {
            // flush=false queues chunk writes asynchronously instead of blocking
            // the server thread. flush=true causes CompletableFuture.join() on the
            // main thread which triggers watchdog crashes.
            world.save(null, false, false);
            world.close();
            Wathe.LOGGER.info("Unloaded map world '{}'.", folderNameFrom(key));
        } catch (IOException e) {
            Wathe.LOGGER.error("Error closing map world '{}': {}", folderNameFrom(key), e.getMessage());
        }
    }

    /**
     * Records the current map name in memory and persists it to {@code config/wathe/server-state.json}
     * so it can be restored on next server start. Pass {@code null} to clear (back to hub).
     */
    public static void setCurrentMap(MinecraftServer server, String folderName) {
        currentMapName = folderName;
        saveLastMapName(folderName);
    }

    public static String getCurrentMapName() {
        return currentMapName;
    }

    public static Optional<ServerWorld> getLoaded(MinecraftServer server, String folderName) {
        return Optional.ofNullable(((MinecraftServerAccessor) server).getWorlds().get(keyFor(folderName)));
    }

    private static void teleportAll(MinecraftServer server, ServerWorld world) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            teleportOne(player, world);
        }
    }

    private static void teleportOne(ServerPlayerEntity player, ServerWorld world) {
        MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(world).getSpawnPos();
        player.teleportTo(new TeleportTarget(world, spawn.pos, Vec3d.ZERO, spawn.yaw, spawn.pitch, TeleportTarget.NO_OP));
    }

    private static void saveLastMapName(String name) {
        try {
            Files.createDirectories(STATE_FILE.getParent());
            JsonObject json = new JsonObject();
            if (name != null) json.addProperty("lastMap", name);
            Files.writeString(STATE_FILE, GSON.toJson(json));
        } catch (IOException e) {
            Wathe.LOGGER.error("Failed to save server state: {}", e.getMessage());
        }
    }

    private static String readLastMapName() {
        if (!Files.exists(STATE_FILE)) return null;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(STATE_FILE)).getAsJsonObject();
            if (!json.has("lastMap") || json.get("lastMap").isJsonNull()) return null;
            String name = json.get("lastMap").getAsString().strip();
            return name.isEmpty() ? null : name;
        } catch (Exception e) {
            Wathe.LOGGER.error("Failed to read server state: {}", e.getMessage());
            return null;
        }
    }

    public static RegistryKey<World> keyFor(String folderName) {
        return RegistryKey.of(RegistryKeys.WORLD, Wathe.id(MAP_KEY_PREFIX + folderName));
    }

    public static boolean isMapWorldKey(RegistryKey<World> key) {
        return key.getValue().getNamespace().equals(MAP_KEY_NAMESPACE) && key.getValue().getPath().startsWith(MAP_KEY_PREFIX);
    }

    public static String folderNameFrom(RegistryKey<World> key) {
        return key.getValue().getPath().substring(MAP_KEY_PREFIX.length());
    }

    /**
     * Returns true if the given world is a playable game world (the hub overworld or a dynamically loaded map).
     */
    public static boolean isGameWorld(ServerWorld world) {
        return world.getServer().getOverworld().equals(world) || isMapWorldKey(world.getRegistryKey());
    }

    /**
     * Returns the folder name of the hub world (the server.properties level-name).
     */
    public static String getHubFolderName(MinecraftServer server) {
        return ((MinecraftServerAccessor) server).getSession().getDirectoryName();
    }
}
