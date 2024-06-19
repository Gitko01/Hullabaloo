package net.gitko.hullabaloo.block.custom;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
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
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VacuumHopperBlock extends BlockWithEntity {
    public static final MapCodec<VacuumHopperBlock> CODEC = createCodec(VacuumHopperBlock::new);

    public static final IntProperty ANIM = IntProperty.of("anim", 1, 4);

    public VacuumHopperBlock(Settings settings) {
        super(settings);
        setDefaultState(this.getStateManager().getDefaultState()
                .with(ANIM, 1)
        );
    }

    @Override
    protected MapCodec<VacuumHopperBlock> getCodec() {
        return CODEC;
    };

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        //builder.add(IntProperty.of("anim", 1, 4));
        builder.add(ANIM);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        VacuumHopperBlockEntity be = new VacuumHopperBlockEntity(pos, state);
        return be;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlocks.VACUUM_HOPPER_BLOCK_ENTITY, VacuumHopperBlockEntity::tick);
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
                toggleMode(sideHit, (VacuumHopperBlockEntity) world.getBlockEntity(pos));
            }

            return ActionResult.SUCCESS;
        }
    }

    public void toggleMode(Direction sideHit, VacuumHopperBlockEntity be) {
        int[] inputs = be.getInputs();
        int[] outputs = be.getOutputs();

        switch (sideHit) {
            case NORTH -> {
                int indexNum = 0;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
            case EAST -> {
                int indexNum = 1;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
            case SOUTH -> {
                int indexNum = 2;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
            case WEST -> {
                int indexNum = 3;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
            case UP -> {
                int indexNum = 4;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
            case DOWN -> {
                int indexNum = 5;
                int trueNum = indexNum + 1;

                if (inputs[indexNum] == 0 && outputs[indexNum] == 0) {
                    // both no
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == 0) {
                    // only input
                    outputs[indexNum] = trueNum;
                    inputs[indexNum] = 0;

                } else if (inputs[indexNum] == 0 && outputs[indexNum] == trueNum) {
                    // only output
                    inputs[indexNum] = trueNum;

                } else if (inputs[indexNum] == trueNum && outputs[indexNum] == trueNum) {
                    // both yes
                    inputs[indexNum] = 0;
                    outputs[indexNum] = 0;

                }
            }
        }

        be.setInputs(inputs);
        be.setOutputs(outputs);
        be.sync();
    }

    // This method will drop all items onto the ground when the block is broken
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof VacuumHopperBlockEntity) {
                ItemScatterer.spawn(world, pos, (VacuumHopperBlockEntity) blockEntity);
                ((VacuumHopperBlockEntity) blockEntity).resetMovingItems();
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

    public static int getAnim(BlockState state) { return state.get(ANIM); }
}
