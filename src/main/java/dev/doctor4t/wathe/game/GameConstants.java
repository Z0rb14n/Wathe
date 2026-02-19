package dev.doctor4t.wathe.game;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface GameConstants {
    // Logistics
    int FADE_TIME = 40;
    int FADE_PAUSE = 20;

    // Blocks
    int DOOR_AUTOCLOSE_TIME = getInTicks(0, 5);

    // Items
    Map<Item, Integer> ITEM_COOLDOWNS = new HashMap<>();

    static void init() {
        ITEM_COOLDOWNS.put(WatheItems.KNIFE, WatheConfig.knifeCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.REVOLVER, WatheConfig.revolverCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.DERRINGER, WatheConfig.derringerCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.GRENADE, WatheConfig.grenadeCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.LOCKPICK, WatheConfig.lockpickCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.CROWBAR, WatheConfig.crowbarCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.BODY_BAG, WatheConfig.bodyBagCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.PSYCHO_MODE, WatheConfig.psychoCooldownTicks);
        ITEM_COOLDOWNS.put(WatheItems.BLACKOUT, WatheConfig.blackoutCooldownTicks);

        SHOP_ENTRIES.clear();
        SHOP_ENTRIES.addAll(getShopEntries());
    }

    static List<ShopEntry> getShopEntries() {
        return Util.make(new ArrayList<>(), entries -> {
            entries.add(new ShopEntry(WatheItems.KNIFE.getDefaultStack(), WatheConfig.knifePrice, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(WatheItems.REVOLVER.getDefaultStack(), WatheConfig.revolverPrice, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(WatheItems.GRENADE.getDefaultStack(), WatheConfig.grenadePrice, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(WatheItems.PSYCHO_MODE.getDefaultStack(), WatheConfig.psychoPrice, ShopEntry.Type.WEAPON) {
                @Override
                public boolean onBuy(@NotNull PlayerEntity player) {
                    return PlayerShopComponent.usePsychoMode(player);
                }
            });
            entries.add(new ShopEntry(WatheItems.POISON_VIAL.getDefaultStack(), WatheConfig.poisonPrice, ShopEntry.Type.POISON));
            entries.add(new ShopEntry(WatheItems.SCORPION.getDefaultStack(), WatheConfig.scorpionPrice, ShopEntry.Type.POISON));
            entries.add(new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), WatheConfig.firecrackerPrice, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(WatheItems.LOCKPICK.getDefaultStack(), WatheConfig.lockpickPrice, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(WatheItems.CROWBAR.getDefaultStack(), WatheConfig.crowbarPrice, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(WatheItems.BODY_BAG.getDefaultStack(), WatheConfig.bodyBagPrice, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(WatheItems.BLACKOUT.getDefaultStack(), WatheConfig.blackoutPrice, ShopEntry.Type.TOOL) {
                @Override
                public boolean onBuy(@NotNull PlayerEntity player) {
                    return PlayerShopComponent.useBlackout(player);
                }
            });
            entries.add(new ShopEntry(new ItemStack(WatheItems.NOTE, 4), WatheConfig.notePrice, ShopEntry.Type.TOOL));
        });
    }

    int JAMMED_DOOR_TIME = getInTicks(1, 0);

    // Corpses
    int TIME_TO_DECOMPOSITION = getInTicks(1, 0);
    int DECOMPOSING_TIME = getInTicks(4, 0);

    // Task Variables
    float MOOD_GAIN = 0.5f;
    float MOOD_DRAIN = 1f / getInTicks(4, 0);
    int TIME_TO_FIRST_TASK = getInTicks(0, 30);
    int MIN_TASK_COOLDOWN = getInTicks(0, 30);
    int MAX_TASK_COOLDOWN = getInTicks(1, 0);
    int SLEEP_TASK_DURATION = getInTicks(0, 8);
    int OUTSIDE_TASK_DURATION = getInTicks(0, 8);
    float MID_MOOD_THRESHOLD = 0.55f;
    float DEPRESSIVE_MOOD_THRESHOLD = 0.2f;
    float ITEM_PSYCHOSIS_CHANCE = .5f; // in percent
    int ITEM_PSYCHOSIS_REROLL_TIME = 200;

    // Shop Variables
    List<ShopEntry> SHOP_ENTRIES = getShopEntries();
    Function<Long, Integer> PASSIVE_MONEY_TICKER = time -> {
        if (time % WatheConfig.ticksBetweenMoneyIncrement == 0) {
            return WatheConfig.moneyIncrementAmount;
        }
        return 0;
    };
    int PSYCHO_MODE_ARMOUR = 1;

    // Timers
    int PSYCHO_TIMER = getInTicks(0, 30);
    int FIRECRACKER_TIMER = getInTicks(0, 15);
    int BLACKOUT_MIN_DURATION = getInTicks(0, 15);
    int BLACKOUT_MAX_DURATION = getInTicks(0, 20);

    static int getInTicks(int minutes, int seconds) {
        return (minutes * 60 + seconds) * 20;
    }

    interface DeathReasons {
        Identifier GENERIC = Wathe.id("generic");
        Identifier KNIFE = Wathe.id("knife_stab");
        Identifier GUN = Wathe.id("gun_shot");
        Identifier BAT = Wathe.id("bat_hit");
        Identifier GRENADE = Wathe.id("grenade");
        Identifier POISON = Wathe.id("poison");
        Identifier FELL_OUT_OF_TRAIN = Wathe.id("fell_out_of_train");
    }
}