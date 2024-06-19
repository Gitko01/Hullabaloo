package net.gitko.hullabaloo.block.custom;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MobAttractorBlock extends BlockWithEntity {
    public static final MapCodec<MobAttractorBlock> CODEC = createCodec(MobAttractorBlock::new);

    public MobAttractorBlock(Settings settings) {
        super(settings);
        setDefaultState(this.getDefaultState());
    }

    @Override
    protected MapCodec<MobAttractorBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        MobAttractorBlockEntity be = new MobAttractorBlockEntity(pos, state);
        return be;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlocks.MOB_ATTRACTOR_BLOCK_ENTITY, MobAttractorBlockEntity::tick);
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

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }
}
