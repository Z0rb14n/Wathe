package dev.doctor4t.wathe.game.mapeffect;

import dev.doctor4t.wathe.WatheConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CustomConfigMapEffect extends KeyProvidingMapEffect {
    private static final int WEATHER_DURATION = 999999;
    public CustomConfigMapEffect(Identifier identifier) {
        super(identifier);
    }

    @Override
    public void initializeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        if (WatheConfig.customMapHasTimeOfDay)
            serverWorld.setTimeOfDay(WatheConfig.customMapTimeOfDay);
        if (WatheConfig.customMapHasWeather) {
            int clearTime = !WatheConfig.customMapRaining && !WatheConfig.customMapThundering ? WEATHER_DURATION : 0;
            int rainDuration = clearTime == 0 ? WEATHER_DURATION : 0;
            serverWorld.setWeather(clearTime, rainDuration, WatheConfig.customMapRaining, WatheConfig.customMapThundering);
        }
        if (WatheConfig.customMapNumRoomKeys > 0)
            provideKeysOnly(serverWorld, players, WatheConfig.customMapNumRoomKeys);
        String[] guaranteedKeys = keyVarToArray(WatheConfig.customMapGuaranteedKeys);
        String[] uniqueKeys = keyVarToArray(WatheConfig.customMapUniqueKeys);
        for (String guaranteedKey : guaranteedKeys) {
            for (ServerPlayerEntity player : players) {
                givePlayerKey(guaranteedKey, player);
            }
        }
        Collections.shuffle(players);
        for (int i = 0; i < uniqueKeys.length; i++) {
            givePlayerKey(uniqueKeys[i], players.get(i % players.size()));
        }
    }

    private String[] keyVarToArray(String keys) {
        return Optional.ofNullable(keys)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(str -> str.split(","))
                .orElse(new String[0]);
    }

    @Override
    public void finalizeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        if (WatheConfig.customMapHasTimeOfDay)
            serverWorld.setTimeOfDay(0);
        if (WatheConfig.customMapHasWeather)
            serverWorld.resetWeather();
    }
}
