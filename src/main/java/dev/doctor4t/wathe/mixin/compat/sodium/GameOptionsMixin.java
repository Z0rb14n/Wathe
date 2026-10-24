package dev.doctor4t.wathe.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.util.SceneryTransform;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @ModifyReturnValue(method = "getClampedViewDistance", at = @At("RETURN"))
    private int wathe$loadScenerySources(int original) {
        return WatheClient.needsSceneryViewDistance() ? Math.max(original, SceneryTransform.SOURCE_VIEW_DISTANCE) : original;
    }

    @ModifyArg(method = "getSyncedOptions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/packet/c2s/common/SyncedClientOptions;<init>(Ljava/lang/String;ILnet/minecraft/network/message/ChatVisibility;ZILnet/minecraft/util/Arm;ZZ)V"), index = 1)
    private int wathe$requestScenerySources(int original) {
        // Request the source chunks too, without changing the saved user option.
        return WatheClient.needsSceneryViewDistance() ? Math.max(original, SceneryTransform.SOURCE_VIEW_DISTANCE) : original;
    }
}
