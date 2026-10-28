package dev.doctor4t.wathe.client.util;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.network.PlayerDeathEffectsPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class WatheBloodCompat {
    private WatheBloodCompat() {}

    public interface Effects {
        ParticleEffect wathez$bloodParticle();
    }

    public static @Nullable Effects effects;
    private static @Nullable ClientWorld particleWorld;
    private static final List<Particle> particles = new ArrayList<>();

    public static void show(ClientWorld world, PlayerDeathEffectsPayload payload) {
        tick(world);
        if (effects == null) return;
        ParticleEffect type = effects.wathez$bloodParticle();
        var manager = MinecraftClient.getInstance().particleManager;
        emit(payload, (position, velocity) -> {
            Particle particle = manager.addParticle(type, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            if (particle != null) particles.add(particle);
        });
    }

    public static void tickClient(MinecraftClient client) {
        tick(client.world);
    }

    public static void tick(@Nullable ClientWorld world) {
        if (particleWorld != world) {
            clear();
            particleWorld = world;
        }
        particles.removeIf(particle -> !particle.isAlive());
    }

    public static void clear() {
        particles.forEach(Particle::markDead);
        particles.clear();
    }

    static void emit(PlayerDeathEffectsPayload payload, BiConsumer<Vec3d, Vec3d> spawn) {
        int min, max;
        double strength, spread, directiveness;
        boolean grenade = payload.reason().equals(GameConstants.DeathReasons.GRENADE);
        if (payload.reason().equals(GameConstants.DeathReasons.GUN)) {
            min = 18; max = 23; strength = 2.5; spread = .5; directiveness = .9;
        } else if (payload.reason().equals(GameConstants.DeathReasons.KNIFE)) {
            min = 10; max = 13; strength = 1; spread = .3; directiveness = .3;
        } else if (payload.reason().equals(GameConstants.DeathReasons.BAT)) {
            min = 5; max = 7; strength = .5; spread = .2; directiveness = .7;
        } else if (grenade) {
            min = 25; max = 50; strength = 5; spread = .8; directiveness = 0;
        } else return;

        Random random = Random.create(payload.seed());
        Vec3d area = new Vec3d(spread, grenade ? 2 : spread, spread);
        Vec3d direction = payload.direction().normalize();
        if (!grenade) {
            int clouds = random.nextBetween(min / 2, max / 2);
            for (int i = 0; i < clouds; i++) spawn.accept(offset(payload.position(), area, random), Vec3d.ZERO);
        }
        int count = random.nextBetween(min, max);
        for (int i = 0; i < count; i++) {
            Vec3d position = offset(payload.position(), area, random);
            Vec3d velocity;
            if (!grenade && direction.lengthSquared() > 0) {
                Vec3d noise = offset(Vec3d.ZERO, new Vec3d(directiveness, directiveness, directiveness), random);
                velocity = direction.add(noise).multiply(strength * payload.strengthScale() / 10);
            } else {
                velocity = offset(Vec3d.ZERO, new Vec3d(2, 2, 2), random).multiply(strength / 10);
            }
            spawn.accept(position, velocity);
        }
    }

    private static Vec3d offset(Vec3d position, Vec3d area, Random random) {
        return position.add((random.nextDouble() - .5) * area.x,
                (random.nextDouble() - .5) * area.y, (random.nextDouble() - .5) * area.z);
    }
}
