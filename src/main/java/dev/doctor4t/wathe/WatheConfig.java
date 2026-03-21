package dev.doctor4t.wathe;

import dev.doctor4t.ratatouille.client.util.OptionLocker;
import dev.doctor4t.wathe.client.WatheClient;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.MinecraftClient;

public class WatheConfig extends MidnightConfig {
    @Entry
    public static boolean ultraPerfMode = false;
    @Entry
    public static boolean disableScreenShake = false;

    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int startingMoney = 100;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int moneyPerKill = 100;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int ticksBetweenMoneyIncrement = 200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int moneyIncrementAmount = 5;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int knifeCooldownTicks = 1200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int knifePrice = 100;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int revolverCooldownTicks = 200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int revolverPrice = 300;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int derringerCooldownTicks = 20;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int grenadeCooldownTicks = 6000;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int grenadePrice = 350;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int lockpickCooldownTicks = 3600;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int lockpickPrice = 50;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int crowbarCooldownTicks = 200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int crowbarPrice = 25;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int bodyBagCooldownTicks = 6000;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int bodyBagPrice = 200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int psychoCooldownTicks = 6000;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int psychoPrice = 300;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int poisonPrice = 100;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int scorpionPrice = 50;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int firecrackerPrice = 10;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int blackoutCooldownTicks = 3600;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int blackoutPrice = 200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int notePrice = 10;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int ticksOnCivilianKill = 1200;
    @Server
    @Entry(category = "GameConstants", min = 0)
    public static int gunRange = 65;
    @Server
    @Entry(category = "GameConstants")
    public static String hornBlockMapEffectId = "hotel";
    @Server
    @Entry(category = "CustomMap")
    public static boolean customMapHasTimeOfDay = false;
    @Server
    @Entry(category = "CustomMap")
    public static int customMapTimeOfDay = 18000;
    @Server
    @Entry(category = "CustomMap")
    public static boolean customMapHasWeather = false;
    @Server
    @Entry(category = "CustomMap")
    public static boolean customMapRaining = false;
    @Server
    @Entry(category = "CustomMap")
    public static boolean customMapThundering = false;
    @Server
    @Entry(category = "CustomMap")
    public static String customMapUniqueKeys = "";
    @Server
    @Entry(category = "CustomMap")
    public static String customMapGuaranteedKeys = "";
    @Server
    @Entry(category = "CustomMap", min = 0)
    public static int customMapNumRoomKeys = 7;

    @Override
    public void writeChanges(String modid) {
        super.writeChanges(modid);

        int lockedRenderDistance = WatheClient.getLockedRenderDistance(ultraPerfMode);
        OptionLocker.overrideOption("renderDistance", lockedRenderDistance);

        MinecraftClient.getInstance().options.viewDistance.setValue(lockedRenderDistance);
    }
}
