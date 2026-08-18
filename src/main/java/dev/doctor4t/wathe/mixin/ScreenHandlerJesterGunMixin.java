package dev.doctor4t.wathe.mixin;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import net.minecraft.util.collection.DefaultedList;

@Mixin(ScreenHandler.class)
public class ScreenHandlerJesterGunMixin {
    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void wathe$blockJesterTakingGunsFromContainers(int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || slotId < 0 || slotId >= this.slots.size()) return;
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (role == null || !role.identifier().getNamespace().equals("noellesroles") || !role.identifier().getPath().equals("jester")) return;
        Slot slot = this.slots.get(slotId);
        if (slot.inventory instanceof PlayerInventory) return;
        if (slot.getStack().isIn(WatheItemTags.GUNS) && !Registries.ITEM.getId(slot.getStack().getItem()).getPath().contains("fake")) {
            ci.cancel();
        }
    }
}