package dev.doctor4t.wathe.util;

import net.minecraft.entity.player.PlayerEntity;

public class RangeChecks {
    /**
     * Check if target is within range of shooter, accounting for hitbox geometry.
     * Uses center-to-center distance first, then hitbox-aware calculation if needed.
     */
    public static boolean isWithinRange(PlayerEntity shooter, PlayerEntity target, float range) {
        if (target.distanceTo(shooter) < range) return true;

        // Find the closest point on target hitbox to shooter eyes
        double closestX = Math.max(target.getX() - target.getWidth() / 2.0, Math.min(shooter.getX(), target.getX() + target.getWidth() / 2.0));
        double closestY = Math.max(target.getY(), Math.min(shooter.getEyeY(), target.getY() + target.getHeight()));
        double closestZ = Math.max(target.getZ() - target.getWidth() / 2.0, Math.min(shooter.getZ(), target.getZ() + target.getWidth() / 2.0));

        double dx = closestX - shooter.getX();
        double dy = closestY - shooter.getEyeY();
        double dz = closestZ - shooter.getZ();
        double eyeToBoxDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return eyeToBoxDist < range;
    }
}
