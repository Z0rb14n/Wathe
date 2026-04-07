package dev.doctor4t.wathe;

import com.google.common.reflect.Reflection;
import dev.doctor4t.wathe.block.DoorPartBlock;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.command.*;
import dev.doctor4t.wathe.command.argument.GameModeArgumentType;
import dev.doctor4t.wathe.command.argument.MapEffectArgumentType;
import dev.doctor4t.wathe.command.argument.TimeOfDayArgumentType;
import dev.doctor4t.wathe.world.WatheMapWorlds;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.*;
import dev.doctor4t.wathe.util.*;
import dev.doctor4t.wathe.world.WatheMapWorlds;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.util.Entitlements;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Wathe implements ModInitializer {
    public static final String MOD_ID = "wathe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static @NotNull Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        // Load config
        WatheConfig.init(Wathe.MOD_ID, WatheConfig.class);
        // Init constants
        GameConstants.init();

        // Registry initializers
        Reflection.initialize(WatheDataComponentTypes.class);
        WatheSounds.initialize();
        WatheEntities.initialize();
        WatheBlocks.initialize();
        WatheItems.initialize();
        WatheBlockEntities.initialize();
        WatheParticles.initialize();

        // Register command argument types
        ArgumentTypeRegistry.registerArgumentType(id("timeofday"), TimeOfDayArgumentType.class, ConstantArgumentSerializer.of(TimeOfDayArgumentType::timeofday));
        ArgumentTypeRegistry.registerArgumentType(id("gamemode"), GameModeArgumentType.class, ConstantArgumentSerializer.of(GameModeArgumentType::gameMode));
        ArgumentTypeRegistry.registerArgumentType(id("mapeffect"), MapEffectArgumentType.class, ConstantArgumentSerializer.of(MapEffectArgumentType::mapEffect));

        // Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            MapVariablesCommand.register(dispatcher);
            ReloadConfigCommand.register(dispatcher);
            GameSettingsCommand.register(dispatcher);
            GiveRoomKeyCommand.register(dispatcher);
            StartCommand.register(dispatcher);
            StopCommand.register(dispatcher);
            SetVisualCommand.register(dispatcher);
            ForceRoleCommand.register(dispatcher);
//            UpdateDoorsCommand.register(dispatcher);
            SetTimerCommand.register(dispatcher);
            SetMoneyCommand.register(dispatcher);
            LockToSupportersCommand.register(dispatcher);
            MapCommand.register(dispatcher);
        }));

        // Auto-load the last used map world on startup
        ServerLifecycleEvents.SERVER_STARTED.register(WatheMapWorlds::autoLoad);

        // Redirect players to the active map world on respawn (vanilla always respawns in minecraft:overworld)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            String mapName = WatheMapWorlds.getCurrentMapName();
            if (mapName == null) return;
            if (WatheMapWorlds.isMapWorldKey(newPlayer.getServerWorld().getRegistryKey())) return;
            WatheMapWorlds.getLoaded(newPlayer.getServer(), mapName).ifPresent(target -> {
                MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(target).getSpawnPos();
                newPlayer.teleportTo(new TeleportTarget(target, spawn.pos, Vec3d.ZERO, spawn.yaw, spawn.pitch, TeleportTarget.NO_OP));
            });
        });

        // server lock to supporters; also redirect players from hub to current map
        ServerPlayerEvents.JOIN.register(player -> {
            String mapName = WatheMapWorlds.getCurrentMapName();
            if (mapName != null) {
                WatheMapWorlds.getLoaded(player.getServer(), mapName).ifPresent(target -> {
                    MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(target).getSpawnPos();
                    player.teleportTo(new TeleportTarget(target, spawn.pos, Vec3d.ZERO, spawn.yaw, spawn.pitch, TeleportTarget.NO_OP));
                });
            }
            Scheduler.schedule(() -> {
                if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == null) return;
                String activeMap = WatheMapWorlds.getCurrentMapName();
                if (activeMap == null) return;
                ServerWorld expected = WatheMapWorlds.getLoaded(player.getServer(), activeMap).orElse(null);
                if (expected == null) return;
                if (player.getServerWorld() != expected) {
                    LOGGER.warn("[join-check] {} not in map world '{}' - actual: {} @ {},{},{}",
                            player.getNameForScoreboard(), activeMap,
                            player.getServerWorld().getRegistryKey().getValue(),
                            String.format("%.1f", player.getX()), String.format("%.1f", player.getY()), String.format("%.1f", player.getZ()));
                }
            }, 3);
            ServerPlayNetworking.send(player, ConfigSyncPayload.fromConfig());
            DataSyncAPI.refreshAllPlayerData(player.getUuid()).thenRunAsync(() -> {
                // check if player is supporter now, if not kick
                if (GameWorldComponent.KEY.get(player.getWorld()).isLockedToSupporters() && !Wathe.isSupporter(player)) {
                    player.networkHandler.disconnect(Text.literal("Server is reserved to doctor4t supporters."));
                }
            }, player.getWorld().getServer());
        });

        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShootMuzzleS2CPayload.ID, ShootMuzzleS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PoisonUtils.PoisonOverlayPayload.ID, PoisonUtils.PoisonOverlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunDropPayload.ID, GunDropPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TaskCompletePayload.ID, TaskCompletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceWelcomePayload.ID, AnnounceWelcomePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceEndingPayload.ID, AnnounceEndingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KnifeStabPayload.ID, KnifeStabPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunShootPayload.ID, GunShootPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StoreBuyPayload.ID, StoreBuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoteEditPayload.ID, NoteEditPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KnifeStabPayload.ID, new KnifeStabPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(GunShootPayload.ID, new GunShootPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(StoreBuyPayload.ID, new StoreBuyPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.ID, new NoteEditPayload.Receiver());

        Scheduler.init();
    }

    public static boolean isSkyVisibleAdjacent(@NotNull Entity player) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos playerPos = BlockPos.ofFloored(player.getEyePos());
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                mutable.set(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                if (player.getWorld().isSkyVisible(mutable)) {
                    return !(player.getWorld().getBlockState(playerPos).getBlock() instanceof DoorPartBlock);
                }
            }
        }
        return false;
    }

    public static boolean isExposedToWind(@NotNull Entity player) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos playerPos = BlockPos.ofFloored(player.getEyePos());
        for (int x = 0; x <= 10; x++) {
            mutable.set(playerPos.getX() - x, player.getEyePos().getY(), playerPos.getZ());
            if (!player.getWorld().isSkyVisible(mutable)) {
                return false;
            }
        }
        return true;
    }

    public static final Identifier COMMAND_ACCESS = id("commandaccess");

    public static int executeSupporterCommand(ServerCommandSource source, Runnable runnable) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !player.getClass().equals(ServerPlayerEntity.class)) return 0;

        if (isSupporter(player) || FabricLoader.getInstance().isDevelopmentEnvironment()) {
            runnable.run();
            return 1;
        } else {
            player.sendMessage(Text.translatable("commands.supporter_only"));
            return 0;
        }
    }

    public static @NotNull Boolean isSupporter(PlayerEntity player) {
        Optional<Entitlements> entitlements = Entitlements.token().get(player.getUuid());
        return entitlements.map(value -> value.keys().stream().anyMatch(identifier -> identifier.equals(COMMAND_ACCESS))).orElse(false);
    }
}