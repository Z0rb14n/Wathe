package dev.doctor4t.wathe.game.mapeffect;

import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

public abstract class KeyProvidingMapEffect extends MapEffect {
    public KeyProvidingMapEffect(Identifier identifier) {
        super(identifier);
    }
    protected void givePlayerKey(String keyName, ServerPlayerEntity player) {
        ItemStack itemStack = new ItemStack(WatheItems.KEY);
        itemStack.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> new LoreComponent(Text.literal(keyName)
                .getWithStyle(Style.EMPTY.withItalic(false).withColor(0xFF8C00))));
        player.giveItemStack(itemStack);
    }

    protected void provideKeysOnly(ServerWorld serverWorld, List<ServerPlayerEntity> players, int rooms) {
        Collections.shuffle(players);
        int roomNumber = 0;
        for (ServerPlayerEntity serverPlayerEntity : players) {
            roomNumber = roomNumber % rooms + 1;
            int finalRoomNumber = roomNumber;
            givePlayerKey("Room " + finalRoomNumber, serverPlayerEntity);
        }
    }

    protected void provideKeysAndLetters(ServerWorld serverWorld, List<ServerPlayerEntity> players, int rooms) {
        Collections.shuffle(players);
        int roomNumber = 0;
        for (ServerPlayerEntity serverPlayerEntity : players) {
            roomNumber = roomNumber % rooms + 1;
            int finalRoomNumber = roomNumber;
            givePlayerKey("Room " + finalRoomNumber, serverPlayerEntity);

            // give letter
            ItemStack letter = new ItemStack(WatheItems.LETTER);

            letter.set(DataComponentTypes.ITEM_NAME, Text.translatable(letter.getTranslationKey()));
            int letterColor = 0xC5AE8B;
            String tipString = "tip.letter.";
            letter.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> {
                        List<Text> text = new ArrayList<>();
                        UnaryOperator<Style> stylizer = style -> style.withItalic(false).withColor(letterColor);

                        Text displayName = serverPlayerEntity.getDisplayName();
                        String string = displayName != null ? displayName.getString() : serverPlayerEntity.getName().getString();
                        if (string.charAt(string.length() - 1) == '\uE780') { // remove ratty supporter icon
                            string = string.substring(0, string.length() - 1);
                        }

                        text.add(Text.translatable(tipString + "name", string).styled(style -> style.withItalic(false).withColor(0xFFFFFF)));
                        text.add(Text.translatable(tipString + "room").styled(stylizer));
                        text.add(Text.translatable(tipString + "tooltip1",
                                Text.translatable(tipString + "room." + switch (finalRoomNumber) {
                                    case 1 -> "grand_suite";
                                    case 2, 3 -> "cabin_suite";
                                    default -> "twin_cabin";
                                }).getString()
                        ).styled(stylizer));
                        text.add(Text.translatable(tipString + "tooltip2").styled(stylizer));

                        return new LoreComponent(text);
                    }
            );
            serverPlayerEntity.giveItemStack(letter);
        }
    }

    @Override
    public void finalizeMapEffects(ServerWorld serverWorld, List<ServerPlayerEntity> players) {
    }
}
