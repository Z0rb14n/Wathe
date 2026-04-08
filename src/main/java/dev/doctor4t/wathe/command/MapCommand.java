package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.world.WatheMapWorlds;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.List;

public class MapCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:map").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.literal("list").executes(context -> list(context.getSource()))).then(CommandManager.literal("swap").then(CommandManager.argument("name", StringArgumentType.string()).suggests((context, builder) -> CommandSource.suggestMatching(WatheMapWorlds.scan(context.getSource().getServer()), builder)).executes(context -> swap(context.getSource(), StringArgumentType.getString(context, "name"))))));
    }

    private static int list(ServerCommandSource source) {
        List<String> names = WatheMapWorlds.scan(source.getServer());
        if (names.isEmpty()) {
            source.sendMessage(Text.literal("No map worlds found. Place world folders in the server root."));
        } else {
            source.sendMessage(Text.literal("Available map worlds: " + String.join(", ", names)));
        }
        return 1;
    }

    private static int swap(ServerCommandSource source, String name) {
        if (GameWorldComponent.KEY.get(source.getWorld()).isRunning()) {
            source.sendError(Text.literal("Cannot swap maps while a game is running."));
            return -1;
        }

        // Store current world to unload it after
        ServerWorld previousWorld = source.getWorld();
        boolean previousWasMapWorld = WatheMapWorlds.isMapWorldKey(previousWorld.getRegistryKey());

        String hubName = WatheMapWorlds.getHubFolderName(source.getServer());
        boolean targetIsHub = name.equals(hubName);

        ServerWorld target;
        if (targetIsHub) {
            // Hub world is always loaded as minecraft:overworld
            target = source.getServer().getOverworld();
        } else {
            try {
                target = WatheMapWorlds.load(source.getServer(), name);
            } catch (Exception e) {
                source.sendError(Text.literal("Failed to load map world '" + name + "': " + e.getMessage()));
                return -1;
            }
        }

        MapVariablesWorldComponent.PosWithOrientation spawnPos = MapVariablesWorldComponent.KEY.get(target).getSpawnPos();
        TeleportTarget teleportTarget = new TeleportTarget(target, spawnPos.pos, Vec3d.ZERO, spawnPos.yaw, spawnPos.pitch, TeleportTarget.NO_OP);

        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            player.teleportTo(teleportTarget);
        }

        // null means "back in the hub" no active dynamic map
        WatheMapWorlds.setCurrentMap(source.getServer(), targetIsHub ? null : name);

        // Unload the previous world if it was a dynamically loaded map world
        if (previousWasMapWorld) {
            WatheMapWorlds.unload(source.getServer(), previousWorld);
        }

        source.sendMessage(Text.literal("Swapped to map '" + name + "'."));
        return 1;
    }
}
