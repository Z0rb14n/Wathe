package dev.doctor4t.wathe.network;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.util.WatheBloodCompat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** A visual notification sent only after an actual, non-cancelled death. */
public record PlayerDeathEffectsPayload(int victim, int killer, Identifier reason) implements CustomPayload {
    public static final Id<PlayerDeathEffectsPayload> ID = new Id<>(Wathe.id("player_death_effects"));
    public static final PacketCodec<PacketByteBuf, PlayerDeathEffectsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PlayerDeathEffectsPayload::victim,
            PacketCodecs.VAR_INT, PlayerDeathEffectsPayload::killer,
            Identifier.PACKET_CODEC, PlayerDeathEffectsPayload::reason, PlayerDeathEffectsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static void send(ServerPlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason) {
        var payload = new PlayerDeathEffectsPayload(victim.getId(), killer == null ? -1 : killer.getId(), reason);
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(victim)) {
            if (ServerPlayNetworking.canSend(viewer, ID)) ServerPlayNetworking.send(viewer, payload);
        }
        if (ServerPlayNetworking.canSend(victim, ID)) ServerPlayNetworking.send(victim, payload);
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<PlayerDeathEffectsPayload> {
        @Override
        public void receive(PlayerDeathEffectsPayload payload, ClientPlayNetworking.Context context) {
            var world = context.client().world;
            if (world != null && world.getEntityById(payload.victim()) instanceof PlayerEntity victim) {
                var killer = world.getEntityById(payload.killer());
                WatheBloodCompat.show(world, victim, killer instanceof PlayerEntity player ? player : null, payload.reason());
            }
        }
    }
}
