package dev.doctor4t.wathe.game.mapeffect;

import dev.doctor4t.wathe.api.WatheMapEffects;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;

public abstract class HarpyExpressTrainMapEffect extends KeyProvidingMapEffect {
    public HarpyExpressTrainMapEffect(Identifier identifier) {
        super(identifier);
    }

    @Override
    public void initializeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        TrainWorldComponent trainWorldComponent = TrainWorldComponent.KEY.get(serverWorld);
        trainWorldComponent.setSnow(true);
        trainWorldComponent.setFog(true);
        trainWorldComponent.setHud(true);
        trainWorldComponent.setSpeed(130);
        trainWorldComponent.setTime(0);

        // select rooms
        provideKeys(serverWorld, players, 7);
    }

    @Override
    public void finalizeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        // switch back to the lobby map effects
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);
        gameWorldComponent.setMapEffect(WatheMapEffects.HARPY_EXPRESS_LOBBY);
        gameWorldComponent.getMapEffect().initializeMapEffects(serverWorld, players);

    }
}
