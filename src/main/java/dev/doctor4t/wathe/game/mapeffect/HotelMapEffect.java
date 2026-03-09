package dev.doctor4t.wathe.game.mapeffect;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;

public class HotelMapEffect extends KeyProvidingMapEffect {
    private final String[] extraKeys;
    public HotelMapEffect(Identifier identifier) {
        super(identifier);
        extraKeys = new String[]{"Storage Room", "Bar", "Kitchen"};
    }

    @Override
    public void initializeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
        super.initializeMapEffects(serverWorld, players);
        Collections.shuffle(players);
        for (int i = 0; i < extraKeys.length; i++) {
            String key = extraKeys[i];
            ServerPlayerEntity player = players.get(i);
            givePlayerKey(key, player);
        }
    }
}
