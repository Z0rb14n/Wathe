package dev.doctor4t.wathe.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

/** Compatibility is scoped to the releases whose old Wathe APIs were inspected. */
public final class LegacyRoleAddons {
    private LegacyRoleAddons() {}

    public static boolean needsHsrPatch() {
        return isVersion("harpysimpleroles", "1.0.0-h1.3.2");
    }

    public static boolean needsKinsPatch() {
        return isVersion("kinswathe", "1.6.4");
    }

    public static boolean needsNoellesPatch() {
        return isVersion("noellesroles", "1.7.5-h1.4");
    }

    public static boolean needsShooterPunishmentsPatch() {
        return isVersion("shooterpunishments", "1.2");
    }

    public static boolean needsBloodPatch() {
        return isVersion("wathe_blood", "1.1.6");
    }

    private static boolean isVersion(String id, String version) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals(version))
                .orElse(false);
    }

    public static boolean keepRevolver(ServerPlayerEntity player) {
        // Keep optional addon types out of the ordinary Wathe initialization path.
        return needsKinsPatch() && KinsWatheCompat.keepRevolver(player);
    }
}
