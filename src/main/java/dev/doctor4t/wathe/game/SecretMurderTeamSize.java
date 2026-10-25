package dev.doctor4t.wathe.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;

/** A count-only preview of HML's civilian/neutral role pool, without assigning roles or firing addon events. */
public final class SecretMurderTeamSize {
    private SecretMurderTeamSize() {}

    /** A negative maximum means HML's default allocation; zero prevents spawning. */
    public record RoleSlot(String id, boolean innocent, boolean visibleToKillers, int maximum) {}

    public static int choose(int participants, int killerDividend, int vigilanteDividend,
                             List<RoleSlot> enabledRoles, Random random, BiPredicate<String, String> conflicts) {
        int killers = participants / Math.max(1, killerDividend);
        int vigilantes = Math.min(participants - killers, participants / Math.max(1, vigilanteDividend));
        int remaining = participants - killers - vigilantes;
        int visible = killers;
        Set<String> assigned = new HashSet<>();

        // HML assigns neutrals before civilian replacements. Both compete for civilian slots.
        for (boolean innocent : new boolean[]{false, true}) {
            List<RoleSlot> pool = new ArrayList<>(enabledRoles.stream()
                    .filter(role -> role.innocent() == innocent).toList());
            Collections.shuffle(pool, random);
            int neutrals = 0;
            for (RoleSlot role : pool) {
                if (!innocent && neutrals >= killers) break;
                if (assigned.stream().anyMatch(other -> conflicts.test(role.id(), other))) continue;
                int desired = innocent
                        ? Math.min(Math.max((int) Math.round((double) remaining / pool.size()), 1), killers)
                        : Math.min((int) Math.ceil((double) remaining / pool.size()), killers);
                int count = Math.min(remaining, role.maximum() < 0 ? desired : role.maximum());
                if (count <= 0) continue;
                remaining -= count;
                neutrals += count;
                assigned.add(role.id());
                if (role.visibleToKillers()) visible += count;
            }
        }
        return Math.max(0, Math.min(participants - 1, visible - 1));
    }
}
