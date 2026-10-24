package dev.doctor4t.wathe.client.util;

import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SecretMurderCohorts {
    private SecretMurderCohorts() {}

    public static List<UUID> choose(List<UUID> participants, UUID self, int killerDividend, Random random) {
        int count = Math.max(0, participants.size() / Math.max(1, killerDividend) - 1);
        List<UUID> candidates = new ArrayList<>(participants);
        candidates.remove(self);
        List<UUID> cohorts = new ArrayList<>();
        while (cohorts.size() < count && !candidates.isEmpty()) {
            cohorts.add(candidates.remove(random.nextInt(candidates.size())));
        }
        return cohorts;
    }
}
