package dev.doctor4t.wathe.mixin.scenery;

import dev.doctor4t.wathe.world.scenery.SceneryChunkFilter;
import dev.doctor4t.wathe.world.scenery.SceneryStreaming;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public abstract class ServerChunkLoadingManagerMixin {
    @Invoker("sendWatchPackets")
    protected abstract void wathe$sendWatchPackets(ServerPlayerEntity player, ChunkFilter filter);

    @Inject(method = "sendWatchPackets(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void wathe$streamSceneryWindow(ServerPlayerEntity player, CallbackInfo ci) {
        SceneryChunkFilter next = SceneryStreaming.update(player);
        if (next == null) return; // Unmodified clients and disabled streaming use vanilla.
        ChunkFilter previous = player.getChunkFilter();
        if (next != previous) {
            boolean sameCenter = previous instanceof SceneryChunkFilter scenery && scenery.center().equals(next.center())
                    || previous instanceof ChunkFilter.Cylindrical normal && normal.center().equals(next.center());
            if (!sameCenter) player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(next.center().x, next.center().z));
            // Vanilla sends ordinary chunk/unload packets and updates all tracking.
            wathe$sendWatchPackets(player, next);
        }
        ci.cancel();
    }
}
