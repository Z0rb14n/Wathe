package dev.doctor4t.wathe.network;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.util.WatheBloodCompat;
import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public record PlayerDeathEffectsPayload(Vec3d position, Vec3d direction, float strengthScale, Identifier reason, long seed) implements CustomPayload {
    public static final Id<PlayerDeathEffectsPayload> ID = new Id<>(Wathe.id("player_death_effects"));
    public static final PacketCodec<PacketByteBuf, PlayerDeathEffectsPayload> CODEC = PacketCodec.of((payload, buf) -> {
        buf.writeVec3d(payload.position());
        buf.writeVec3d(payload.direction());
        buf.writeFloat(payload.strengthScale());
        buf.writeIdentifier(payload.reason());
        buf.writeLong(payload.seed());
    }, buf -> new PlayerDeathEffectsPayload(buf.readVec3d(), buf.readVec3d(), buf.readFloat(), buf.readIdentifier(), buf.readLong()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    // Capture before spectator mode changes the hitbox.
    public static PlayerDeathEffectsPayload capture(ServerPlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason) {
        Vec3d position = victim.getBoundingBox().getCenter();
        if (killer == null || killer == victim || reason.equals(GameConstants.DeathReasons.GRENADE)) {
            return new PlayerDeathEffectsPayload(position, Vec3d.ZERO, 1, reason, victim.getRandom().nextLong());
        }
        return aimedAt(victim.getBoundingBox(), killer.getEyePos(), killer.getRotationVec(1),
                (float) Math.max(.5, 1 / Math.max(.001, victim.distanceTo(killer))), reason, victim.getRandom().nextLong());
    }

    public static PlayerDeathEffectsPayload aimedAt(Box victimBox, Vec3d origin, Vec3d aim, float strengthScale, Identifier reason, long seed) {
        Vec3d center = victimBox.getCenter();
        Vec3d direction = aim.normalize();
        var hit = victimBox.raycast(origin, origin.add(direction.multiply(origin.distanceTo(center) + 2)));
        if (hit.isEmpty()) {
            // Confirmed hits can miss the current aim ray.
            direction = center.subtract(origin).normalize();
            hit = victimBox.raycast(origin, center);
        }
        return new PlayerDeathEffectsPayload(hit.orElse(center), direction, strengthScale, reason, seed);
    }

    public void send(ServerPlayerEntity victim, @Nullable PlayerEntity killer) {
        var viewers = new HashSet<>(PlayerLookup.tracking(victim));
        viewers.addAll(PlayerLookup.around(victim.getServerWorld(), position, 128));
        viewers.add(victim);
        if (killer instanceof ServerPlayerEntity player && player.getWorld() == victim.getWorld()) viewers.add(player);
        for (ServerPlayerEntity viewer : viewers) {
            if (ServerPlayNetworking.canSend(viewer, ID)) ServerPlayNetworking.send(viewer, this);
        }
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<PlayerDeathEffectsPayload> {
        @Override
        public void receive(PlayerDeathEffectsPayload payload, ClientPlayNetworking.Context context) {
            var world = context.client().world;
            if (world != null) WatheBloodCompat.show(world, payload);
        }
    }
}
