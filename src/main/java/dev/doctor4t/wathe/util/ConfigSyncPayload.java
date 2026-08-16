package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.item.RevolverItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

public record ConfigSyncPayload(int startingMoney, int moneyPerKill, int ticksBetweenMoneyIncrement,
                                int moneyIncrementAmount, int knifeCooldownTicks, int knifePrice,
                                int revolverCooldownTicks, int revolverPrice, int derringerCooldownTicks,
                                int grenadeCooldownTicks, int grenadePrice, int lockpickCooldownTicks,
                                int lockpickPrice, int crowbarCooldownTicks, int crowbarPrice, int bodyBagCooldownTicks,
                                int bodyBagPrice, int psychoCooldownTicks, int psychoPrice, int poisonPrice,
                                int scorpionPrice, int firecrackerPrice, int blackoutCooldownTicks, int blackoutPrice,
                                int notePrice, int ticksOnCivilianKill, int gunRange) implements CustomPayload {
    public static final Id<ConfigSyncPayload> ID = new Id<>(Wathe.id("config_sync"));
    public static final PacketCodec<PacketByteBuf, ConfigSyncPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.startingMoney());
        buf.writeInt(value.moneyPerKill());
        buf.writeInt(value.ticksBetweenMoneyIncrement());
        buf.writeInt(value.moneyIncrementAmount());
        buf.writeInt(value.knifeCooldownTicks());
        buf.writeInt(value.knifePrice());
        buf.writeInt(value.revolverCooldownTicks());
        buf.writeInt(value.revolverPrice());
        buf.writeInt(value.derringerCooldownTicks());
        buf.writeInt(value.grenadeCooldownTicks());
        buf.writeInt(value.grenadePrice());
        buf.writeInt(value.lockpickCooldownTicks());
        buf.writeInt(value.lockpickPrice());
        buf.writeInt(value.crowbarCooldownTicks());
        buf.writeInt(value.crowbarPrice());
        buf.writeInt(value.bodyBagCooldownTicks());
        buf.writeInt(value.bodyBagPrice());
        buf.writeInt(value.psychoCooldownTicks());
        buf.writeInt(value.psychoPrice());
        buf.writeInt(value.poisonPrice());
        buf.writeInt(value.scorpionPrice());
        buf.writeInt(value.firecrackerPrice());
        buf.writeInt(value.blackoutCooldownTicks());
        buf.writeInt(value.blackoutPrice());
        buf.writeInt(value.notePrice());
        buf.writeInt(value.ticksOnCivilianKill());
        buf.writeInt(value.gunRange());
    }, buf -> new ConfigSyncPayload(buf.readInt(), // startingMoney
            buf.readInt(), // moneyPerKill
            buf.readInt(), // ticksBetweenMoneyIncrement
            buf.readInt(), // moneyIncrementAmount
            buf.readInt(), // knifeCooldownTicks
            buf.readInt(), // knifePrice
            buf.readInt(), // revolverCooldownTicks
            buf.readInt(), // revolverPrice
            buf.readInt(), // derringerCooldownTicks
            buf.readInt(), // grenadeCooldownTicks
            buf.readInt(), // grenadePrice
            buf.readInt(), // lockpickCooldownTicks
            buf.readInt(), // lockpickPrice
            buf.readInt(), // crowbarCooldownTicks
            buf.readInt(), // crowbarPrice
            buf.readInt(), // bodyBagCooldownTicks
            buf.readInt(), // bodyBagPrice
            buf.readInt(), // psychoCooldownTicks
            buf.readInt(), // psychoPrice
            buf.readInt(), // poisonPrice
            buf.readInt(), // scorpionPrice
            buf.readInt(), // firecrackerPrice
            buf.readInt(), // blackoutCooldownTicks
            buf.readInt(), // blackoutPrice
            buf.readInt(), // notePrice
            buf.readInt(), // ticksOnCivilianKill
            buf.readInt()  // gunRange
    ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static ConfigSyncPayload fromConfig() {
        return new ConfigSyncPayload(WatheConfig.startingMoney, WatheConfig.moneyPerKill, WatheConfig.ticksBetweenMoneyIncrement, WatheConfig.moneyIncrementAmount, WatheConfig.knifeCooldownTicks, WatheConfig.knifePrice, WatheConfig.revolverCooldownTicks, WatheConfig.revolverPrice, WatheConfig.derringerCooldownTicks, WatheConfig.grenadeCooldownTicks, WatheConfig.grenadePrice, WatheConfig.lockpickCooldownTicks, WatheConfig.lockpickPrice, WatheConfig.crowbarCooldownTicks, WatheConfig.crowbarPrice, WatheConfig.bodyBagCooldownTicks, WatheConfig.bodyBagPrice, WatheConfig.psychoCooldownTicks, WatheConfig.psychoPrice, WatheConfig.poisonPrice, WatheConfig.scorpionPrice, WatheConfig.firecrackerPrice, WatheConfig.blackoutCooldownTicks, WatheConfig.blackoutPrice, WatheConfig.notePrice, WatheConfig.ticksOnCivilianKill, WatheConfig.gunRange);
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<ConfigSyncPayload> {
        @Override
        public void receive(@NotNull ConfigSyncPayload payload, ClientPlayNetworking.@NotNull Context context) {
            WatheConfig.startingMoney = payload.startingMoney();
            WatheConfig.moneyPerKill = payload.moneyPerKill();
            WatheConfig.ticksBetweenMoneyIncrement = payload.ticksBetweenMoneyIncrement();
            WatheConfig.moneyIncrementAmount = payload.moneyIncrementAmount();
            WatheConfig.knifeCooldownTicks = payload.knifeCooldownTicks();
            WatheConfig.knifePrice = payload.knifePrice();
            WatheConfig.revolverCooldownTicks = payload.revolverCooldownTicks();
            WatheConfig.revolverPrice = payload.revolverPrice();
            WatheConfig.derringerCooldownTicks = payload.derringerCooldownTicks();
            WatheConfig.grenadeCooldownTicks = payload.grenadeCooldownTicks();
            WatheConfig.grenadePrice = payload.grenadePrice();
            WatheConfig.lockpickCooldownTicks = payload.lockpickCooldownTicks();
            WatheConfig.lockpickPrice = payload.lockpickPrice();
            WatheConfig.crowbarCooldownTicks = payload.crowbarCooldownTicks();
            WatheConfig.crowbarPrice = payload.crowbarPrice();
            WatheConfig.bodyBagCooldownTicks = payload.bodyBagCooldownTicks();
            WatheConfig.bodyBagPrice = payload.bodyBagPrice();
            WatheConfig.psychoCooldownTicks = payload.psychoCooldownTicks();
            WatheConfig.psychoPrice = payload.psychoPrice();
            WatheConfig.poisonPrice = payload.poisonPrice();
            WatheConfig.scorpionPrice = payload.scorpionPrice();
            WatheConfig.firecrackerPrice = payload.firecrackerPrice();
            WatheConfig.blackoutCooldownTicks = payload.blackoutCooldownTicks();
            WatheConfig.blackoutPrice = payload.blackoutPrice();
            WatheConfig.notePrice = payload.notePrice();
            WatheConfig.ticksOnCivilianKill = payload.ticksOnCivilianKill();
            WatheConfig.gunRange = payload.gunRange();
            RevolverItem.clientGunRange = payload.gunRange();
            GameConstants.init();
        }
    }
}
