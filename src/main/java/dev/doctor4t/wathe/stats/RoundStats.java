package dev.doctor4t.wathe.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory per-round stats. One instance per active game world.
 * Finalized and serialized to JSON when the round ends.
 */
public class RoundStats {
    private final Instant startedAt;
    private final long startTick;
    private final String worldId;
    private final Identifier gameMode;
    private final Identifier mapEffect;

    private Instant endedAt;
    private long endTick;
    private GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.NONE;
    private UUID looseEndWinner;

    // Insertion-ordered so per-round files list players in join order.
    private final Map<UUID, PlayerEntry> entries = new LinkedHashMap<>();

    public RoundStats(String worldId, Identifier gameMode, Identifier mapEffect, long startTick) {
        this.startedAt = Instant.now();
        this.startTick = startTick;
        this.worldId = worldId;
        this.gameMode = gameMode;
        this.mapEffect = mapEffect;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Identifier getGameMode() {
        return gameMode;
    }

    public String getWorldId() {
        return worldId;
    }

    /**
     * Records that a player started this round with the given role.
     * Players with no role (joined as spectator, etc.) are not tracked.
     */
    public void registerPlayer(UUID uuid, String name, Role role) {
        if (role == null) return;
        entries.computeIfAbsent(uuid, u -> new PlayerEntry(u, name, role));
    }

    public PlayerEntry getEntry(UUID uuid) {
        return entries.get(uuid);
    }

    public Map<UUID, PlayerEntry> getEntries() {
        return entries;
    }

    public void recordKill(UUID killerUuid, String killerName, UUID victimUuid, String victimName, Identifier reason, long currentTick) {
        long tickIntoRound = currentTick - startTick;
        PlayerEntry victim = entries.get(victimUuid);
        if (victim != null) {
            victim.deaths++;
            victim.deathReason = reason;
            victim.killedBy = killerUuid;
            victim.killedByName = killerName;
            victim.deathTickIntoRound = tickIntoRound;
            victim.survived = false;
        }
        if (killerUuid != null) {
            PlayerEntry killer = entries.get(killerUuid);
            if (killer != null) {
                killer.kills++;
                killer.killsByReason.merge(reason.toString(), 1, Integer::sum);
                killer.victims.add(new VictimRecord(victimUuid, victimName, reason, tickIntoRound));
            }
        }
    }

    public void recordEarned(UUID uuid, int amount) {
        if (amount <= 0) return;
        PlayerEntry entry = entries.get(uuid);
        if (entry != null) entry.moneyEarned += amount;
    }

    public void recordPurchase(UUID uuid, Identifier item, int price, long currentTick) {
        PlayerEntry entry = entries.get(uuid);
        if (entry == null) return;
        entry.moneySpent += price;
        entry.purchases.add(new Purchase(item, price, currentTick - startTick));
    }

    public void finalizeRound(GameFunctions.WinStatus winStatus, UUID looseEndWinner, long endTick) {
        this.winStatus = winStatus;
        this.looseEndWinner = looseEndWinner;
        this.endTick = endTick;
        this.endedAt = Instant.now();
        for (PlayerEntry entry : entries.values()) {
            entry.won = WatheStats.didPlayerWin(entry.role, entry.uuid, winStatus, looseEndWinner);
        }
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("startedAt", startedAt.toString());
        if (endedAt != null) root.addProperty("endedAt", endedAt.toString());
        root.addProperty("durationTicks", Math.max(0, endTick - startTick));
        root.addProperty("world", worldId);
        root.addProperty("gameMode", gameMode == null ? null : gameMode.toString());
        root.addProperty("mapEffect", mapEffect == null ? null : mapEffect.toString());
        root.addProperty("winStatus", winStatus.name());
        if (looseEndWinner != null) root.addProperty("looseEndWinner", looseEndWinner.toString());

        JsonArray players = new JsonArray();
        for (PlayerEntry e : entries.values()) players.add(e.toJson());
        root.add("players", players);
        return root;
    }

    public static class PlayerEntry {
        public final UUID uuid;
        public final String name;
        public final Role role;

        public boolean won = false;
        public boolean survived = true;

        public int kills = 0;
        public final Map<String, Integer> killsByReason = new LinkedHashMap<>();
        public final java.util.List<VictimRecord> victims = new java.util.ArrayList<>();

        public int deaths = 0;
        public Identifier deathReason = null;
        public UUID killedBy = null;
        public String killedByName = null;
        public Long deathTickIntoRound = null;

        public int moneyEarned = 0;
        public int moneySpent = 0;
        public final java.util.List<Purchase> purchases = new java.util.ArrayList<>();

        PlayerEntry(UUID uuid, String name, Role role) {
            this.uuid = uuid;
            this.name = name;
            this.role = role;
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("uuid", uuid.toString());
            o.addProperty("name", name);
            o.addProperty("role", role.identifier().toString());
            o.addProperty("won", won);
            o.addProperty("survived", survived);
            o.addProperty("kills", kills);

            JsonObject killsByReasonJson = new JsonObject();
            killsByReason.forEach(killsByReasonJson::addProperty);
            o.add("killsByReason", killsByReasonJson);

            JsonArray victimsJson = new JsonArray();
            for (VictimRecord v : victims) victimsJson.add(v.toJson());
            o.add("victims", victimsJson);

            o.addProperty("deaths", deaths);
            if (deathReason != null) o.addProperty("deathReason", deathReason.toString());
            if (killedBy != null) {
                o.addProperty("killedBy", killedBy.toString());
                o.addProperty("killedByName", killedByName);
            }
            if (deathTickIntoRound != null) o.addProperty("deathTickIntoRound", deathTickIntoRound);

            o.addProperty("moneyEarned", moneyEarned);
            o.addProperty("moneySpent", moneySpent);

            JsonArray purchasesJson = new JsonArray();
            for (Purchase p : purchases) purchasesJson.add(p.toJson());
            o.add("purchases", purchasesJson);
            return o;
        }
    }

    public record VictimRecord(UUID uuid, String name, Identifier reason, long tickIntoRound) {
        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("uuid", uuid.toString());
            o.addProperty("name", name);
            o.addProperty("reason", reason.toString());
            o.addProperty("tickIntoRound", tickIntoRound);
            return o;
        }
    }

    public record Purchase(Identifier item, int price, long tickIntoRound) {
        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("item", item.toString());
            o.addProperty("price", price);
            o.addProperty("tickIntoRound", tickIntoRound);
            return o;
        }
    }
}
