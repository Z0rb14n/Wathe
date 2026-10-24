package dev.doctor4t.wathe.mixin.scenery;

import dev.doctor4t.wathe.world.scenery.SceneryChunkFilter;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ChunkFilter.class)
public interface ChunkFilterMixin {
    @Inject(method = "forEachChangedChunk", at = @At("HEAD"), cancellable = true)
    private static void wathe$diffSceneryWindow(ChunkFilter previous, ChunkFilter next,
                                               Consumer<ChunkPos> added, Consumer<ChunkPos> removed, CallbackInfo ci) {
        if (previous instanceof SceneryChunkFilter || next instanceof SceneryChunkFilter) {
            // Vanilla's generic-filter fallback unloads and reloads the entire set.
            // Diff membership so overlapping templates and meshes remain resident.
            previous.forEach(pos -> { if (!next.isWithinDistance(pos)) removed.accept(pos); });
            next.forEach(pos -> { if (!previous.isWithinDistance(pos)) added.accept(pos); });
            ci.cancel();
        }
    }
}
