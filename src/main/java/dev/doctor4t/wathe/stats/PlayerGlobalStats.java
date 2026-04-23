package dev.doctor4t.wathe.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cumulative per-player stats persisted to {@code config/wathe/stats/players/<uuid>.json}.
 * Loaded, mutated, and re-saved at the end of each round.
 */
public class PlayerGlobalStats {
    public static final int SCHEMA_VERSION = 1;

    public final UUID uuid;
    public String name;
    public String firstSeen;
    public String lastSeen;

    public int rounds = 0;
    public int wins = 0;
    public int losses = 0;
    public int survivedRounds = 0;

    public int kills = 0;
    public int deaths = 0;

    public long moneyEarnedTotal = 0;
    public long moneySpentTotal = 0;

    public final Map<String, Integer> killsByReason = new LinkedHashMap<>();
    public final Map<String, Integer> deathsByReason = new LinkedHashMap<>();
    public final Map<String, Integer> rolesPlayed = new LinkedHashMap<>();
    public final Map<String, Integer> winsByRole = new LinkedHashMap<>();
    public final Map<String, Integer> lossesByRole = new LinkedHashMap<>();
    public final Map<String, Integer> killsByRole = new LinkedHashMap<>();
    public final Map<String, Integer> purchasesByItem = new LinkedHashMap<>();

    public PlayerGlobalStats(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Folds a single completed round into this player's totals.
     */
    public void applyRound(RoundStats.PlayerEntry entry) {
        Identifier roleId = entry.role.identifier();
        String roleKey = roleId.toString();

        rounds++;
        if (entry.won) {
            wins++;
            winsByRole.merge(roleKey, 1, Integer::sum);
        } else {
            losses++;
            lossesByRole.merge(roleKey, 1, Integer::sum);
        }
        if (entry.survived) survivedRounds++;
        rolesPlayed.merge(roleKey, 1, Integer::sum);

        kills += entry.kills;
        if (entry.kills > 0) killsByRole.merge(roleKey, entry.kills, Integer::sum);
        entry.killsByReason.forEach((k, v) -> killsByReason.merge(k, v, Integer::sum));

        if (entry.deaths > 0) {
            deaths += entry.deaths;
            if (entry.deathReason != null) {
                deathsByReason.merge(entry.deathReason.toString(), 1, Integer::sum);
            }
        }

        moneyEarnedTotal += entry.moneyEarned;
        moneySpentTotal += entry.moneySpent;
        for (RoundStats.Purchase p : entry.purchases) {
            purchasesByItem.merge(p.item().toString(), 1, Integer::sum);
        }

        String now = Instant.now().toString();
        if (firstSeen == null) firstSeen = now;
        lastSeen = now;
        name = entry.name;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("schemaVersion", SCHEMA_VERSION);
        o.addProperty("uuid", uuid.toString());
        if (name != null) o.addProperty("name", name);
        if (firstSeen != null) o.addProperty("firstSeen", firstSeen);
        if (lastSeen != null) o.addProperty("lastSeen", lastSeen);

        o.addProperty("rounds", rounds);
        o.addProperty("wins", wins);
        o.addProperty("losses", losses);
        o.addProperty("survivedRounds", survivedRounds);
        o.addProperty("kills", kills);
        o.addProperty("deaths", deaths);
        o.addProperty("moneyEarnedTotal", moneyEarnedTotal);
        o.addProperty("moneySpentTotal", moneySpentTotal);

        o.add("killsByReason", mapToJson(killsByReason));
        o.add("deathsByReason", mapToJson(deathsByReason));
        o.add("rolesPlayed", mapToJson(rolesPlayed));
        o.add("winsByRole", mapToJson(winsByRole));
        o.add("lossesByRole", mapToJson(lossesByRole));
        o.add("killsByRole", mapToJson(killsByRole));
        o.add("purchasesByItem", mapToJson(purchasesByItem));
        return o;
    }

    private static JsonObject mapToJson(Map<String, Integer> map) {
        JsonObject o = new JsonObject();
        map.forEach(o::addProperty);
        return o;
    }

    public static PlayerGlobalStats fromJson(UUID uuid, JsonObject o) {
        PlayerGlobalStats s = new PlayerGlobalStats(uuid);
        if (o.has("name") && !o.get("name").isJsonNull()) s.name = o.get("name").getAsString();
        if (o.has("firstSeen") && !o.get("firstSeen").isJsonNull()) s.firstSeen = o.get("firstSeen").getAsString();
        if (o.has("lastSeen") && !o.get("lastSeen").isJsonNull()) s.lastSeen = o.get("lastSeen").getAsString();
        s.rounds = getInt(o, "rounds");
        s.wins = getInt(o, "wins");
        s.losses = getInt(o, "losses");
        s.survivedRounds = getInt(o, "survivedRounds");
        s.kills = getInt(o, "kills");
        s.deaths = getInt(o, "deaths");
        s.moneyEarnedTotal = getLong(o, "moneyEarnedTotal");
        s.moneySpentTotal = getLong(o, "moneySpentTotal");
        readMap(o, "killsByReason", s.killsByReason);
        readMap(o, "deathsByReason", s.deathsByReason);
        readMap(o, "rolesPlayed", s.rolesPlayed);
        readMap(o, "winsByRole", s.winsByRole);
        readMap(o, "lossesByRole", s.lossesByRole);
        readMap(o, "killsByRole", s.killsByRole);
        readMap(o, "purchasesByItem", s.purchasesByItem);
        return s;
    }

    private static int getInt(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
    }

    private static long getLong(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
    }

    private static void readMap(JsonObject o, String key, Map<String, Integer> into) {
        if (!o.has(key) || !o.get(key).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(key).entrySet()) {
            if (e.getValue().isJsonNull()) continue;
            into.put(e.getKey(), e.getValue().getAsInt());
        }
    }
}
