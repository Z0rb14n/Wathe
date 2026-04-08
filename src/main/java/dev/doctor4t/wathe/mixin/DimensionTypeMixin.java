package dev.doctor4t.wathe.mixin;

import dev.doctor4t.wathe.world.WatheMapWorlds;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

/**
 * Forces wathe:map/* worlds to use the session root for chunk storage
 * (same as minecraft:overworld), so region data lives at <folder>/region/
 * instead of <folder>/dimensions/wathe/map/<name>/region/.
 */
@Mixin(DimensionType.class)
public class DimensionTypeMixin {
    @Inject(method = "getSaveDirectory", at = @At("HEAD"), cancellable = true)
    private static void wathe$useRootForMapWorlds(RegistryKey<World> worldRef, Path worldDirectory, CallbackInfoReturnable<Path> cir) {
        if (WatheMapWorlds.isMapWorldKey(worldRef)) {
            cir.setReturnValue(worldDirectory);
        }
    }
}
