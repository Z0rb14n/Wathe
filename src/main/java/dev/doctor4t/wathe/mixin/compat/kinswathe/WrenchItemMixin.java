package dev.doctor4t.wathe.mixin.compat.kinswathe;

import dev.doctor4t.wathe.block.VentHatchBlock;
import dev.doctor4t.wathe.block_entity.VentHatchBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.BsXinQin.kinswathe.items.WrenchItem", remap = false)
public abstract class WrenchItemMixin extends Item {

    private WrenchItemMixin(Settings settings) {
        super(settings);
    }

    // method_7884 = Item.useOnBlock (intermediary), required because KinsWathe is not in our refmap
    @Inject(method = "method_7884", at = @At("HEAD"), cancellable = true, remap = false)
    private void wathe$repairVentHatch(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (player == null || player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }

        if (!(world.getBlockEntity(pos) instanceof VentHatchBlockEntity entity)) {
            return;
        }

        if (!entity.isJammed() && !entity.isBlasted()) {
            return;
        }

        if (world.isClient) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        if (entity.isJammed()) {
            entity.setJammed(0);
            player.sendMessage(Text.literal(""), true);
        }

        if (entity.isBlasted()) {
            entity.setBlasted(false);
        }

        // Ensure hatch is open after repair
        if (!world.getBlockState(pos).get(VentHatchBlock.OPEN)) {
            world.setBlockState(pos, world.getBlockState(pos).with(VentHatchBlock.OPEN, true));
        }

        entity.sync();

        Integer cooldown = GameConstants.ITEM_COOLDOWNS.get(this);
        if (cooldown != null) {
            player.getItemCooldownManager().set(this, cooldown);
        }

        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.8F, 1.0F);
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
