package net.gitko.hullabaloo.block.custom;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.item.ModItems;
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
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class CobblestoneGeneratorBlock extends BlockWithEntity {
    public static final MapCodec<CobblestoneGeneratorBlock> CODEC = createCodec(CobblestoneGeneratorBlock::new);

    public CobblestoneGeneratorBlock(Settings settings) {
        super(settings);
        setDefaultState(this.getDefaultState());
    }

    @Override
    protected MapCodec<CobblestoneGeneratorBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        CobblestoneGeneratorBlockEntity be = new CobblestoneGeneratorBlockEntity(pos, state);
        return be;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlocks.COBBLESTONE_GENERATOR_BLOCK_ENTITY, CobblestoneGeneratorBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getInventory().getStack(player.getInventory().selectedSlot).getItem() != ModItems.SCREWDRIVER) {
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
        } else {
            if (!world.isClient) {
                // Get side clicked on
                Direction sideHit = hit.getSide();
                toggleMode(sideHit, (CobblestoneGeneratorBlockEntity) world.getBlockEntity(pos));
            }

            return ActionResult.SUCCESS;
        }
    }

    public void toggleMode(Direction sideHit, CobblestoneGeneratorBlockEntity be) {
        int[] outputs = be.getOutputs();

        int indexNum = -1;
        int trueNum = -1;

        switch (sideHit) {
            case NORTH -> {
                indexNum = 0;
                trueNum = indexNum + 1;
            }
            case EAST -> {
                indexNum = 1;
                trueNum = indexNum + 1;
            }
            case SOUTH -> {
                indexNum = 2;
                trueNum = indexNum + 1;
            }
            case WEST -> {
                indexNum = 3;
                trueNum = indexNum + 1;
            }
            case UP -> {
                indexNum = 4;
                trueNum = indexNum + 1;
            }
            case DOWN -> {
                indexNum = 5;
                trueNum = indexNum + 1;
            }
        }

        if (indexNum != -1 && trueNum != -1) {
            if (outputs[indexNum] == 0) {
                outputs[indexNum] = trueNum;
            } else if (outputs[indexNum] == trueNum) {
                outputs[indexNum] = 0;
            }
            Hullabaloo.LOGGER.info("New outputs: " + Arrays.toString(outputs));

            be.setOutputs(outputs);
            be.sync();
        }
    }

    // This method will drop all items onto the ground when the block is broken
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CobblestoneGeneratorBlockEntity) {
                ItemScatterer.spawn(world, pos, (CobblestoneGeneratorBlockEntity) blockEntity);

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
}
