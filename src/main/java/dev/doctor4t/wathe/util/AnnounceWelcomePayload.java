package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

public record AnnounceWelcomePayload(int role, int killers, int targets) implements CustomPayload {
    public static final Id<AnnounceWelcomePayload> ID = new Id<>(Wathe.id("announcewelcome"));
    public static final PacketCodec<PacketByteBuf, AnnounceWelcomePayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, AnnounceWelcomePayload::role, PacketCodecs.INTEGER, AnnounceWelcomePayload::killers, PacketCodecs.INTEGER, AnnounceWelcomePayload::targets, AnnounceWelcomePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AnnounceWelcomePayload> {
        @Override
        public void receive(@NotNull AnnounceWelcomePayload payload, ClientPlayNetworking.@NotNull Context context) {
            RoleAnnouncementTexts.RoleAnnouncementText text = null;
            ClientPlayerEntity player = context.player();
            if (player != null) {
                text = RoleAnnouncementTexts.findByRole(GameWorldComponent.KEY.get(player.getWorld()).getRole(player));
            }
            if (text == null) {
                if (payload.role() < 0 || payload.role() >= RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.size()) return;
                text = RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.get(payload.role());
            }
            RoundTextRenderer.startWelcome(text, payload.killers(), payload.targets());
        }
    }
}