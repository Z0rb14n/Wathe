package dev.doctor4t.wathe.block;

import com.mojang.serialization.MapCodec;
import dev.doctor4t.wathe.api.event.AllowPlayerOpenLockedDoor;
import dev.doctor4t.wathe.block_entity.VentHatchBlockEntity;
import dev.doctor4t.wathe.index.WatheBlockEntities;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class VentHatchBlock extends WallMountedBlock implements BlockEntityProvider {

    public static final BooleanProperty OPEN = Properties.OPEN;
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(1, 1, 15, 15, 15, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 1, 1, 1, 15, 15);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(1, 1, 0, 15, 15, 1);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 1, 1, 16, 15, 15);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 1, 15);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(1, 15, 1, 15, 16, 15);
    private static final VoxelShape[] OPEN_WALL_SHAPES = {
            Block.createCuboidShape(1, 15, 0, 15, 16, 14),
            Block.createCuboidShape(2, 15, 1, 16, 16, 15),
            Block.createCuboidShape(1, 15, 2, 15, 16, 16),
            Block.createCuboidShape(0, 15, 1, 14, 16, 15)
    };
    private static final VoxelShape[] OPEN_CEILING_SHAPES = {
            Block.createCuboidShape(1, 2, 0, 15, 16, 1),
            Block.createCuboidShape(15, 2, 1, 16, 16, 15),
            Block.createCuboidShape(1, 2, 15, 15, 16, 16),
            Block.createCuboidShape(0, 2, 1, 1, 16, 15)
    };
    private static final VoxelShape[] OPEN_FLOOR_SHAPES = {
            Block.createCuboidShape(1, 0, 15, 15, 14, 16),
            Block.createCuboidShape(0, 0, 1, 1, 14, 15),
            Block.createCuboidShape(1, 0, 0, 15, 14, 1),
            Block.createCuboidShape(15, 0, 1, 16, 14, 15)
    };

    public VentHatchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(super.getDefaultState().with(OPEN, false));
    }

    @Override
    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return null;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VentHatchBlockEntity(WatheBlockEntities.VENT_HATCH, pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient || type != WatheBlockEntities.VENT_HATCH) {
            return null;
        }
        return VentHatchBlockEntity::serverTick;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof VentHatchBlockEntity entity) {
            if (entity.isBlasted()) {
                return ActionResult.PASS;
            }

            if (world.isClient) {
                return ActionResult.SUCCESS;
            }

            if (player.isCreative() || AllowPlayerOpenLockedDoor.EVENT.invoker().allowOpen(player)) {
                return this.toggle(state, world, pos);
            }

            boolean requiresKey = !entity.getKeyName().isEmpty();
            boolean hasLockpick = player.getMainHandStack().isOf(WatheItems.LOCKPICK);

            if (player.getMainHandStack().isOf(WatheItems.CROWBAR)) {
                return ActionResult.FAIL;
            }

            if (state.get(OPEN)) {
                return this.toggle(state, world, pos);
            } else if (entity.isJammed()) {
                world.playSound(null, pos, WatheSounds.BLOCK_VENT_HATCH_LOCKED, SoundCategory.BLOCKS, 1f, 1f);
                player.sendMessage(Text.translatable("tip.hatch.jammed"), true);
                return ActionResult.FAIL;
            } else if (requiresKey) {
                if (player.getMainHandStack().isOf(WatheItems.KEY) || hasLockpick) {
                    LoreComponent lore = player.getMainHandStack().get(DataComponentTypes.LORE);
                    boolean isRightKey = lore != null && !lore.lines().isEmpty() && lore.lines().getFirst().getString().equals(entity.getKeyName());
                    if (isRightKey || hasLockpick) {
                        if (isRightKey)
                            world.playSound(null, pos, WatheSounds.ITEM_KEY_DOOR, SoundCategory.BLOCKS, 1f, 1f);
                        if (hasLockpick)
                            world.playSound(null, pos, WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1f, 1f);
                        return this.toggle(state, world, pos);
                    } else {
                        world.playSound(null, pos, WatheSounds.BLOCK_VENT_HATCH_LOCKED, SoundCategory.BLOCKS, 1f, 1f);
                        player.sendMessage(Text.translatable("tip.hatch.requires_different_key"), true);
                        return ActionResult.FAIL;
                    }
                }

                world.playSound(null, pos, WatheSounds.BLOCK_VENT_HATCH_LOCKED, SoundCategory.BLOCKS, 1f, 1f);
                player.sendMessage(Text.translatable("tip.hatch.requires_key"), true);
                return ActionResult.FAIL;
            }
        }

        return this.toggle(state, world, pos);
    }

    private ActionResult toggle(BlockState state, World world, BlockPos pos) {
        VentHatchBlockEntity entity = world.getBlockEntity(pos) instanceof VentHatchBlockEntity e ? e : null;
        boolean locked = entity != null && !entity.getKeyName().isEmpty();
        boolean open = state.get(OPEN);
        world.setBlockState(pos, state.with(OPEN, !open));
        SoundEvent sound = locked
                ? (open ? WatheSounds.BLOCK_VENT_HATCH_CLOSE : WatheSounds.BLOCK_VENT_HATCH_OPEN)
                : (open ? SoundEvents.BLOCK_COPPER_TRAPDOOR_CLOSE : SoundEvents.BLOCK_COPPER_TRAPDOOR_OPEN);
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1f, locked ? 1f : 1.125f);
        return ActionResult.success(world.isClient);
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return this.getOutlineShape(state, world, pos, ShapeContext.absent());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(OPEN)) {
            Direction facing = state.get(FACING);
            return (switch (state.get(FACE)) {
                case CEILING -> OPEN_CEILING_SHAPES;
                case WALL -> OPEN_WALL_SHAPES;
                case FLOOR -> OPEN_FLOOR_SHAPES;
            })[facing.getHorizontal()];
        }
        return this.getShapeForState(state);
    }

    public VoxelShape getShapeForState(BlockState state) {
        return switch (VentHatchBlock.getDirection(state)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, OPEN);
    }
}
