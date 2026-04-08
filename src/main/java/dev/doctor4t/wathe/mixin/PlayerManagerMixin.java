package dev.doctor4t.wathe.mixin;

import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.world.WatheMapWorlds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.SERVER)
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    /**
     * Redirects the server.getWorld() call that resolves the joining player's saved
     * dimension. Return the active map world so that vanilla's setServerWorld,
     * GameJoinS2CPacket, sendWorldInfo, and onPlayerConnected all use the map world.
     */
    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"))
    private ServerWorld wathe$redirectToMapWorld(MinecraftServer server, RegistryKey<World> key, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        String mapName = WatheMapWorlds.getCurrentMapName();
        if (mapName != null) {
            ServerWorld target = WatheMapWorlds.getLoaded(server, mapName).orElse(null);
            if (target != null) {
                MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(target).getSpawnPos();
                player.refreshPositionAndAngles(spawn.pos, spawn.yaw, spawn.pitch);
                return target;
            }
        }
        return server.getWorld(key);
    }
}
