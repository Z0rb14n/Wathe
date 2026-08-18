package dev.doctor4t.wathe.mixin.client.ui;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LimitedInventoryScreen.class)
public abstract class LimitedInventoryScreenRowLabelMixin {
    private static final int ROLE_ROW_OFFSET = 80;
    private static final int MODIFIER_ROW_OFFSET = 105;

    @Shadow
    @Final
    public ClientPlayerEntity player;

    @Shadow
    protected TextRenderer textRenderer;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    public abstract List<? extends Element> children();

    @Inject(method = "render", at = @At("TAIL"))
    private void wathe$renderRowLabels(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.player == null) return;
        Integer roleRowY = findRowY(ROLE_ROW_OFFSET);
        if (roleRowY != null) {
            Role role = GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player);
            if (role != null) {
                drawLabel(context, Text.translatable("announcement.role." + role.identifier().toTranslationKey()).withColor(role.color()), roleRowY);
            }
        }
        Integer modifierRowY = findRowY(MODIFIER_ROW_OFFSET);
        if (modifierRowY != null) {
            drawLabel(context, Text.translatable("announcement.role.noellesroles.guesser"), modifierRowY);
        }
    }

    @Unique
    private Integer findRowY(int rowOffset) {
        int centerY = (this.height - 32) / 2;
        for (Element child : this.children()) {
            if (child instanceof ClickableWidget widget && widget.getY() >= centerY + rowOffset - 6 && widget.getY() <= centerY + rowOffset + 6) {
                return widget.getY();
            }
        }
        return null;
    }

    @Unique
    private void drawLabel(DrawContext context, Text label, int rowY) {
        int x = this.width / 2 - this.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(this.textRenderer, label, x, rowY - this.textRenderer.fontHeight - 2, 0xFFFFFF);
    }
}