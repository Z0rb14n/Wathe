package dev.doctor4t.wathe.compat;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.SecretMurderTeamSize;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Read optional addon settings on the server; clients never need local copies of those settings. */
public final class SecretMurderRoleCompat {
    private SecretMurderRoleCompat() {}

    public static boolean isSecretMurder(GameMode mode) {
        return mode == WatheGameModes.SECRET_MURDER || mode != null && mode.getClass().getName()
                .equals("org.agmas.harpymodloader.modded_murder.SecretModdedMurderGameMode");
    }

    public static int chooseCohortCount(GameWorldComponent game, int participants, long seed) {
        int fallback = Math.max(0, participants / Math.max(1, game.getKillerDividend()) - 1);
        var loader = FabricLoader.getInstance();
        if (!loader.getModContainer("harpymodloader")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("1.2.5-h1.4")).orElse(false)) {
            return fallback;
        }
        try {
            Class<?> hml = Class.forName("org.agmas.harpymodloader.Harpymodloader");
            Class<?> configType = Class.forName("org.agmas.harpymodloader.config.HarpyModLoaderConfig");
            Class<?> handlerType = Class.forName("dev.isxander.yacl3.config.v2.api.ConfigClassHandler");
            Object handler = configType.getField("HANDLER").get(null);
            handlerType.getMethod("load").invoke(handler);
            Object config = handlerType.getMethod("instance").invoke(handler);
            Collection<?> disabled = (Collection<?>) configType.getField("disabled").get(config);
            Collection<?> vanilla = (Collection<?>) hml.getField("VANNILA_ROLES").get(null);
            Collection<?> nonMurder = (Collection<?>) hml.getField("NON_MURDER_ROLES").get(null);
            Map<?, ?> maxima = (Map<?, ?>) hml.getField("ROLE_MAX").get(null);

            Set<Role> visible = new HashSet<>();
            if (LegacyRoleAddons.needsNoellesPatch()) {
                Class<?> noelle = Class.forName("org.agmas.noellesroles.Noellesroles");
                visible.add((Role) noelle.getField("MIMIC").get(null));
                for (Object role : (Collection<?>) noelle.getField("KILLER_SIDED_NEUTRALS").get(null)) {
                    visible.add((Role) role);
                }
            }

            List<SecretMurderTeamSize.RoleSlot> pool = new ArrayList<>();
            for (Role role : WatheRoles.ROLES) {
                if (role.canUseKiller() || vanilla.contains(role) || nonMurder.contains(role)
                        || disabled.contains(role.identifier().toString())) continue;
                Object maximum = maxima.get(role.identifier());
                pool.add(new SecretMurderTeamSize.RoleSlot(role.identifier().toString(), role.isInnocent(),
                        visible.contains(role), maximum instanceof Number number ? Math.max(0, number.intValue()) : -1));
            }
            return SecretMurderTeamSize.choose(participants, game.getKillerDividend(), game.getVigilanteDividend(),
                    pool, new Random(seed), (first, second) -> LegacyRoleAddons.needsKinsPatch()
                            && KinsWatheCompat.conflictingRoles(Identifier.of(first), Identifier.of(second)));
        } catch (ReflectiveOperationException | LinkageError exception) {
            LoggerFactory.getLogger("wathe").warn("Could not preview Secret Murder addon teammates; using the base count", exception);
            return fallback;
        }
    }
}
