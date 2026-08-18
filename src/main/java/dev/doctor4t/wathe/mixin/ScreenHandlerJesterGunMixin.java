package dev.doctor4t.wathe.mixin;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerJesterGunMixin {
    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Shadow
    public abstract ItemStack getCursorStack();

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void wathe$blockJesterTakingGunsFromContainers(int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative()) return;
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (role == null || !role.identifier().getNamespace().equals("noellesroles") || !role.identifier().getPath().equals("jester")) return;

        if (actionType == SlotActionType.PICKUP_ALL) {
            ItemStack cursor = this.getCursorStack();
            if (cursor.isIn(WatheItemTags.GUNS) && !Registries.ITEM.getId(cursor.getItem()).getPath().contains("fake")) {
                ci.cancel();
                return;
            }
        }

        if (slotId < 0 || slotId >= this.slots.size()) return;
        Slot slot = this.slots.get(slotId);
        if (slot.inventory instanceof PlayerInventory) return;
        if (slot.getStack().isIn(WatheItemTags.GUNS) && !Registries.ITEM.getId(slot.getStack().getItem()).getPath().contains("fake")) {
            ci.cancel();
        }
    }
}