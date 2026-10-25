package dev.doctor4t.wathe.mixin.compat.carpet;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "carpet.network.CarpetClient", remap = false)
public abstract class CarpetClientMixin {
    @Shadow(remap = false)
    private static boolean isServerCarpet;

    @Inject(method = "disconnect", at = @At("HEAD"), remap = false)
    private static void wathe$useIntegratedServerCleanup(CallbackInfo ci) {
        if (MinecraftClient.getInstance().getServer() != null) {
            isServerCarpet = false;
        }
    }
}
