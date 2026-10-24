package dev.doctor4t.wathe.network;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.world.scenery.SceneryStreaming;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SceneryViewDistancePayload(int distance) implements CustomPayload {
    public static final Id<SceneryViewDistancePayload> ID = new Id<>(Wathe.id("scenery_view_distance"));
    public static final PacketCodec<PacketByteBuf, SceneryViewDistancePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SceneryViewDistancePayload::distance, SceneryViewDistancePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<SceneryViewDistancePayload> {
        @Override
        public void receive(SceneryViewDistancePayload payload, ServerPlayNetworking.Context context) {
            SceneryStreaming.request(context.player(), payload.distance());
        }
    }
}
