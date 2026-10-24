package dev.doctor4t.wathe.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.client.util.SceneryTransform;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0))
    private Object wathe$keepScenerySourcesLoaded(Object original) {
        // Singleplayer reads the raw slider every tick, bypassing the client's
        // effective distance and advertised chunk request. Keep simulation separate.
        IntegratedServer server = (IntegratedServer) (Object) this;
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (TrainWorldComponent.KEY.get(player.getServerWorld()).getSpeed() > 0) {
                return Math.max((Integer) original, SceneryTransform.SOURCE_VIEW_DISTANCE);
            }
        }
        return original;
    }
}
