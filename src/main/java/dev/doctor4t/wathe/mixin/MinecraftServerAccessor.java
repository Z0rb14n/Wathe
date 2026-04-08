package dev.doctor4t.wathe.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.LevelStorage;

import java.util.Map;
import java.util.concurrent.Executor;

@Environment(EnvType.SERVER)
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("worlds")
    Map<RegistryKey<World>, ServerWorld> getWorlds();

    @Accessor("workerExecutor")
    Executor getWorkerExecutor();

    @Accessor("session")
    LevelStorage.Session getSession();
}
