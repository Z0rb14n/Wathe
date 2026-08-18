package dev.doctor4t.wathe.game.mapeffect;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;

public class FNAFTestMapEffect extends KeyProvidingMapEffect {
    public FNAFTestMapEffect(Identifier identifier) {
        super(identifier);
    }

    @Override
    public void initializeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            givePlayerKey("Bathroom", player);
        }
        serverWorld.setTimeOfDay(18000);
        serverWorld.setWeather(999999, 0, false, false);
    }

    @Override
    public void finalizeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        serverWorld.setTimeOfDay(0);
        serverWorld.resetWeather();
    }
}
