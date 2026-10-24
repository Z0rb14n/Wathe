package dev.doctor4t.wathe.world.scenery;

import net.minecraft.server.network.ChunkFilter;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Consumer;

public record SceneryChunkFilter(ChunkPos center, int distance, float speed, long nextUpdate,
                                 SceneryWindow.Snapshot snapshot) implements ChunkFilter {
    @Override
    public boolean isWithinDistance(int x, int z, boolean includeEdge) {
        if (!snapshot.chunks().contains(SceneryWindow.key(x, z))) return false;
        if (includeEdge) return true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!snapshot.chunks().contains(SceneryWindow.key(x + dx, z + dz))) return false;
            }
        }
        return true;
    }

    @Override
    public void forEach(Consumer<ChunkPos> consumer) {
        for (long key : snapshot.chunks()) consumer.accept(new ChunkPos(SceneryWindow.x(key), SceneryWindow.z(key)));
    }
}
