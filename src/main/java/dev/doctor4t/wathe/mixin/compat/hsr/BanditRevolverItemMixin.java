package dev.doctor4t.wathe.mixin.compat.hsr;

import dev.doctor4t.wathe.client.util.GunControls;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use HSR's existing shot, recoil, particles and cooldown through Wathe's input route. */
@Pseudo
@Mixin(targets = "dpm.harpysimpleroles.item.BanditRevolverItem", remap = false)
public class BanditRevolverItemMixin implements GunControls.AddonGun {
    @Unique
    private boolean wathe$dispatchingShot;

    @ModifyConstant(method = "getGunTarget", constant = @Constant(doubleValue = 15.0), require = 1, allow = 1, remap = false)
    private static double wathe$configuredGunRange(double original) {
        return dev.doctor4t.wathe.WatheConfig.gunRange;
    }

    @Override
    public void triggerAttackUseClient(PlayerEntity player) {
        var item = (Item) (Object) this;
        if (!player.getMainHandStack().isOf(item) || player.getItemCooldownManager().isCoolingDown(item)) return;
        wathe$dispatchingShot = true;
        try {
            item.use(player.getWorld(), player, Hand.MAIN_HAND);
        } finally {
            wathe$dispatchingShot = false;
        }
    }

    @Override
    public void triggerAttackUseServer(ServerPlayerEntity player) {
        // The original item already sends BanditRevolverShootPayload.
    }

    @Inject(method = {"use", "method_7836"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void wathe$routeGunInput(World world, PlayerEntity player, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (world.isClient && !wathe$dispatchingShot) {
            cir.setReturnValue(TypedActionResult.pass(player.getStackInHand(hand)));
        }
    }
}
