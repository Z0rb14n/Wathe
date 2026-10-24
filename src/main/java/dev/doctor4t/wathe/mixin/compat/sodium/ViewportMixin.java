package dev.doctor4t.wathe.mixin.compat.sodium;

import dev.doctor4t.wathe.client.WatheClient;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Viewport.class)
public class ViewportMixin {
    // The out-of-graph tree traversal bypasses OcclusionCuller.isWithinFrustum.
    @Inject(method = "isBoxVisibleDirect", at = @At("HEAD"), remap = false, cancellable = true)
    private void wathe$includeScenerySection(float x, float y, float z, float size,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (WatheClient.isTrainMoving()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getBoxIntersectionDirect", at = @At("HEAD"), remap = false, cancellable = true)
    private void wathe$includeSceneryTree(float x, float y, float z, float size,
                                         CallbackInfoReturnable<Integer> cir) {
        if (WatheClient.isTrainMoving()) {
            cir.setReturnValue(FrustumIntersection.INSIDE);
        }
    }
}
