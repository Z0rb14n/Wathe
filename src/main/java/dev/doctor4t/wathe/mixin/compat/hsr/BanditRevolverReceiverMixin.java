package dev.doctor4t.wathe.mixin.compat.hsr;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerVariablesComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Packet class references are migrated by TmmMixinPlugin before this mixin applies. */
@Pseudo
@Mixin(targets = "dpm.harpysimpleroles.util.BanditRevolverShootPayload$Receiver", remap = false)
public class BanditRevolverReceiverMixin {
    private static final String RECEIVE = "receive(Ldpm/harpysimpleroles/util/BanditRevolverShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V";

    @ModifyConstant(method = RECEIVE, constant = @Constant(doubleValue = 65.0), require = 1, allow = 1)
    private double wathe$configuredGunRange(double original) {
        return dev.doctor4t.wathe.WatheConfig.gunRange;
    }

    @Redirect(method = RECEIVE, at = @At(value = "INVOKE",
            target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;getBackfireChance()F"))
    private float wathe$backfirePerInnocentKill(GameWorldComponent game,
            @Local(argsOnly = true) ServerPlayNetworking.Context context) {
        PlayerVariablesComponent variables = PlayerVariablesComponent.KEY.get(context.player());
        float chance = variables.getInnocentKills() * game.getBackfireChancePerInnocentKill();
        variables.incrementInnocentKills();
        return chance;
    }

    @Redirect(method = RECEIVE, at = @At(value = "INVOKE",
            target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
            remap = true))
    private boolean wathe$resetInnocentKills(GameWorldComponent game, PlayerEntity candidate,
            @Local(argsOnly = true) ServerPlayNetworking.Context context) {
        boolean innocent = game.isInnocent(candidate);
        if (candidate != context.player() && !innocent && !context.player().isCreative()) {
            PlayerVariablesComponent.KEY.get(context.player()).setInnocentKills(0);
        }
        return innocent;
    }
}
