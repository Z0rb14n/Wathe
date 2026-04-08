package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public record GunShootPayload(int target) implements CustomPayload {
    public static final Id<GunShootPayload> ID = new Id<>(Wathe.id("gunshoot"));
    public static final PacketCodec<PacketByteBuf, GunShootPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, GunShootPayload::target, GunShootPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<GunShootPayload> {
        private static final float UNPATCHED_CLIENT_GUN_RANGE = 15f;

        @Override
        public void receive(@NotNull GunShootPayload payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            ItemStack mainHandStack = player.getMainHandStack();
            if (!mainHandStack.isIn(WatheItemTags.GUNS)) return;
            if (player.getItemCooldownManager().isCoolingDown(mainHandStack.getItem())) return;

            player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(), WatheSounds.ITEM_REVOLVER_CLICK, SoundCategory.PLAYERS, 0.5f, 1f + player.getRandom().nextFloat() * .1f - .05f);

            // cancel if derringer has been shot
            Boolean isUsed = mainHandStack.get(WatheDataComponentTypes.USED);
            if (mainHandStack.isOf(WatheItems.DERRINGER)) {
                if (isUsed == null) {
                    isUsed = false;
                }

                if (isUsed) {
                    return;
                }

                if (!player.isCreative()) mainHandStack.set(WatheDataComponentTypes.USED, true);
            }

            Entity entity = player.getServerWorld().getEntityById(payload.target());

            if (payload.target() == -1 && WatheConfig.gunRange > UNPATCHED_CLIENT_GUN_RANGE) {
                // fix noelle's roles fake gun being actually working
                if (!Registries.ITEM.getId(mainHandStack.getItem()).getPath().contains("fake_revolver")) {
                    // hacky server side logic
                    // lag compensation? what's that?
                    HitResult hitResult = ProjectileUtil.getCollision(player, new PlayerPredicate(), WatheConfig.gunRange);
                    if (hitResult instanceof EntityHitResult entityHitResult) {
                        entity = entityHitResult.getEntity();
                    }
                }
            }

            if (entity instanceof PlayerEntity target && isWithinRange(player, target)) {
                GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
                Item revolver = WatheItems.REVOLVER;

                boolean backfire = false;

                if (game.isInnocent(target) && !player.isCreative() && mainHandStack.isOf(revolver)) {
                    // backfire: if you kill an innocent you have a chance of shooting yourself instead
                    if (game.isInnocent(player) && player.getRandom().nextFloat() <= game.getBackfireChance()) {
                        backfire = true;
                        GameFunctions.killPlayer(player, true, player, GameConstants.DeathReasons.GUN);
                    } else {
                        Scheduler.schedule(() -> {
                            if (!context.player().getInventory().contains((s) -> s.isIn(WatheItemTags.GUNS))) return;
                            player.getInventory().remove((s) -> s.isOf(revolver), 1, player.getInventory());
                            ItemEntity item = player.dropItem(revolver.getDefaultStack(), false, false);
                            if (item != null) {
                                item.setPickupDelay(10);
                                item.setThrower(player);
                            }
                            ServerPlayNetworking.send(player, new GunDropPayload());
                            PlayerMoodComponent.KEY.get(player).setMood(0);
                        }, 4);
                    }
                }

                if (!backfire) {
                    GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.GUN);
                }
            }

            player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(), WatheSounds.ITEM_REVOLVER_SHOOT, SoundCategory.PLAYERS, 5f, 1f + player.getRandom().nextFloat() * .1f - .05f);

            for (ServerPlayerEntity tracking : PlayerLookup.tracking(player))
                ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
            ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));
            if (!player.isCreative())
                player.getItemCooldownManager().set(mainHandStack.getItem(), GameConstants.ITEM_COOLDOWNS.getOrDefault(mainHandStack.getItem(), 0));
        }

        private static boolean isWithinRange(ServerPlayerEntity shooter, PlayerEntity target) {
            if (target.distanceTo(shooter) < WatheConfig.gunRange) return true;

            // Find the closest point on target hitbox to shooter eyes
            double closestX = Math.max(target.getX() - target.getWidth() / 2.0, Math.min(shooter.getX(), target.getX() + target.getWidth() / 2.0));
            double closestY = Math.max(target.getY(), Math.min(shooter.getEyeY(), target.getY() + target.getHeight()));
            double closestZ = Math.max(target.getZ() - target.getWidth() / 2.0, Math.min(shooter.getZ(), target.getZ() + target.getWidth() / 2.0));

            double dx = closestX - shooter.getX();
            double dy = closestY - shooter.getEyeY();
            double dz = closestZ - shooter.getZ();
            double eyeToBoxDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            return eyeToBoxDist < WatheConfig.gunRange;
        }

        // create a predicate rather than a lambda to prevent conflicts with More Shooter Punishments
        private static class PlayerPredicate implements Predicate<Entity> {

            @Override
            public boolean test(Entity entity) {
                return entity instanceof PlayerEntity otherPlayer && GameFunctions.isPlayerAliveAndSurvival(otherPlayer);
            }
        }
    }
}