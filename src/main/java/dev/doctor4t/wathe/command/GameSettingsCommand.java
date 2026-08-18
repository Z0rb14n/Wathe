package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.AutoStartComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class GameSettingsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:gameSettings")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("help")
                                .executes(context -> sendHelp(context.getSource()))
                        )
                        .then(CommandManager.literal("weights")
                                .then(CommandManager.literal("check")
                                        .executes(context -> {
                                            ScoreboardRoleSelectorComponent.KEY.get(context.getSource().getWorld().getScoreboard()).checkWeights(context.getSource());
                                            return 1;
                                        })
                                )
                                .then(CommandManager.literal("reset")
                                        .executes(context -> {
                                            ScoreboardRoleSelectorComponent scoreboardRoleSelectorComponent = ScoreboardRoleSelectorComponent.KEY.get(context.getSource().getServer().getScoreboard());
                                            scoreboardRoleSelectorComponent.reset();
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("weights")
                                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    GameWorldComponent.KEY.get(context.getSource().getWorld()).setWeightsEnabled(BoolArgumentType.getBool(context, "enabled"));
                                                    ScoreboardRoleSelectorComponent.KEY.get(context.getSource()).reset();
                                                    return 1;
                                                })
                                        )
                                )
                                .then(CommandManager.literal("autoStart")
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 60))
                                                .executes(context -> setAutoStart(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))
                                        )
                                )
                                .then(CommandManager.literal("backfire")
                                        .then(CommandManager.argument("chance", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(context -> setBackfire(context.getSource(), FloatArgumentType.getFloat(context, "chance")))
                                        )
                                )
                                .then(CommandManager.literal("roleDividend")
                                        .then(CommandManager.literal("killer")
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3))
                                                        .executes(context -> setKillerDividend(context.getSource(), IntegerArgumentType.getInteger(context, "dividend")))
                                                )
                                        )
                                        .then(CommandManager.literal("vigilante")
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3))
                                                        .executes(context -> setVigilanteDividend(context.getSource(), IntegerArgumentType.getInteger(context, "dividend")))
                                                )
                                        )
                                )
                                .then(CommandManager.literal("bounds")
                                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> enableBounds(context.getSource(), BoolArgumentType.getBool(context, "enabled"))))
                                )
                        ).then(CommandManager.literal("get")
                                .then(CommandManager.literal("weights")
                                        .executes(context -> sendVar(context.getSource(), "weightsEnabled", GameWorldComponent.KEY.get(context.getSource().getWorld()).areWeightsEnabled()))
                                )
                                .then(CommandManager.literal("autoStart")
                                        .executes(context -> sendVar(context.getSource(), "autoStart", AutoStartComponent.KEY.get(context.getSource().getWorld()).startTime))
                                )
                                .then(CommandManager.literal("backfire")
                                        .executes(context -> sendVar(context.getSource(), "backfireChance", GameWorldComponent.KEY.get(context.getSource().getWorld()).getBackfireChance()))
                                )
                                .then(CommandManager.literal("roleDividend")
                                        .then(CommandManager.literal("killer")
                                                .executes(context -> sendVar(context.getSource(), "killerDividend", GameWorldComponent.KEY.get(context.getSource().getWorld()).getKillerDividend()))
                                        )
                                        .then(CommandManager.literal("vigilante")
                                                .executes(context -> sendVar(context.getSource(), "vigilanteDividend", GameWorldComponent.KEY.get(context.getSource().getWorld()).getVigilanteDividend()))
                                        )
                                )
                                .then(CommandManager.literal("bounds")
                                        .executes(context -> sendVar(context.getSource(), "bounds", GameWorldComponent.KEY.get(context.getSource().getWorld()).isBound()))
                                )
                        )
        );
    }

    private static int sendHelp(ServerCommandSource source) {
        source.sendMessage(Text.translatable("wathe.game_settings.help"));
        return 1;
    }

    private static int sendVar(ServerCommandSource source, String var, Object toString) {
        // screw localization
        source.sendMessage(Text.literal("Game Setting Variable " + var + " is " + toString));
        return 1;
    }

    private static int setAutoStart(ServerCommandSource source, int seconds) {
        return Wathe.executeSupporterCommand(source,
                () -> AutoStartComponent.KEY.get(source.getWorld()).setStartTime(GameConstants.getInTicks(0, seconds))
        );
    }

    private static int setBackfire(ServerCommandSource source, float chance) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setBackfireChance(chance)
        );
    }

    private static int setKillerDividend(ServerCommandSource source, int dividend) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setKillerDividend(dividend)
        );
    }

    private static int setVigilanteDividend(ServerCommandSource source, int dividend) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setVigilanteDividend(dividend)
        );
    }

    private static int enableBounds(ServerCommandSource source, boolean enabled) {
        return Wathe.executeSupporterCommand(source,
                () -> {
                    GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(source.getWorld());
                    gameWorldComponent.setBound(enabled);
                }
        );
    }

}
