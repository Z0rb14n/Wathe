package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class WriteConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:writeConfig")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(WriteConfigCommand::writeConfig)
                        );
    }

    private static int writeConfig(CommandContext<ServerCommandSource> context) {
        // Write config
        try {
            WatheConfig.write(Wathe.MOD_ID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 1;
    }
}
