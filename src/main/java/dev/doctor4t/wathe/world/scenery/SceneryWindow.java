package dev.doctor4t.wathe.world.scenery;

import dev.doctor4t.wathe.client.util.SceneryTransform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongPredicate;

/** A source-column window; independent of camera yaw so turning never needs a reload. */
public final class SceneryWindow {
    public static final int UPDATE_TICKS = 10;
    public static final int LOOK_AHEAD_TICKS = 100;
    public static final int EVICTION_TICKS = 40;
    private static final int SAFETY_BLOCKS = 64;

    public record Snapshot(Map<Long, Long> deadlines, Set<Long> chunks) {
    }

    private SceneryWindow() {
    }

    public static long key(int x, int z) {
        return (x & 0xffffffffL) | ((long) z << 32);
    }

    public static int x(long key) { return (int) key; }
    public static int z(long key) { return (int) (key >> 32); }

    public static Snapshot update(int centerX, int centerZ, double cameraX, double cameraZ,
                                  float time, float speed, int distance, long now,
                                  Snapshot previous, LongPredicate allowed) {
        Map<Long, Long> deadlines = new HashMap<>();
        int radius = SceneryTransform.SOURCE_VIEW_DISTANCE + 1;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long key = key(x, z);
                if (!allowed.test(key)) continue;
                if (wantsColumn(x * 16, z * 16, cameraX, cameraZ, time, speed, distance)) {
                    deadlines.put(key, now + EVICTION_TICKS);
                } else if (previous != null) {
                    Long expiry = previous.deadlines().get(key);
                    if (expiry != null && expiry > now) deadlines.put(key, expiry);
                }
            }
        }

        // Sodium requires neighboring columns (including their lighting) to mesh a
        // section. Keep this halo even for columns outside the visible window.
        Set<Long> chunks = new HashSet<>();
        for (long key : deadlines.keySet()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long neighbor = key(x(key) + dx, z(key) + dz);
                    if (allowed.test(neighbor)) chunks.add(neighbor);
                }
            }
        }
        return new Snapshot(Map.copyOf(deadlines), Set.copyOf(chunks));
    }

    public static boolean wantsColumn(int sourceX, int sourceZ, double cameraX, double cameraZ,
                                       float time, float speed, int distance) {
        int cameraSectionZ = (int) Math.floor(cameraZ / 16);
        int row = Math.floorDiv(sourceZ, 16) - cameraSectionZ;
        // Retain a small physical-world neighborhood for collision and spectators.
        if (Math.abs(sourceX - cameraX) <= 48 && Math.abs(sourceZ - cameraZ) <= 48) return true;

        double radius = distance * 16.0 + SAFETY_BLOCKS;
        // The central strip also contains the actual stationary train at y >= 64.
        if (row > -8 && row < 8 && intersects(sourceX, sourceZ, cameraX, cameraZ, radius)) return true;

        float fractionX = (float) (cameraX - Math.floor(cameraX));
        // Padding covers motion between samples and between window refreshes, plus
        // the whole section. This intentionally overfetches rather than risking holes.
        radius += UPDATE_TICKS * Math.abs(speed) / 73.8;
        for (int ahead = 0; ahead <= LOOK_AHEAD_TICKS; ahead += UPDATE_TICKS) {
            var position = SceneryTransform.transform(sourceX, 0, sourceZ,
                    fractionX, cameraSectionZ, time + ahead, speed);
            if (intersects(position.x(), position.z(), cameraX, cameraZ, radius)) return true;
        }
        return false;
    }

    private static boolean intersects(double x, double z, double cameraX, double cameraZ, double radius) {
        double dx = Math.max(0, Math.max(x - 1 - cameraX, cameraX - x - 17));
        double dz = Math.max(0, Math.max(z - 1 - cameraZ, cameraZ - z - 17));
        return dx * dx + dz * dz <= radius * radius;
    }
}
