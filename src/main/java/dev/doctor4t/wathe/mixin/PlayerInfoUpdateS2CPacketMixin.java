package dev.doctor4t.wathe.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.doctor4t.wathe.util.SkinEnforcer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Intercepts {@code profile()} on {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry}
 * at packet serialization. Replaces the textures property for flagged players so all
 * clients: receive a different skin URL without any client-side changes.
 */
@Environment(EnvType.SERVER)
@Mixin(targets = "net.minecraft.network.packet.s2c.play.PlayerListS2CPacket$Entry")
public class PlayerInfoUpdateS2CPacketMixin {

    @Inject(method = "profile", at = @At("RETURN"), cancellable = true)
    private void wathe$replaceSkinProperty(CallbackInfoReturnable<GameProfile> cir) {
        GameProfile profile = cir.getReturnValue();
        if (profile == null) return;

        Optional<Property> override = SkinEnforcer.getOverride(profile.getId());
        if (override == null) return; // not flagged

        GameProfile modified = new GameProfile(profile.getId(), profile.getName());
        modified.getProperties().putAll(profile.getProperties());
        modified.getProperties().removeAll("textures");
        // Optional.empty(): client falls back to Steve/Alex by UUID parity
        override.ifPresent(prop -> modified.getProperties().put("textures", prop));

        cir.setReturnValue(modified);
    }
}
