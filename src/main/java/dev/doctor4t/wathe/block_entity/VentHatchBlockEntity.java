package dev.doctor4t.wathe.block_entity;

import dev.doctor4t.wathe.block.VentHatchBlock;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VentHatchBlockEntity extends SyncingBlockEntity {

    private String keyName = "";
    private int jammedTime = 0;
    private boolean blasted = false;

    public VentHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static <T extends BlockEntity> void serverTick(World world, BlockPos pos, BlockState state, T blockEntity) {
        VentHatchBlockEntity entity = (VentHatchBlockEntity) blockEntity;
        if (entity.isJammed()) {
            entity.setJammed(entity.getJammedTime() - 1);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("keyName", this.getKeyName());
        nbt.putInt("jammedTime", this.getJammedTime());
        nbt.putBoolean("blasted", this.isBlasted());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.setKeyName(nbt.getString("keyName"));
        this.setJammed(nbt.getInt("jammedTime"));
        this.setBlasted(nbt.getBoolean("blasted"));
    }

    public String getKeyName() {
        return this.keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setJammed(int time) {
        this.jammedTime = time;
    }

    public void jam() {
        this.setJammed(GameConstants.JAMMED_DOOR_TIME);
        if (this.world != null) {
            BlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof VentHatchBlock && state.get(VentHatchBlock.OPEN)) {
                this.world.setBlockState(this.pos, state.with(VentHatchBlock.OPEN, false), Block.NOTIFY_ALL);
            }
        }
        this.sync();
    }

    public boolean isJammed() {
        return this.jammedTime > 0;
    }

    public int getJammedTime() {
        return this.jammedTime;
    }

    public void setBlasted(boolean blasted) {
        this.blasted = blasted;
    }

    public void blast() {
        if (this.blasted) {
            return;
        }
        if (this.world != null) {
            BlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof VentHatchBlock && !state.get(VentHatchBlock.OPEN)) {
                this.world.setBlockState(this.pos, state.with(VentHatchBlock.OPEN, true), Block.NOTIFY_ALL);
            }
        }
        this.setBlasted(true);
        this.sync();
    }

    public boolean isBlasted() {
        return this.blasted;
    }
}
