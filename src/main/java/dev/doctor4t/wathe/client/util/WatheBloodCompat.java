package dev.doctor4t.wathe.client.util;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Populated only by the version-gated Wathe Blood client mixin. */
public final class WatheBloodCompat {
    private WatheBloodCompat() {}

    public interface Effects {
        void wathez$showBlood(ClientWorld world, PlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason);
    }

    public static @Nullable Effects effects;

    public static void show(ClientWorld world, PlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason) {
        if (effects != null) effects.wathez$showBlood(world, victim, killer, reason);
    }
}
