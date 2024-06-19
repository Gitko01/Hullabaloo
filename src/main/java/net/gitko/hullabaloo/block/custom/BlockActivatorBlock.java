package net.gitko.hullabaloo.block.custom;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BlockActivatorBlock extends BlockWithEntity {
    public static final MapCodec<BlockActivatorBlock> CODEC = createCodec(BlockActivatorBlock::new);

    public static final DirectionProperty FACING = FacingBlock.FACING;
    public static final IntProperty ANIM = IntProperty.of("anim", 1, 4);
    public static final BooleanProperty ON = BooleanProperty.of("on");

    public BlockActivatorBlock(Settings settings) {
        super(settings);
        setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(ANIM, 1)
                .with(ON, false)
        );
    }

    @Override
    protected MapCodec<BlockActivatorBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(ANIM);
        builder.add(ON);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        BlockActivatorBlockEntity be = new BlockActivatorBlockEntity(pos, state);
        return be;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlocks.BLOCK_ACTIVATOR_BLOCK_ENTITY, BlockActivatorBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            // This will call the createScreenHandlerFactory method from BlockWithEntity, which will return our blockEntity casted to
            // a namedScreenHandlerFactory. If your block class does not extend BlockWithEntity, it needs to implement createScreenHandlerFactory.
            ExtendedScreenHandlerFactory screenHandlerFactory = (ExtendedScreenHandlerFactory) state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null) {
                // With this call the server will request the client to open the appropriate Screenhandler
                player.openHandledScreen(screenHandlerFactory);
            }
        }

        return ActionResult.SUCCESS;
    }

    // This method will drop all items onto the ground when the block is broken
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BlockActivatorBlockEntity) {
                ItemScatterer.spawn(world, pos, (BlockActivatorBlockEntity) blockEntity);
                // update comparators
                world.updateComparators(pos,this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    public static Direction getFacing(BlockState state) {
        return state.get(FACING);
    }

    public static Boolean getOn(BlockState state) { return state.get(ON); }

    public static int getAnim(BlockState state) { return state.get(ANIM); }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(Properties.FACING, ctx.getPlayerLookDirection().getOpposite());
    }
}
