package dev.doctor4t.wathe.mixin.compat.blood;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.doctor4t.wathe.client.util.WatheBloodCompat;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.SimpleParticleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "wathe_blood.WatheBloodClient", remap = false)
public abstract class WatheBloodClientMixin implements WatheBloodCompat.Effects {
    @Shadow @Final public static SimpleParticleType BLOOD_PARTICLE;

    // Replace the server veto listener with death packets.
    @WrapWithCondition(method = "onInitializeClient", at = @At(value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V", ordinal = 0))
    private boolean wathez$skipServerDeathListener(Event<?> event, Object listener) { return false; }

    @Inject(method = "onInitializeClient", at = @At("RETURN"))
    private void wathez$registerEffects(CallbackInfo ci) { WatheBloodCompat.effects = this; }

    @Override
    public ParticleEffect wathez$bloodParticle() { return BLOOD_PARTICLE; }
}
