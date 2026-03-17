package dev.doctor4t.wathe.api;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.game.mapeffect.*;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class WatheMapEffects {
    public static final HashMap<Identifier, MapEffect> MAP_EFFECTS = new HashMap<>();

    public static final Identifier HARPY_EXPRESS_LOBBY_ID = Wathe.id("harpy_express_lobby");
    public static final Identifier HARPY_EXPRESS_NIGHT_ID = Wathe.id("harpy_express_night");
    public static final Identifier HARPY_EXPRESS_DAY_ID = Wathe.id("harpy_express_day");
    public static final Identifier HARPY_EXPRESS_SUNDOWN_ID = Wathe.id("harpy_express_sundown");
    public static final Identifier HOTEL_ID = Wathe.id("hotel");
    public static final Identifier GENERIC_ID = Wathe.id("generic");
    public static final Identifier FNAF_TEST_ID = Wathe.id("fnaf_test");

    public static final MapEffect HARPY_EXPRESS_LOBBY = registerMapEffect(HARPY_EXPRESS_LOBBY_ID, new HarpyExpressLobbyMapEffect(HARPY_EXPRESS_LOBBY_ID));
    public static final MapEffect HARPY_EXPRESS_NIGHT = registerMapEffect(HARPY_EXPRESS_NIGHT_ID, new HarpyExpressNightTrainMapEffect(HARPY_EXPRESS_NIGHT_ID));
    public static final MapEffect HARPY_EXPRESS_DAY = registerMapEffect(HARPY_EXPRESS_DAY_ID, new HarpyExpressDayTrainMapEffect(HARPY_EXPRESS_DAY_ID));
    public static final MapEffect HARPY_EXPRESS_SUNDOWN = registerMapEffect(HARPY_EXPRESS_SUNDOWN_ID, new HarpyExpressSundownTrainMapEffect(HARPY_EXPRESS_SUNDOWN_ID));
    @SuppressWarnings("unused")
    public static final MapEffect HOTEL = registerMapEffect(HOTEL_ID, new HotelMapEffect(HOTEL_ID));
    public static final MapEffect GENERIC = registerMapEffect(GENERIC_ID, new GenericMapEffect(GENERIC_ID));
    @SuppressWarnings("unused")
    public static final MapEffect FNAF_TEST = registerMapEffect(FNAF_TEST_ID, new FNAFTestMapEffect(FNAF_TEST_ID));

    public static MapEffect registerMapEffect(Identifier identifier, MapEffect mapEffect) {
        MAP_EFFECTS.put(identifier, mapEffect);
        return mapEffect;
    }

    public static MapEffect getMapEffect(String id) {
        return MAP_EFFECTS.get(Wathe.id(id));
    }
}
