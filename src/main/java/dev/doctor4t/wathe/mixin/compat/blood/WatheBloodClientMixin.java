package dev.doctor4t.wathe.mixin.compat.blood;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.doctor4t.wathe.client.util.WatheBloodCompat;
import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "wathe_blood.WatheBloodClient", remap = false)
public abstract class WatheBloodClientMixin implements WatheBloodCompat.Effects {
    @Shadow protected abstract void howDoISpawnBlood(ClientWorld world, PlayerEntity victim, PlayerEntity killer, Identifier reason);
    @Shadow public abstract void spawnRandomBlood(ClientWorld world, Vec3d pos, double strength, int min, int max, Vec3d area);
    @Shadow public abstract void spawnBloodCloud(ClientWorld world, Vec3d pos, int min, int max, Vec3d area);

    // The addon's callback is client-only but AllowPlayerDeath is a server veto event.
    // Replace that subscription with confirmed server notifications, including in singleplayer.
    @WrapWithCondition(method = "onInitializeClient", at = @At(value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V", ordinal = 0))
    private boolean wathez$skipServerDeathListener(Event<?> event, Object listener) { return false; }

    @Inject(method = "onInitializeClient", at = @At("RETURN"))
    private void wathez$registerEffects(CallbackInfo ci) { WatheBloodCompat.effects = this; }

    @Override
    public void wathez$showBlood(ClientWorld world, PlayerEntity victim, @Nullable PlayerEntity killer, Identifier reason) {
        // 1.1.6 compares these identifiers by reference; decoded packets need the constants.
        Identifier canonical;
        int min, max;
        double strength, area;
        if (reason.equals(GameConstants.DeathReasons.GUN)) {
            canonical = GameConstants.DeathReasons.GUN; min = 18; max = 23; strength = 2.5; area = .5;
        } else if (reason.equals(GameConstants.DeathReasons.KNIFE)) {
            canonical = GameConstants.DeathReasons.KNIFE; min = 10; max = 13; strength = 1; area = .3;
        } else if (reason.equals(GameConstants.DeathReasons.BAT)) {
            canonical = GameConstants.DeathReasons.BAT; min = 5; max = 7; strength = .5; area = .2;
        } else if (reason.equals(GameConstants.DeathReasons.GRENADE)) {
            spawnRandomBlood(world, victim.getPos().add(0, .9, 0), 5, 25, 50, new Vec3d(.8, 2, .8));
            return;
        } else return;

        if (killer != null && killer != victim) {
            howDoISpawnBlood(world, victim, killer, canonical);
        } else {
            // Environmental/self-inflicted deaths have no meaningful attacker raycast.
            Vec3d pos = victim.getPos().add(0, .9, 0);
            Vec3d spread = new Vec3d(area, area, area);
            spawnBloodCloud(world, pos, min, max, spread);
            spawnRandomBlood(world, pos, strength, min, max, spread);
        }
    }
}
