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
    }
}
