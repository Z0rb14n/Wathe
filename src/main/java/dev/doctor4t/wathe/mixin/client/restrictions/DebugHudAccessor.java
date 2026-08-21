package dev.doctor4t.wathe.mixin.client.restrictions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(DebugHud.class)
public interface DebugHudAccessor {
    @Accessor("showDebugHud")
    boolean isShowDebugHud();

    @Accessor("showDebugHud")
    void setShowDebugHud(boolean showDebugHud);

    @Accessor("renderingChartVisible")
    void setRenderingChartVisible(boolean visible);

    @Accessor("renderingAndTickChartsVisible")
    void setRenderingAndTickChartsVisible(boolean visible);

    @Accessor("packetSizeAndPingChartsVisible")
    void setPacketSizeAndPingChartsVisible(boolean visible);
}
