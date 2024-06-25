package net.gitko.hullabaloo.block.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.CobblestoneGeneratorScreenHandler;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.network.payload.CobblestoneGeneratorData;
import net.gitko.hullabaloo.util.ImplementedInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class CobblestoneGeneratorBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<CobblestoneGeneratorData>, SidedInventory {
    public static final int UPGRADE_SLOT_INDEX = 0;
    public boolean legacyUpgradeSlotFixed = true;

    public int genSpeed = 0;
    public int genItemCount = 1;
    private int currentGenCooldown = 0;

    // north, east, south, west, up, down
    // 1 (north), 2 (east), 3 (south), 4 (west), 5 (up), 6 (down)
    // 0 = no IO
    // default: all outputs
    private int[] outputs = {1, 2, 3, 4, 5, 6};

    // levels of speed (measured in ticks)
    // stone, iron, gold, diamond, amethyst, netherite, ultimate
    // 6, 4, 1.6, 0.8, 0.6, 0.2, 0.05 seconds respectively
    private static final Hashtable<Item, Integer> genSpeeds = new Hashtable<Item, Integer>() {
        {
            put(Items.AIR, 120);
            put(ModItems.IRON_COBBLESTONE_GENERATOR_UPGRADE, 80);
            put(ModItems.GOLD_COBBLESTONE_GENERATOR_UPGRADE, 32);
            put(ModItems.DIAMOND_COBBLESTONE_GENERATOR_UPGRADE, 16);
            put(ModItems.AMETHYST_COBBLESTONE_GENERATOR_UPGRADE, 12);
            put(ModItems.NETHERITE_COBBLESTONE_GENERATOR_UPGRADE, 4);
            put(ModItems.ULTIMATE_COBBLESTONE_GENERATOR_UPGRADE, 1);
        }
    };

    // stone, iron, gold, diamond, amethyst, netherite, ultimate
    // 1, 1, 2, 4, 6, 8, 64 cobblestone respectively
    private static final Hashtable<Item, Integer> genItemCounts = new Hashtable<Item, Integer>() {
        {
            put(Items.AIR, 1);
            put(ModItems.IRON_COBBLESTONE_GENERATOR_UPGRADE, 1);
            put(ModItems.GOLD_COBBLESTONE_GENERATOR_UPGRADE, 2);
            put(ModItems.DIAMOND_COBBLESTONE_GENERATOR_UPGRADE, 4);
            put(ModItems.AMETHYST_COBBLESTONE_GENERATOR_UPGRADE, 6);
            put(ModItems.NETHERITE_COBBLESTONE_GENERATOR_UPGRADE, 8);
            put(ModItems.ULTIMATE_COBBLESTONE_GENERATOR_UPGRADE, 64);
        }
    };

    public CobblestoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COBBLESTONE_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    // 19th slot is the upgrade slot
    public static final int INV_SIZE = 19;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    // 0 = false, 1 = true
    public int pushMode = 1;

    // 0 = ignore, 1 = active if redstone active, 2 = active if redstone inactive
    public int redstoneMode = 0;

    public static void tick(World world, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be) {
        if (world.isClient()) return;

        // Since the upgrade slot had its ID updated in v1.1.0, we need to "migrate" the slot and swap the items from the old slot to the new slot
        if (!be.legacyUpgradeSlotFixed) {
            be.legacyUpgradeSlotFixed = true;
            ItemStack oldUpgradeSlot = be.getStack(18).copy();
            ItemStack newUpgradeSlot = be.getStack(CobblestoneGeneratorBlockEntity.UPGRADE_SLOT_INDEX).copy();
            be.setStack(CobblestoneGeneratorBlockEntity.UPGRADE_SLOT_INDEX, oldUpgradeSlot);
            be.setStack(18, newUpgradeSlot);
            be.markDirty();
        }

        if (be.outputs.length < 6) {
            int[] defaults = {1,2,3,4,5,6};
            be.setOutputs(defaults);
            be.sync();
        }

        // Check redstone mode
        if (world.isReceivingRedstonePower(pos)) {
            if (be.getRedstoneMode() == 2) {
                return;
            }
        } else {
            if (be.getRedstoneMode() == 1) {
                return;
            }
        }

        // Push items to nearby storages
        if (be.getPushMode() == 1) {
            be.attemptExtraction(world, pos, state, be);
        }

        // Check upgrades
        ItemStack upgradeStack = be.getStack(UPGRADE_SLOT_INDEX);
        boolean hasUpgrade = false;

        for (Item item : genSpeeds.keySet()) {
            if (upgradeStack.getItem() == item && !upgradeStack.isEmpty()) {
                be.setGenSpeed(genSpeeds.get(item));
                hasUpgrade = true;
            }
        }

        for (Item item : genItemCounts.keySet()) {
            if (upgradeStack.getItem() == item && !upgradeStack.isEmpty()) {
                be.setGenItemCount(genItemCounts.get(item));
                hasUpgrade = true;
            }
        }

        if (!hasUpgrade || upgradeStack.isEmpty()) {
            be.setGenSpeed(genSpeeds.get(Items.AIR));
            be.setGenItemCount(genItemCounts.get(Items.AIR));
        }

        // Attempt cobble generation
        if (be.getCurrentGenCooldown() >= be.getGenSpeed()) {
            be.setCurrentGenCooldown(0);

            ItemStack itemStack = new ItemStack(Items.COBBLESTONE);
            itemStack.setCount(be.genItemCount);

            be.addStack(itemStack);
            be.markDirty();
        } else {
            be.setCurrentGenCooldown(be.getCurrentGenCooldown() + 1);
        }
    }

    private void attemptExtraction(World world, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be) {
        // Check slots, grab item from first slot found
        DefaultedList<ItemStack> availableItems = be.getItems();
        ItemStack itemToMove = null;

        for (ItemStack item : availableItems) {
            if (!item.isEmpty() && availableItems.indexOf(item) != UPGRADE_SLOT_INDEX) {
                itemToMove = item;
                break;
            }
        }

        if (itemToMove != null) {
            // Found an item, now look for directions to use
            for (int directionNum : be.getOutputs()) {
                if (directionNum != 0) {
                    Direction direction = null;

                    switch (directionNum) {
                        case 1 -> {
                            direction = Direction.NORTH;
                        }
                        case 2 -> {
                            direction = Direction.EAST;
                        }
                        case 3 -> {
                            direction = Direction.SOUTH;
                        }
                        case 4 -> {
                            direction = Direction.WEST;
                        }
                        case 5 -> {
                            direction = Direction.UP;
                        }
                        case 6 -> {
                            direction = Direction.DOWN;
                        }
                    }

                    if (direction != null) {
                        be.attemptExtractTo(world, pos, state, be, direction, itemToMove);
                    }
                }
            }
        }
    }

    private void attemptExtractTo(World world, BlockPos pos, BlockState state, CobblestoneGeneratorBlockEntity be, Direction direction, ItemStack itemToMove) {
        Inventory inv = null;
        inv = getInventoryAtDirection(world, pos, state, direction);

        int itemToMoveIndex = be.getItems().indexOf(itemToMove);
        if (itemToMoveIndex != -1) {
            itemToMove = be.getItems().get(itemToMoveIndex);

            if (inv != null && !itemToMove.isEmpty()) {

                for (int i = 0; i < inv.size(); i++) {
                    if (canInsertIntoInv(inv, itemToMove, i, direction)) {
                        ItemStack checkedStack = inv.getStack(i);

                        if (checkedStack.isEmpty()) {
                            inv.setStack(i, itemToMove.copy());
                            be.removeStack(itemToMoveIndex);
                            break;

                        } else if (itemToMove.getItem() == checkedStack.getItem()) {
                            // check to see if the items can combine

                            if (checkedStack.isStackable()) {
                                int originalCount = itemToMove.getCount();

                                for (int c = 1; c <= originalCount; c++) {
                                    if (checkedStack.getCount() < checkedStack.getMaxCount()) {
                                        // add to checked stack
                                        checkedStack.setCount(checkedStack.getCount() + 1);
                                        itemToMove.setCount(itemToMove.getCount() - 1);
                                    }
                                }
                            }
                        }
                    }
                    // not insertable, move on to next slot...
                }
            }
        }
    }

    private void addStack(ItemStack stack) {
        boolean full = false;

        for (int i = 0; i < size(); i++) {
            if (i == UPGRADE_SLOT_INDEX) {
                continue;
            }

            ItemStack checkedStack = getStack(i);
            if (checkedStack.isEmpty()) {
                // set empty slot with stack
                setStack(i, stack);

                return;
            }
            if (stack.getItem() == checkedStack.getItem()) {
                // add as much to stack as possible
                if (canMergeItems(stack, checkedStack)) {
                    int originalCount = stack.getCount();

                    for (int c = 1; c <= originalCount; c++) {
                        if (checkedStack.getCount() < checkedStack.getMaxCount()) {
                            // add to checked stack
                            checkedStack.setCount(checkedStack.getCount() + 1);
                            stack.setCount(stack.getCount() - 1);
                        } else {
                            full = true;
                        }
                    }
                    if (!full) {
                        setStack(i, checkedStack);
                        return;
                    }
                }
            }
        }
        // no available slots
        // so we are doing nothing :)
    }

    @Override
    public CobblestoneGeneratorData getScreenOpeningData(ServerPlayerEntity player) {
        // used in ScreenHandler
        return new CobblestoneGeneratorData(this.getPos(), this.getRedstoneMode(), this.getPushMode());
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        Inventories.readNbt(nbt, items, registryLookup);
        this.redstoneMode = nbt.getInt("redstoneMode");
        this.pushMode = nbt.getInt("pushMode");
        this.outputs = nbt.getIntArray("outputs");
        if (!nbt.contains("legacyUpgradeSlotFixed")) {
            this.legacyUpgradeSlotFixed = false;
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(nbt, items, registryLookup);
        nbt.putInt("redstoneMode", this.redstoneMode);
        nbt.putInt("pushMode", this.pushMode);
        nbt.putIntArray("outputs", this.outputs);
        nbt.putBoolean("legacyUpgradeSlotFixed", this.legacyUpgradeSlotFixed);

        super.writeNbt(nbt, registryLookup);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // only needs to be called whenever the client needs the data for rendering immediately (blocks such as signs and banners need to use a function like this whenever data is updated)
    // just markDirty can be called for blocks such as chests and furnaces which only need the data when the GUI is opened
    public void sync() {
        assert this.getWorld() != null;
        if (!this.getWorld().isClient()) {
            // updates comparators and marks dirty
            this.markDirty();
            // let client know that the block has been updated
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getWorld().getBlockState(this.getPos()), Block.NOTIFY_LISTENERS);
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new CobblestoneGeneratorScreenHandler(syncId, inv, this);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // Return an array of all slots
        int[] result = new int[getItems().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }

        return result;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction direction) {
        int[] outputs = this.getOutputs();

        // check direction
        switch (Objects.requireNonNull(direction)) {
            case NORTH -> {
                if (outputs[0] == 1) {
                    return true;
                }
            }
            case EAST -> {
                if (outputs[1] == 2) {
                    return true;
                }
            }
            case SOUTH -> {
                if (outputs[2] == 3) {
                    return true;
                }
            }
            case WEST -> {
                if (outputs[3] == 4) {
                    return true;
                }
            }
            case UP -> {
                if (outputs[4] == 5) {
                    return true;
                }
            }
            case DOWN -> {
                if (outputs[5] == 6) {
                    return true;
                }
            }
        }
        return false;
    }

    // From HopperBlockEntity
    private static boolean canInsertIntoInv(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
        if (!inventory.isValid(slot, stack)) {
            return false;
        } else {
            return !(inventory instanceof SidedInventory) || ((SidedInventory)inventory).canInsert(slot, stack, side);
        }
    }

    // From HopperBlockEntity
    @Nullable
    public static Inventory getInventoryAt(World world, BlockPos pos) {
        return getInventoryAt(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    // Modified from HopperBlockEntity
    @Nullable
    private static Inventory getInventoryAtDirection(World world, BlockPos pos, BlockState state, Direction direction) {
        return getInventoryAt(world, pos.offset(direction));
    }

    // From HopperBlockEntity
    @Nullable
    private static Inventory getInventoryAt(World world, double x, double y, double z) {
        Inventory inventory = null;
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider)block).getInventory(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Inventory) {
                inventory = (Inventory)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }

        if (inventory == null) {
            List<Entity> list = world.getOtherEntities((Entity)null, new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntityPredicates.VALID_INVENTORIES);
            if (!list.isEmpty()) {
                inventory = (Inventory)list.get(world.random.nextInt(list.size()));
            }
        }

        return (Inventory)inventory;
    }

    // From HopperBlockEntity
    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (!first.isOf(second.getItem())) {
            return false;
        } else if (first.getDamage() != second.getDamage()) {
            return false;
        } else if (first.getCount() > first.getMaxCount()) {
            return false;
        } else {
            return ItemStack.areItemsAndComponentsEqual(first, second);
        }
    }

    public void setPushMode(int modeID) {
        this.pushMode = modeID;
    }

    public int getPushMode() {
        return this.pushMode;
    }

    public void setRedstoneMode(int modeID) {
        this.redstoneMode = modeID;
    }

    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    public int getCurrentGenCooldown() {
        return this.currentGenCooldown;
    }

    public void setCurrentGenCooldown(int newCooldown) {
        this.currentGenCooldown = newCooldown;
    }

    public int getGenSpeed() {
        return this.genSpeed;
    }

    public void setGenSpeed(int newSpeed) {
        this.genSpeed = newSpeed;
    }

    public int getGenItemCount() {
        return this.genItemCount;
    }

    public void setGenItemCount(int newCount) {
        this.genItemCount = newCount;
    }

    public int[] getOutputs() {
        return this.outputs;
    }

    public void setOutputs(int[] outputs) {
        this.outputs = outputs;
    }
}
