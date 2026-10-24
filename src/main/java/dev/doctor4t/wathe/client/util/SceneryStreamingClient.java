package dev.doctor4t.wathe.client.util;

import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.network.SceneryViewDistancePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public final class SceneryStreamingClient {
    private static ClientWorld lastWorld;
    private static int lastDistance = -1;
    private static int heartbeat;

    private SceneryStreamingClient() {
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.getNetworkHandler() == null) {
            lastWorld = null;
            lastDistance = -1;
            heartbeat = 0;
            return;
        }
        if (!ClientPlayNetworking.canSend(SceneryViewDistancePayload.ID)) return;
        int distance = WatheConfig.streamTrainScenery && WatheClient.needsSceneryViewDistance()
                ? client.options.getViewDistance().getValue() : 0;
        if (lastWorld != client.world || lastDistance != distance || ++heartbeat >= 40) {
            ClientPlayNetworking.send(new SceneryViewDistancePayload(distance));
            lastWorld = client.world;
            lastDistance = distance;
            heartbeat = 0;
        }
    }
}
