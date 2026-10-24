package dev.doctor4t.wathe.mixin.compat.sodium;

import dev.doctor4t.wathe.client.WatheClient;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {

    @Inject(method = "isOutOfGraph", at = @At("HEAD"), remap = false, cancellable = true)
    private void wathe$visitDisconnectedTemplates(CallbackInfoReturnable<Boolean> cir) {
        // Selective retention leaves gaps between source strips. Graph traversal
        // cannot cross those gaps; the tree visits every resident template island.
        if (WatheClient.isTrainMoving()) cir.setReturnValue(true);
    }

    @Inject(method = "shouldUseOcclusionCulling",
            at = @At("HEAD"),
            remap = false,
            cancellable = true)
    private void wathe$forceNotUseOcclusionCulling(Camera camera, boolean spectator, CallbackInfoReturnable<Boolean> cir) {
        if (WatheClient.isTrainMoving()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getSearchDistance", at = @At("HEAD"), remap = false, cancellable = true)
    private void wathe$includeScenerySources(CallbackInfoReturnable<Float> cir) {
        if (WatheClient.isTrainMoving()) {
            // Source sections can be far away before the shader moves them beside
            // the train. Neither fog nor the original render radius can reject them.
            cir.setReturnValue(Float.POSITIVE_INFINITY);
        }
    }
}
