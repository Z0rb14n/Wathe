package dev.doctor4t.wathe.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.timer.Timer;
import net.minecraft.world.timer.TimerCallbackSerializer;

import java.util.UUID;

public class MapWorldProperties implements ServerWorldProperties {
    private final String levelName;
    private final GameRules gameRules;
    private final Difficulty difficulty;

    private BlockPos spawnPos = BlockPos.ORIGIN;
    private float spawnAngle = 0f;
    private long time = 0L;
    private long timeOfDay = 6000L;
    private boolean thundering = false;
    private boolean raining = false;
    private int rainTime = 0;
    private int thunderTime = 0;
    private int clearWeatherTime = 6000;
    private int wanderingTraderSpawnDelay = 0;
    private int wanderingTraderSpawnChance = 0;
    private UUID wanderingTraderId = null;
    private GameMode gameMode = GameMode.ADVENTURE;
    private WorldBorder.Properties worldBorder = WorldBorder.DEFAULT_BORDER;
    private boolean initialized = true;
    private final Timer<MinecraftServer> scheduledEvents = new Timer<>(TimerCallbackSerializer.INSTANCE);

    public MapWorldProperties(String levelName, GameRules gameRules, Difficulty difficulty) {
        this.levelName = levelName;
        this.gameRules = gameRules;
        this.difficulty = difficulty;
    }

    @Override
    public String getLevelName() {
        return levelName;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    @Override
    public int getRainTime() {
        return rainTime;
    }

    @Override
    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public int getThunderTime() {
        return thunderTime;
    }

    @Override
    public int getClearWeatherTime() {
        return clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int clearWeatherTime) {
        this.clearWeatherTime = clearWeatherTime;
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return wanderingTraderSpawnDelay;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int delay) {
        this.wanderingTraderSpawnDelay = delay;
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return wanderingTraderSpawnChance;
    }

    @Override
    public void setWanderingTraderSpawnChance(int chance) {
        this.wanderingTraderSpawnChance = chance;
    }

    @Override
    public UUID getWanderingTraderId() {
        return wanderingTraderId;
    }

    @Override
    public void setWanderingTraderId(UUID id) {
        this.wanderingTraderId = id;
    }

    @Override
    public GameMode getGameMode() {
        return gameMode;
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    public void setWorldBorder(WorldBorder.Properties properties) {
        this.worldBorder = properties;
    }

    @Override
    public WorldBorder.Properties getWorldBorder() {
        return worldBorder;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public boolean areCommandsAllowed() {
        return true;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public void setTimeOfDay(long timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    @Override
    public Timer<MinecraftServer> getScheduledEvents() {
        return scheduledEvents;
    }

    @Override
    public void setSpawnPos(BlockPos pos, float angle) {
        this.spawnPos = pos;
        this.spawnAngle = angle;
    }

    @Override
    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    @Override
    public float getSpawnAngle() {
        return spawnAngle;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getTimeOfDay() {
        return timeOfDay;
    }

    @Override
    public boolean isThundering() {
        return thundering;
    }

    @Override
    public boolean isRaining() {
        return raining;
    }

    @Override
    public void setRaining(boolean raining) {
        this.raining = raining;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public GameRules getGameRules() {
        return gameRules;
    }

    @Override
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public boolean isDifficultyLocked() {
        return false;
    }
}
