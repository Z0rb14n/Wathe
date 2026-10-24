package dev.doctor4t.wathe.world.scenery;

import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.client.util.SceneryTransform;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class SceneryStreaming {
    // Accessed only on the server thread; values do not retain player references.
    private static final Map<ServerPlayerEntity, Integer> requests = new WeakHashMap<>();

    private SceneryStreaming() {
    }

    public static void request(ServerPlayerEntity player, int distance) {
        if (distance <= 0) requests.remove(player);
        else requests.put(player, Math.clamp(distance, 2, SceneryTransform.SOURCE_VIEW_DISTANCE));
    }

    public static SceneryChunkFilter update(ServerPlayerEntity player) {
        Integer distance = requests.get(player);
        var train = TrainWorldComponent.KEY.get(player.getServerWorld());
        if (distance == null || distance >= SceneryTransform.SOURCE_VIEW_DISTANCE || train.getSpeed() <= 0) return null;
        var center = player.getChunkPos();
        long now = player.getServerWorld().getTime();
        var previous = player.getChunkFilter() instanceof SceneryChunkFilter filter ? filter : null;
        if (previous != null && previous.center().equals(center) && previous.distance() == distance
                && previous.speed() == train.getSpeed() && now < previous.nextUpdate()) return previous;

        var allowed = ChunkFilter.cylindrical(center, SceneryTransform.SOURCE_VIEW_DISTANCE);
        var snapshot = SceneryWindow.update(center.x, center.z, player.getX(), player.getZ(),
                train.getTime(), train.getSpeed(), distance, now, previous == null ? null : previous.snapshot(),
                key -> allowed.isWithinDistance(SceneryWindow.x(key), SceneryWindow.z(key)));
        return new SceneryChunkFilter(center, distance, train.getSpeed(), now + SceneryWindow.UPDATE_TICKS, snapshot);
    }
}
