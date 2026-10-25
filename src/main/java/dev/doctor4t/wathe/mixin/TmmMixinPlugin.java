package dev.doctor4t.wathe.mixin;

import net.fabricmc.loader.api.FabricLoader;
import dev.doctor4t.wathe.compat.LegacyRoleAddons;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
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
        if (mixinClassName.startsWith("dev.doctor4t.wathe.mixin.compat.carpet.")) {
            return FabricLoader.getInstance().getModContainer("carpet")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("1.4.147+v240613"))
                    .orElse(false);
        }
        if (mixinClassName.startsWith("dev.doctor4t.wathe.mixin.compat.shooterpunishments.")) {
            return LegacyRoleAddons.needsShooterPunishmentsPatch()
                    && (!mixinClassName.endsWith(".BanditReceiverMixin") || LegacyRoleAddons.needsHsrPatch());
        }
        if (mixinClassName.startsWith("dev.doctor4t.wathe.mixin.compat.noellesroles.")) return LegacyRoleAddons.needsNoellesPatch();
        if (mixinClassName.startsWith("dev.doctor4t.wathe.mixin.compat.hsr.")) return LegacyRoleAddons.needsHsrPatch();
        if (mixinClassName.startsWith("dev.doctor4t.wathe.mixin.compat.kinswathe.")) return LegacyRoleAddons.needsKinsPatch();
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            return !mixinClassName.contains("dev.doctor4t.wathe.mixin.compat.sodium");
        } else return !mixinClassName.equals("dev.doctor4t.wathe.mixin.client.scenery.SceneryWorldRendererMixin");
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
        // Constructors must change together with NEW/frame types. An injection after
        // construction would still resolve the removed class before it could run.
        if (mixinClassName.endsWith(".hsr.BanditRevolverReceiverMixin")) {
            migratePackets(classNode, Map.of(
                    "dev/doctor4t/wathe/util/ShootMuzzleS2CPayload", "dev/doctor4t/wathe/network/ShootMuzzleS2CPayload",
                    "dev/doctor4t/wathe/util/GunDropPayload", "dev/doctor4t/wathe/network/GunDropPayload"), 4);
        } else if (mixinClassName.endsWith(".kinswathe.DreamerKillerComponentMixin")) {
            migratePackets(classNode, Map.of(
                    "dev/doctor4t/wathe/util/AnnounceWelcomePayload", "dev/doctor4t/wathe/network/AnnounceWelcomePayload"), 2);
        }
    }

    private static void migratePackets(ClassNode target, Map<String, String> replacements, int expectedConstructors) {
        long constructors = target.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.name.equals("<init>") && replacements.containsKey(call.owner)).count();
        if (constructors != expectedConstructors) {
            throw new IllegalStateException("Unexpected legacy packet references in " + target.name + ": " + constructors);
        }
        ClassNode migrated = new ClassNode();
        target.accept(new ClassRemapper(migrated, new Remapper() {
            @Override
            public String map(String name) {
                return replacements.getOrDefault(name, name);
            }
        }));
        target.methods = migrated.methods;
        target.fields = migrated.fields;
    }

    @Override
    public void postApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo info) {
        if (mixinClassName.endsWith(".shooterpunishments.SavedSettingMixin")) {
            repairShooterCallback(classNode, "readFromNbt", false);
        } else if (mixinClassName.endsWith(".shooterpunishments.ItemPickupMixin")) {
            repairShooterCallback(classNode, "preventGunPickup", true);
        }
        if (!mixinClassName.endsWith(".noellesroles.ExecutionerAnnouncementMixin")) return;
        int corrected = 0;
        for (var method : classNode.methods) {
            if (!method.name.contains("executionerConfirm")) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals("java/util/ArrayList")
                        || !call.name.equals("indexOf") || !call.desc.equals("(Ljava/lang/Object;)I")) continue;
                var previous = instruction.getPrevious();
                while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                if (previous instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals("dev/doctor4t/wathe/api/WatheRoles") && field.name.equals("KILLER")) {
                    field.owner = "dev/doctor4t/wathe/client/gui/RoleAnnouncementTexts";
                    field.desc = "Ldev/doctor4t/wathe/client/gui/RoleAnnouncementTexts$RoleAnnouncementText;";
                    corrected++;
                }
            }
        }
        if (corrected != 1) throw new IllegalStateException("Unexpected NoellesRoles Executioner announcement sites: " + corrected);
    }

    private static void repairShooterCallback(ClassNode target, String callback, boolean pickup) {
        int repaired = 0;
        for (var method : target.methods) {
            if (!method.name.contains("$shooterpunishments$") || !method.name.endsWith("$" + callback)) continue;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            if (pickup) {
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "dev/doctor4t/wathe/compat/ShooterPunishmentsCompat", "onItemCollision",
                        "(L" + target.name + ";" + method.desc.substring(1), false));
            } else {
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "dev/doctor4t/wathe/compat/ShooterPunishmentsCompat", "readSetting",
                        "(" + Type.getArgumentTypes(method.desc)[0].getDescriptor() + ")V", false));
            }
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = pickup ? 3 : 1;
            repaired++;
        }
        if (repaired != 1) throw new IllegalStateException("Unexpected Shooter Punishments " + callback + " callbacks: " + repaired);
    }

}
