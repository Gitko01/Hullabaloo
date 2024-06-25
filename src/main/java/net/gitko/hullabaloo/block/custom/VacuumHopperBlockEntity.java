package net.gitko.hullabaloo.block.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.VacuumHopperScreenHandler;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.gitko.hullabaloo.util.ImplementedInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class VacuumHopperBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory, SidedInventory {

    private final DefaultedList<ItemEntity> detectedList = DefaultedList.ofSize(0);
    private final Hashtable<ItemEntity, Long> detectedListTimes = new Hashtable<>();
    public static final double SPEED = 0.125;
    // 0.65 from center
    private static final double INTAKE_RANGE = 0.8;
    private static final double DISTANCE_MULTIPLIER = 3;

    // north, east, south, west, up, down
    // 1 (north), 2 (east), 3 (south), 4 (west), 5 (up), 6 (down)
    // 0 = no IO
    // default: all outputs, no inputs (except for vacuum itself ofc)
    private int[] inputs = {0, 0, 0, 0, 0, 0};
    private int[] outputs = {1, 2, 3, 4, 5, 6};
    public int vacuumReach = 3;
    public static final int MAX_REACH = 5;
    public static final int FILTER_SLOT_INDEX = 0;
    public boolean legacyFilterSlotFixed = true;
    private int transferCooldown = 0;

    public VacuumHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VACUUM_HOPPER_BLOCK_ENTITY, pos, state);
    }

    // 11th slot is for a vacuum filter
    public static final int INV_SIZE = 11;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    // 0 = false, 1 = true
    public int pushMode = 1;

    // 0 = ignore, 1 = active if redstone active, 2 = active if redstone inactive
    public int redstoneMode = 0;

    public static void tick(World world, BlockPos pos, BlockState state, VacuumHopperBlockEntity be) {
        if (world.isClient()) return;

        // Since the filter slot had its ID updated in v1.1.0, we need to "migrate" the slot and swap the items from the old slot to the new slot
        if (!be.legacyFilterSlotFixed) {
            be.legacyFilterSlotFixed = true;
            ItemStack oldFilterSlot = be.getStack(10).copy();
            ItemStack newFilterSlot = be.getStack(VacuumHopperBlockEntity.FILTER_SLOT_INDEX).copy();
            be.setStack(VacuumHopperBlockEntity.FILTER_SLOT_INDEX, oldFilterSlot);
            be.setStack(10, newFilterSlot);
            be.markDirty();
        }

        // Auto-fix inputs and outputs if lengths incorrect
        if (be.getInputs().length < 6) {
            be.setInputs(new int[]{0, 0, 0, 0, 0, 0});
            be.sync();
        }
        if (be.getOutputs().length < 6) {
            be.setOutputs(new int[]{0, 0, 0, 0, 0, 0});
            be.sync();
        }

        double colliderXSize = state.getCollisionShape(world, pos).getBoundingBox().maxX + state.getCollisionShape(world, pos).getBoundingBox().minX;
        double colliderYSize = state.getCollisionShape(world, pos).getBoundingBox().maxY + state.getCollisionShape(world, pos).getBoundingBox().minY;
        double colliderZSize = state.getCollisionShape(world, pos).getBoundingBox().maxZ + state.getCollisionShape(world, pos).getBoundingBox().minZ;

        Vec3d posToTest;
        Vec3d centerPos = new Vec3d(pos.getX() + colliderXSize / 2,pos.getY() + colliderYSize / 2,pos.getZ() + colliderZSize / 2);

        int testX;
        int testY;
        int testZ;

        // Check redstone mode
        if (world.isReceivingRedstonePower(pos)) {
            if (be.getRedstoneMode() == 2) {
                be.resetMovingItems();
                return;
            }
        } else {
            if (be.getRedstoneMode() == 1) {
                be.resetMovingItems();
                return;
            }
        }

        // Push items to nearby storages
        if (be.getTransferCooldown() >= 2 && be.getPushMode() == 1) {
            be.setTransferCooldown(0);

            be.attemptExtraction(world, pos, state, be);
        } else if (be.getPushMode() == 1) {
            be.setTransferCooldown(be.getTransferCooldown() + 1);
        }

        int reach = be.getVacuumReach();

        // Pull items towards vacuum hopper
        for (var i = 0; i < be.getDetectedList().size(); i++) {
            try {
                ItemEntity ie = be.getDetectedList().get(i);

                be.setDetectedListTimes(ie, be.getDetectedListTimes().get(ie) + 1);

                // If an item has been in the range for 30 seconds and still hasn't been sucked in, we will assume it is stuck
                if (be.getDetectedListTimes().get(ie) >= 600) {
                    boolean passedFilter = true;

                    // check filter
                    Hashtable<Integer, Item> itemsToFilter;
                    int vacuumFilterMode;
                    ItemStack vacuumFilterStack = be.getStack(FILTER_SLOT_INDEX);
                    if (!vacuumFilterStack.isEmpty() && vacuumFilterStack.getItem() == ModItems.VACUUM_FILTER) {
                        itemsToFilter = VacuumFilterItem.getItemsToFilterFromNbtAsItems(vacuumFilterStack);
                        vacuumFilterMode = VacuumFilterItem.getModeFromNbt(vacuumFilterStack);

                        if (vacuumFilterMode == 0) {
                            if (!itemsToFilter.contains(ie.getStack().getItem())) passedFilter = false;
                        } else {
                            if (itemsToFilter.contains(ie.getStack().getItem())) passedFilter = false;
                        }
                    }

                    if (passedFilter) {
                        ie.setNoGravity(false);
                        be.removeFromDetectedList(ie);
                        be.removeFromDetectedListTimes(ie);

                        be.addStack(ie);
                    }
                } else {
                    //Vec3d itemPos = ie.getPos();
                    Vec3d itemPos = getCenterOfItemEntity(ie);

                    Vec3d direction = itemPos.relativize(centerPos);

                    double distance = centerPos.distanceTo(itemPos);

                    // Designed to speed up item as it gets closer to the vacuum hopper
                    Vec3d velocity = new Vec3d(direction.getX() * SPEED / (distance * DISTANCE_MULTIPLIER), direction.getY() * SPEED / (distance * DISTANCE_MULTIPLIER), direction.getZ() * SPEED / (distance * DISTANCE_MULTIPLIER));

                    ie.addVelocity(velocity.getX(), velocity.getY(), velocity.getZ());

                    if (!ie.hasNoGravity()) {
                        ie.setNoGravity(true);
                    }
                }
            } catch (Exception ignored) {}
        }

        for (testX = -reach; testX <= reach; testX++) {
            for (testY = -reach; testY <= reach; testY++) {
                for (testZ = -reach; testZ <= reach; testZ++) {
                    if (testX == 0 && testY == 0 && testZ == 0) {
                        continue;
                    }

                    posToTest = new Vec3d(pos.getX() + testX, pos.getY() + testY, pos.getZ() + testZ);

                    // find all items nearby
                    // is in lava is a test
                    List<ItemEntity> itemEntities = world.getEntitiesByClass(ItemEntity.class, Box.from(posToTest), e -> (
                            !e.isInLava()
                    ));

                    DefaultedList<ItemEntity> ignoredItemEntities = DefaultedList.ofSize(0);

                    // if one of the detected list items ISN'T FOUND then remove it
                    for (var i = 0; i < be.getDetectedList().size(); i++) {
                        try {
                            ItemEntity ie = be.getDetectedList().get(i);
                            Vec3d itemPos = getCenterOfItemEntity(ie);

                            if ((!itemEntities.contains(ie) && !centerPos.isInRange(itemPos, reach + 1)) || ie.isRemoved()) {
                                ie.setNoGravity(false);
                                be.removeFromDetectedList(ie);
                                be.removeFromDetectedListTimes(ie);
                            }
                        } catch (Exception ignored) {}
                    }

                    // take them items
                    for (ItemEntity itemEntity: itemEntities) {
                        // check filter
                        Hashtable<Integer, Item> itemsToFilter;
                        int vacuumFilterMode;
                        ItemStack vacuumFilterStack = be.getStack(FILTER_SLOT_INDEX);
                        if (!vacuumFilterStack.isEmpty() && vacuumFilterStack.getItem() == ModItems.VACUUM_FILTER) {
                            itemsToFilter = VacuumFilterItem.getItemsToFilterFromNbtAsItems(vacuumFilterStack);
                            vacuumFilterMode = VacuumFilterItem.getModeFromNbt(vacuumFilterStack);

                            if (vacuumFilterMode == 0) {
                                if (!itemsToFilter.contains(itemEntity.getStack().getItem())) continue;
                            } else {
                                if (itemsToFilter.contains(itemEntity.getStack().getItem())) continue;
                            }
                        }

                        if (centerPos.isInRange(getCenterOfItemEntity(itemEntity), INTAKE_RANGE)) {
                            if (be.getDetectedList().contains(itemEntity)) {
                                be.removeFromDetectedList(itemEntity);
                                be.removeFromDetectedListTimes(itemEntity);
                                itemEntity.setNoGravity(false);
                            }

                            be.addStack(itemEntity);
                        } else {
                            // tell vacuum hopper to pull item
                            if (!be.getDetectedList().contains(itemEntity)) {
                                be.addToDetectedList(itemEntity);
                                be.setDetectedListTimes(itemEntity, 0L);
                            }
                        }
                    }
                }
            }
        }

        // Animation
        if (world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) <= 1) {
            world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), 4));
        } else {
            world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) - 1));
        }
    }

    // Not sure if this is correct or not, pretty much just a guess
    // Take it with a grain of salt :)
    private static Vec3d getCenterOfItemEntity(ItemEntity ie) {
        float halfHeight = ie.getHeight() / 2;
        Vec3d pos = ie.getPos();

        return new Vec3d(pos.getX(), pos.getY() + halfHeight, pos.getZ());
    }

    private void attemptExtraction(World world, BlockPos pos, BlockState state, VacuumHopperBlockEntity be) {
        // Check slots, grab item from first slot found
        DefaultedList<ItemStack> availableItems = be.getItems();
        ItemStack itemToMove = null;

        for (ItemStack item : availableItems) {
            if (!item.isEmpty() && availableItems.indexOf(item) != FILTER_SLOT_INDEX) {
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

    private void attemptExtractTo(World world, BlockPos pos, BlockState state, VacuumHopperBlockEntity be, Direction direction, ItemStack itemToMove) {
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
                            be.markDirty();
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
                                        be.markDirty();
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

    private void addStack(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        boolean markedForRemoval = false;

        for (int i = 0; i < size(); i++) {
            if (i == FILTER_SLOT_INDEX) {
                continue;
            }

            ItemStack checkedStack = getStack(i);
            if (checkedStack.isEmpty()) {
                // set empty slot with stack
                itemEntity.remove(Entity.RemovalReason.DISCARDED);
                setStack(i, stack);

                return;
            }
            if (stack.getItem() == checkedStack.getItem()) {
                // add as much to stack as possible
                if (canMergeItems(stack, checkedStack)) {
                    int originalCount = stack.getCount();
                    boolean removed = false;
                    for (int c = 1; c <= originalCount; c++) {
                        if (checkedStack.getCount() < checkedStack.getMaxCount()) {
                            // add to checked stack
                            checkedStack.setCount(checkedStack.getCount() + 1);
                            stack.setCount(stack.getCount() - 1);
                            if (!removed) {
                                itemEntity.remove(Entity.RemovalReason.DISCARDED);
                            }
                        } else {
                            if (!removed) {
                                itemEntity.remove(Entity.RemovalReason.DISCARDED);
                            }
                            // throw out stack that is trying to be added if it can't find a slot
                            markedForRemoval = true;
                        }
                    }
                    if (!markedForRemoval) {
                        setStack(i, checkedStack);
                        return;
                    }
                }
            }
        }
        if (markedForRemoval) {
            ItemEntity itemStackEntity = new ItemEntity(this.getWorld(), this.getPos().getX(), this.getPos().getY() + 1, this.getPos().getZ(), stack);
            this.getWorld().spawnEntity(itemStackEntity);
        }
        // no available slots
        // so we are doing nothing :)
    }

    private boolean slotAvailable() {
        for (int i = 0; i < size(); i++) {
            ItemStack checkedStack = getStack(i);
            if (checkedStack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        // used in ScreenHandler
        buf.writeBlockPos(pos);
        buf.writeInt(redstoneMode);
        buf.writeInt(pushMode);
        buf.writeInt(vacuumReach);
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
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        Inventories.readNbt(nbt, items);
        this.inputs = nbt.getIntArray("inputs");
        this.outputs = nbt.getIntArray("outputs");
        this.vacuumReach = nbt.getInt("vacuumReach");
        this.redstoneMode = nbt.getInt("redstoneMode");
        this.pushMode = nbt.getInt("pushMode");
        if (!nbt.contains("legacyFilterSlotFixed")) {
            this.legacyFilterSlotFixed = false;
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        nbt.putIntArray("inputs", this.inputs);
        nbt.putIntArray("outputs", this.outputs);
        nbt.putInt("vacuumReach", this.vacuumReach);
        nbt.putInt("redstoneMode", this.redstoneMode);
        nbt.putInt("pushMode", this.pushMode);
        nbt.putBoolean("legacyFilterSlotFixed", this.legacyFilterSlotFixed);

        super.writeNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
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
        return new VacuumHopperScreenHandler(syncId, inv, this);
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
        // check inv
        if (!this.slotAvailable()) {return false;}

        int[] inputs = this.getInputs();

        // check direction
        switch (Objects.requireNonNull(direction)) {
            case NORTH -> {
                if (inputs[0] == 1) {
                    return true;
                }
            }
            case EAST -> {
                if (inputs[1] == 2) {
                    return true;
                }
            }
            case SOUTH -> {
                if (inputs[2] == 3) {
                    return true;
                }
            }
            case WEST -> {
                if (inputs[3] == 4) {
                    return true;
                }
            }
            case UP -> {
                if (inputs[4] == 5) {
                    return true;
                }
            }
            case DOWN -> {
                if (inputs[5] == 6) {
                    return true;
                }
            }
        }
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
            return ItemStack.areNbtEqual(first, second);
        }
    }

    public DefaultedList<ItemEntity> getDetectedList() {
        return this.detectedList;
    }

    public void addToDetectedList(ItemEntity ie) {
        if (!this.detectedList.contains(ie)) {
            this.detectedList.add(ie);
        }
    }

    public void removeFromDetectedList(ItemEntity ie) {
        this.detectedList.remove(ie);
    }

    public Hashtable<ItemEntity, Long> getDetectedListTimes() {
        return this.detectedListTimes;
    }

    public void setDetectedListTimes(ItemEntity ie, Long time) {
        this.detectedListTimes.put(ie, time);
    }

    public void removeFromDetectedListTimes(ItemEntity ie) {
        this.detectedListTimes.remove(ie);
    }

    public int getVacuumReach() {
        return this.vacuumReach;
    }

    public void setVacuumReach(int vacuumReach) {
        this.vacuumReach = vacuumReach;
    }

    public int[] getInputs() {
        return this.inputs;
    }

    public int[] getOutputs() {
        return this.outputs;
    }

    public void setInputs(int[] inputs) {
        this.inputs = inputs;
    }

    public void setOutputs(int[] outputs) {
        this.outputs = outputs;
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

    public int getTransferCooldown() {
        return this.transferCooldown;
    }

    public void setTransferCooldown(int newCooldown) {
        this.transferCooldown = newCooldown;
    }

    public void resetMovingItems() {
        try {
            for (ItemEntity ie : this.getDetectedList()) {
                ie.setNoGravity(false);
                this.removeFromDetectedList(ie);
            }
        } catch (Exception ignored) {}
    }
}
