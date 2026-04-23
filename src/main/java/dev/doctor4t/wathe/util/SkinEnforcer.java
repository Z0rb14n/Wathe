package dev.doctor4t.wathe.util;

import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players whose inner skin layer is predominantly dark (average channel value
 * below {@link WatheConfig#darkThreshold}) and strips their textures property from outgoing
 * {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket}s so all
 * clients fall back to the Steve/Alex default skin.
 *
 * <p>Interception happens server-side at packet serialisation via
 * {@link dev.doctor4t.wathe.mixin.PlayerInfoUpdateS2CPacketMixin} (targets
 * {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry}),
 * so no client-side changes are needed. Client-side skin overrides (e.g. psycho
 * mode, morphling disguise) are unaffected.
 */
public class SkinEnforcer {

    // flagged UUID -> Optional<replacement textures Property>
    // Optional.empty() = strip (Steve/Alex), Optional.of(prop) = use this skin
    private static final Map<UUID, Optional<Property>> overrides = new ConcurrentHashMap<>();

    // Inner skin layer regions in the 64×64 texture: {x, y, width, height}
    private static final int[][] INNER_REGIONS = {
            {8, 8, 8, 8},  // head face
            {20, 20, 8, 12},  // torso
            {44, 20, 4, 12},  // right arm
            {36, 52, 4, 12},  // left arm
            {4, 20, 4, 12},  // right leg
            {20, 52, 4, 12},  // left leg
    };

    public static void analyzeOnJoin(ServerPlayerEntity player) {
        Collection<Property> props = player.getGameProfile().getProperties().get("textures");
        if (props == null || props.isEmpty()) return;
        Property texProp = props.iterator().next();

        CompletableFuture.runAsync(() -> {
            try {
                String json = new String(Base64.getDecoder().decode(texProp.value()));
                var root = JsonParser.parseString(json).getAsJsonObject();
                var textures = root.getAsJsonObject("textures");
                if (!textures.has("SKIN")) return;
                String url = textures.getAsJsonObject("SKIN").get("url").getAsString();

                BufferedImage skin = ImageIO.read(URI.create(url).toURL());
                if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 64) return;

                if (isInnerLayerDark(skin)) {
                    flag(player);
                }
            } catch (Exception e) {
                Wathe.LOGGER.warn("[SkinEnforcer] Skin analysis failed for {}: {}", player.getNameForScoreboard(), e.getMessage());
            }
        });
    }

    private static boolean isInnerLayerDark(BufferedImage skin) {
        long total = 0;
        int count = 0;
        for (int[] r : INNER_REGIONS) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                for (int y = r[1]; y < r[1] + r[3]; y++) {
                    int argb = skin.getRGB(x, y);
                    if (((argb >> 24) & 0xFF) < 128) continue; // transparent – skip
                    total += ((argb >> 16) & 0xFF)  // R
                            + ((argb >> 8) & 0xFF)  // G
                            + (argb & 0xFF); // B
                    count += 3;
                }
            }
        }
        return count > 0 && (double) total / count < WatheConfig.darkThreshold;
    }

    private static void flag(ServerPlayerEntity player) {
        // Optional.empty() causes mixin to strips textures: client defaults to Steve/Alex
        Optional<Property> replacement = Optional.empty();

        overrides.put(player.getUuid(), replacement);
        Wathe.LOGGER.warn("[SkinEnforcer] Flagged {} for dark inner skin — will show {}", player.getNameForScoreboard(), replacement.isPresent() ? "another player's skin" : "default skin");

        // Re-broadcast player info so clients pick up the replacement immediately.
        player.getServer().execute(() -> {
            var packet = new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.ADD_PLAYER, PlayerListS2CPacket.Action.UPDATE_LISTED, PlayerListS2CPacket.Action.UPDATE_GAME_MODE, PlayerListS2CPacket.Action.UPDATE_LATENCY, PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), List.of(player));
            player.getServer().getPlayerManager().sendToAll(packet);
        });
    }

    /**
     * Called from {@link dev.doctor4t.wathe.mixin.PlayerInfoUpdateS2CPacketMixin}
     * at {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry#profile()} return.
     */
    public static @Nullable Optional<Property> getOverride(UUID uuid) {
        return overrides.get(uuid);
    }

    public static boolean isFlagged(UUID uuid) {
        return overrides.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        overrides.remove(uuid);
    }
}
