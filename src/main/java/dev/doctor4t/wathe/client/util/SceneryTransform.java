package dev.doctor4t.wathe.client.util;

public final class SceneryTransform {
    public static final int SOURCE_VIEW_DISTANCE = 32;

    private SceneryTransform() {
    }

    public record Position(float x, float y, float z) {
    }

    public static Position transform(int x, int y, int z, float cameraFractionX,
                                     int cameraSectionZ, float time, float speed) {
        int zSection = Math.floorDiv(z, 16) - cameraSectionZ;
        int tileOffset = zSection <= -8 ? -512 : zSection >= 8 ? 512 : 0;
        if (tileOffset == 0 && Math.floorDiv(y, 16) >= 4) {
            return new Position(x, y, z);
        }

        float movingX = ((x - cameraFractionX + tileOffset + time / 73.8f * speed) % 1536 - 768)
                + cameraFractionX;
        return new Position(movingX, y + 116, z + (zSection <= -8 ? 240 : zSection >= 8 ? -240 : 0));
    }

    public static boolean isWithinViewDistance(Position origin, double cameraX, double cameraY,
                                               double cameraZ, int viewDistance) {
        // Test the complete section, including models extending beyond its bounds.
        double dx = nearestDistance(origin.x() - 1, origin.x() + 17, cameraX);
        double dy = nearestDistance(origin.y() - 1, origin.y() + 17, cameraY);
        double dz = nearestDistance(origin.z() - 1, origin.z() + 17, cameraZ);
        double radius = viewDistance * 16.0;
        return dx * dx + dz * dz <= radius * radius && dy <= radius;
    }

    private static double nearestDistance(double min, double max, double camera) {
        return Math.max(0, Math.max(min - camera, camera - max));
    }
}
