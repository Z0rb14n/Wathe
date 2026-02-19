package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ReloadConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:reloadConfig")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ReloadConfigCommand::reloadConfig)
                        );
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        // Load config
        WatheConfig.init(Wathe.MOD_ID, WatheConfig.class);
        // Init constants
        GameConstants.init();
        return 1;
    }
}
