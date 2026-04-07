package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ConfigSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ReloadConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:reloadConfig").requires(source -> source.hasPermissionLevel(2)).executes(ReloadConfigCommand::reloadConfig));
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        // Load config
        WatheConfig.init(Wathe.MOD_ID, WatheConfig.class);
        // Init constants
        GameConstants.init();
        // Sync updated config to all connected players
        ConfigSyncPayload payload = ConfigSyncPayload.fromConfig();
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
        return 1;
    }
}
