package dev.doctor4t.wathe.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TmmMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String s) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("dev.doctor4t.wathe.mixin.compat.kinswathe.") && !FabricLoader.getInstance().isModLoaded("kinswathe")) {
            return false;
        }
        if (mixinClassName.contains("dev.doctor4t.wathe.mixin.compat.sodium.") && !FabricLoader.getInstance().isModLoaded("sodium")) {
            return false;
        }
        if (mixinClassName.equals("dev.doctor4t.wathe.mixin.client.scenery.SceneryWorldRendererMixin") && FabricLoader.getInstance().isModLoaded("sodium")) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
