package dev.doctor4t.wathe.mixin.client.restrictions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void wathe$resetDebugStateOnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getDebugHud() instanceof DebugHudAccessor accessor) {
            accessor.setShowDebugHud(false);
            accessor.setRenderingChartVisible(false);
            accessor.setRenderingAndTickChartsVisible(false);
            accessor.setPacketSizeAndPingChartsVisible(false);
        }
        client.getEntityRenderDispatcher().setRenderHitboxes(false);
    }
}