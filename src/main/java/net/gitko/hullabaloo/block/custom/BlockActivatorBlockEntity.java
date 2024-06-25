package net.gitko.hullabaloo.block.custom;

import com.mojang.authlib.GameProfile;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.BlockActivatorScreenHandler;
import net.gitko.hullabaloo.util.ImplementedInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.gitko.hullabaloo.util.FakePlayerUtil.createFakePlayerBuilder;

public class BlockActivatorBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory {
    public BlockActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLOCK_ACTIVATOR_BLOCK_ENTITY, pos, state);
    }

    private int tickCount = 0;
    private int destroyTickCount = 0;
    private int tickInterval = 10;
    public int energyDecreasePerUse = 0;
    private static final float INEFFICIENCY = 2f;
    private static final int BASE_ENERGY_USAGE = 50;
    public static final int MAX_TICK_INTERVAL = 10;

    // Energy decrease per use: Math.round(Math.pow((float) BASE_ENERGY_USAGE / (float) tickInterval, INEFFICIENCY));
    // 10 tick interval: 25 energy per use
    // 5 tick interval: 100 energy per use
    // 3 tick interval: around 277 energy per use

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(9, ItemStack.EMPTY);

    private int lastSelectedItem = -1;

    private boolean roundRobin = false;

    // 0 = ignore, 1 = active if redstone active, 2 = active if redstone inactive
    public int redstoneMode = 0;

    // Create energy storage for block activator
    public static final int maxEnergyCapacity = 10000;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(maxEnergyCapacity, 5000, 0) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    // Sync energy amount to the screen
    private final PropertyDelegate energyAmountPropertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return (int) energyStorage.getAmount();
            } else if (index == 1) {
                if (tickInterval != 0) {
                    return Math.round((float) getEnergyDecreasePerUse() / (float) tickInterval);
                } else {
                    return 5000;
                }
            } else {
                return getEnergyDecreasePerUse();
            }
        }

        @Override
        public void set(int index, int value) {
            energyStorage.amount = value;
        }

        // make sure this is set correctly!
        // it will not work if set incorrectly
        @Override
        public int size() {
            return 3;
        }
    };

    private int mode = 0;

    private UUID uuid;

    private static final DefaultedList<BlockState> blocksBeingBroken = DefaultedList.ofSize(0);
    // Int in the hashtable below is the ID of the block entity
    private static final DefaultedList<Hashtable<UUID, Double>> blocksBeingBrokenProgresses = DefaultedList.ofSize(0);
    private static final DefaultedList<BlockPos> blocksBeingBrokenPositions = DefaultedList.ofSize(0);
    private static final Hashtable<ItemStack, BlockPos> itemsBeingUsedToBreakBlocks = new Hashtable<>();
    private static final Hashtable<ItemStack, BlockPos> itemsBeingUsedToBreakBlocksBlockEntity = new Hashtable<>();

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // We provide *this* to the screenHandler as our class Implements Inventory
        // Only the Server has the Inventory at the start, this will be synced to the client in the ScreenHandler
        return new BlockActivatorScreenHandler(syncId, playerInventory, this, this.energyAmountPropertyDelegate);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        // used in BlockActivatorScreenHandler
        packetByteBuf.writeBlockPos(pos);
        packetByteBuf.writeInt(mode);
        packetByteBuf.writeBoolean(roundRobin);
        packetByteBuf.writeInt(tickInterval);
        packetByteBuf.writeInt(redstoneMode);
    }

    private FakeServerPlayer fakeServerPlayer = null;

    public static void tick(World world, BlockPos pos, BlockState state, BlockActivatorBlockEntity be) {
        // BLOCK PLACE SETTING
        // SPLASH POTION / THROW MODE
        // ENCHANTS AFFECTING MINING
        // REVISE ITEM DROPPING METHOD TO ENSURE NO DESPAWNS
        // CLICKING WITH A STACK OF BUCKETS USES EVERY BUCKET IT CAN (COULD BE FIXED, BUT MAY BE LEFT IN)

        if (!world.isClient()) {
            if (be.tickInterval != 0) {
                be.energyDecreasePerUse = (int) Math.round(Math.pow((float) BASE_ENERGY_USAGE / (float) be.tickInterval, INEFFICIENCY));
            } else {
                be.energyDecreasePerUse = 5000;
            }

            // Check redstone mode
            if (world.isReceivingRedstonePower(pos)) {
                if (be.getRedstoneMode() == 2) {
                    be.setDestroyTickCount(0);
                    return;
                }
            } else {
                if (be.getRedstoneMode() == 1) {
                    be.setDestroyTickCount(0);
                    return;
                }
            }

            // mode 0: right click
            // mode 1: left click
            // mode 2: right click entity
            // mode 3: left click entity

            be.setTickCount(be.getTickCount() + 1);

            // Create the fake player with a unique UUID
            if (be.fakeServerPlayer == null) {
                UUID randUUID = UUID.randomUUID();
                be.fakeServerPlayer = createFakePlayerBuilder().create(
                        world.getServer(), (ServerWorld) world, new GameProfile(randUUID, "[Block Activator]")
                );
                be.uuid = randUUID;
            }

            Direction facing = BlockActivatorBlock.getFacing(state);

            BlockPos posToHit = pos.offset(facing);
            Vec3d posToHitVec3d = Vec3d.of(posToHit);

            be.fakeServerPlayer.setPosition(Vec3d.of(pos));

            // reset the left click
            if (be.mode == 1) {
                BlockState blockState = world.getBlockState(posToHit);
                Float blockHardness = blockState.getHardness(world, posToHit);

                if (!breakable(blockState, blockHardness)) {
                    be.setDestroyTickCount(0);
                    world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);
                }
            }

            if (be.getTickCount() >= be.getTickInterval() && be.energyStorage.amount >= be.energyDecreasePerUse) {
                be.setTickCount(0);

                be.energyStorage.amount -= be.energyDecreasePerUse;
                be.markDirty();

                // Handle clicking with items
                // Could possibly add a system that filters through every item and tries to use it
                ItemStack itemToClickWith = ItemStack.EMPTY;
                DefaultedList<ItemStack> items = be.getItems();

                int lastNonAirItem = 0;
                boolean allAir = true;

                for (ItemStack itemStack : items) {
                    if (itemStack != ItemStack.EMPTY && itemStack != Items.AIR.getDefaultStack() && itemStack.getCount() > 0) {
                        if (be.roundRobin) {
                            lastNonAirItem = items.indexOf(itemStack);
                            allAir = false;
                        }
                    }
                }

                if (allAir) {
                    be.lastSelectedItem = -1;
                }

                int slot = -1;

                for (ItemStack itemStack : items) {
                    slot += 1;
                    if (itemStack != ItemStack.EMPTY && itemStack != Items.AIR.getDefaultStack() && itemStack.getCount() > 0) {
                        if (!be.roundRobin) {
                            itemToClickWith = itemStack;
                            be.lastSelectedItem = items.indexOf(itemStack);

                            break;
                        } else {
                            if (be.lastSelectedItem == lastNonAirItem) {
                                if (items.indexOf(itemStack) < be.lastSelectedItem) {
                                    itemToClickWith = itemStack;
                                    be.lastSelectedItem = items.indexOf(itemStack);
                                    break;
                                }
                            } else if (items.indexOf(itemStack) > be.lastSelectedItem) {
                                itemToClickWith = itemStack;
                                be.lastSelectedItem = items.indexOf(itemStack);
                                break;
                            }
                        }
                    }
                }

                // add the item to the fake player's inventory
                be.fakeServerPlayer.getInventory().main.set(0, itemToClickWith);
                be.fakeServerPlayer.getInventory().selectedSlot = 0;

                if (be.mode == 0) {
                    // right click

                    // Animation (slowly open)
                    if (world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) <= 1) {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), 4));
                    } else {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) - 1));
                    }

                    DamageSource dmgSource = world.getDamageSources().playerAttack(be.fakeServerPlayer);
                    List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, Box.from(posToHitVec3d), e -> (
                            //e.getType() != EntityType.PLAYER &&
                            //e.getType() != EntityType.ARMOR_STAND &&
                            !e.isInvulnerableTo(dmgSource) &&
                                    !e.isDead()
                    ));

                    if (entities.isEmpty()) {
                        // not an entity, try clicking on a block
                        clickRight(be, posToHitVec3d, itemToClickWith, world, posToHit);
                    } else {
                        clickEntityRight(be, entities);
                    }


                } else if (be.mode == 1) {
                    // left click

                    // Animation (slowly close)
                    if (world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) >= 4) {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), 1));
                    } else {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) + 1));
                    }

                    DamageSource dmgSource = world.getDamageSources().playerAttack(be.fakeServerPlayer);
                    List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, Box.from(posToHitVec3d), e -> (
                            !e.isInvulnerableTo(dmgSource) &&
                            !e.isDead()
                    ));

                    if (entities.isEmpty()) {
                        // not an entity, try clicking on a block
                        clickLeft(itemToClickWith, world, pos, posToHit, be);
                    } else {
                        clickEntityLeft(entities, be);
                    }
                }

                ItemStack fakeInventoryItem = be.fakeServerPlayer.getInventory().main.get(0);

                if (fakeInventoryItem == null)
                    return;

                if (fakeInventoryItem.getItem() == Items.AIR)
                    return;

                items.set(slot, fakeInventoryItem);

                for (int fakeInventorySlot = 1; fakeInventorySlot < be.fakeServerPlayer.getInventory().size(); fakeInventorySlot++) {
                    ItemStack itemStack = be.fakeServerPlayer.getInventory().getStack(fakeInventorySlot);

                    if (itemStack == null)
                        continue;

                    if (itemStack.getItem() == Items.AIR)
                        continue;

                    int nextAirSlot = -1;

                    for (int itemsSlot = 0; itemsSlot < items.size(); itemsSlot++) {
                        ItemStack item = items.get(itemsSlot);

                        if (item == null)
                            continue;

                        if (item.getItem() != Items.AIR)
                            continue;

                        nextAirSlot = itemsSlot;
                        break;
                    }

                    if (nextAirSlot == -1) {
                        ItemEntity itemEntity = new ItemEntity(world, pos.getX() + .5, pos.getY() + 1.5, pos.getZ() + .5, itemStack);

                        double xVelocity = ThreadLocalRandom.current().nextDouble(.2);

                        if (ThreadLocalRandom.current().nextBoolean())
                            xVelocity = -xVelocity;

                        double zVelocity = ThreadLocalRandom.current().nextDouble(.2);

                        if (ThreadLocalRandom.current().nextBoolean())
                            zVelocity = -zVelocity;

                        world.spawnEntity(itemEntity);

                        itemEntity.setVelocity(new Vec3d(xVelocity, .2, zVelocity));
                        continue;
                    }

                    items.set(nextAirSlot, itemStack);
                }

                be.fakeServerPlayer.getInventory().clear();
                be.markDirty();
            }

            if (be.energyStorage.amount < be.energyDecreasePerUse) {
                if (world.getBlockState(pos).get(BooleanProperty.of("on"))) {
                    world.setBlockState(pos, state.with(BooleanProperty.of("on"), false));
                }
            } else {
                if (!world.getBlockState(pos).get(BooleanProperty.of("on"))) {
                    world.setBlockState(pos, state.with(BooleanProperty.of("on"), true));
                }
            }
        }
    }

    public static void clickLeft(ItemStack itemToClickWith, World world, BlockPos pos, BlockPos posToHit, BlockActivatorBlockEntity be) {
        // left-click on a block
        BlockState blockState = world.getBlockState(posToHit);
        Float blockHardness = blockState.getHardness(world, posToHit);

        if (!breakable(blockState, blockHardness)) {
            be.setDestroyTickCount(0);
            world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);

            // remove old blocks that were removed by an external source
            try {
                for (BlockPos blocksBeingBrokenPos : blocksBeingBrokenPositions) {
                    if (posToHit.equals(blocksBeingBrokenPos)) {
                        blocksBeingBrokenProgresses.remove(blocksBeingBrokenPositions.indexOf(blocksBeingBrokenPos));
                        blocksBeingBroken.remove(blocksBeingBrokenPositions.indexOf(blocksBeingBrokenPos));
                        blocksBeingBrokenPositions.remove(blocksBeingBrokenPos);
                    }
                }
            } catch (Exception ignored) {}
        }

        Float breakSpeed = itemToClickWith.getMiningSpeedMultiplier(blockState);
        double destroyProgress = be.getDestroyTickCount() * 0.001 * (breakSpeed / blockHardness) * 150;
        double globalDestroyProgress = 0;

        be.setDestroyTickCount(be.getDestroyTickCount() + 1);

        // set item being used to break blocks
        // key = item
        // value = block pos

        // reset items to ensure the block activator doesn't try to use 2+ items at once
        try {
            for (ItemStack item : itemsBeingUsedToBreakBlocksBlockEntity.keySet()) {
                if (itemsBeingUsedToBreakBlocksBlockEntity.get(item).equals(pos)) {
                    itemsBeingUsedToBreakBlocksBlockEntity.remove(item);
                    itemsBeingUsedToBreakBlocks.remove(item);
                }
            }
        } catch (Exception ignored) {}

        // add item to click with into the array of items being used
        if (!itemsBeingUsedToBreakBlocks.containsKey(itemToClickWith)) {
            itemsBeingUsedToBreakBlocks.put(itemToClickWith, posToHit);
            itemsBeingUsedToBreakBlocksBlockEntity.put(itemToClickWith, pos);
        }

        // check to make sure block pos hasn't been updated
        if (blocksBeingBroken.contains(blockState)) {
            if (!blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                blocksBeingBrokenProgresses.remove(blocksBeingBroken.indexOf(blockState));
                blocksBeingBrokenPositions.remove(blocksBeingBroken.indexOf(blockState));
                blocksBeingBroken.remove(blockState);
            }
        }

        if (!blocksBeingBroken.contains(blockState)) {
            blocksBeingBroken.add(blockState);
            blocksBeingBrokenProgresses.add(new Hashtable<>());
            blocksBeingBrokenPositions.add(posToHit);
        }

        // find destroy progress from other block activators
        if (blocksBeingBroken.contains(blockState)) {
            if (blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                Hashtable<UUID, Double> progressList = blocksBeingBrokenProgresses.get(blocksBeingBroken.indexOf(blockState));

                if (!progressList.contains(be.uuid)) {
                    progressList.put(be.uuid, destroyProgress);
                } else {
                    progressList.remove(be.uuid);
                    progressList.put(be.uuid, destroyProgress);
                }

                // grab each destroy progress reported by each block activator, place it into one variable
                for (Double aDouble : blocksBeingBrokenProgresses.get(blocksBeingBroken.indexOf(blockState)).values()) {
                    globalDestroyProgress += aDouble;
                }
            }
        }

        // Find the fastest tool under toolsBeingUsedToBreakBlocks, if that tool matches the held tool of
        // the current block entity, then use it.
        ItemStack bestItem = itemToClickWith;

        for (ItemStack item : itemsBeingUsedToBreakBlocks.keySet()) {
            if (itemsBeingUsedToBreakBlocks.get(item).equals(posToHit)) {
                if (item != ItemStack.EMPTY && item != Items.AIR.getDefaultStack() && item.getCount() > 0) {
                    if (item.getMiningSpeedMultiplier(blockState) > bestItem.getMiningSpeedMultiplier(blockState)) {
                        if (item.isDamageable() && bestItem.isDamageable()) {
                            float itemDurability = (float) ((item.getMaxDamage() - item.getDamage()) / item.getMaxDamage());
                            float bestItemDurability = (float) ((bestItem.getMaxDamage() - bestItem.getDamage()) / bestItem.getMaxDamage());

                            if (itemDurability > bestItemDurability) {
                                bestItem = item;
                            }
                        } else {
                            bestItem = item;
                        }
                    }
                }
            }
        }

        // Start of the ACTUAL block destruction
        if (globalDestroyProgress >= 10) {
            be.setDestroyTickCount(0);

            if (bestItem.equals(itemToClickWith)) {
                world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);
                be.fakeServerPlayer.interactionManager.tryBreakBlock(posToHit);

                // do damage to others too
                for (ItemStack item : itemsBeingUsedToBreakBlocks.keySet()) {
                    if (itemsBeingUsedToBreakBlocks.get(item).equals(posToHit)) {
                        if (!item.equals(bestItem)) {
                            if (item.isDamageable()) {
                                item.damage(1, be.fakeServerPlayer, fakeServerPlayer1 -> {
                                });
                            }
                        }
                    }
                }
            }

            // remember to remove block from the block state list when removed!
            if (blocksBeingBroken.contains(blockState)) {
                if (blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                    blocksBeingBrokenProgresses.remove(blocksBeingBroken.indexOf(blockState));
                    blocksBeingBrokenPositions.remove(blocksBeingBroken.indexOf(blockState));
                    blocksBeingBroken.remove(blockState);
                }
            }

            return;
        }

        if (bestItem.equals(itemToClickWith)) {
            world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, (int) globalDestroyProgress);
        }
    }

    public static void clickRight(BlockActivatorBlockEntity be, Vec3d posToHitVec3d, ItemStack itemToClickWith, World world, BlockPos posToHit) {
        // right-click on a block
        be.fakeServerPlayer.interactAt(be.fakeServerPlayer, posToHitVec3d, Hand.MAIN_HAND);
        be.fakeServerPlayer.interactionManager.interactBlock(
                be.fakeServerPlayer,
                world,
                itemToClickWith,
                Hand.MAIN_HAND,
                new BlockHitResult(posToHitVec3d, Direction.UP, posToHit, false)
        );
    }

    public static void clickEntityLeft(List<LivingEntity> entities, BlockActivatorBlockEntity be) {
        // left-click on an entity
        for (LivingEntity entity: entities) {
            be.fakeServerPlayer.attack(entity);
            break;
        }
    }

    public static void clickEntityRight(BlockActivatorBlockEntity be, List<LivingEntity> entities) {
        // right-click on an entity
        for (LivingEntity entity: entities) {
            be.fakeServerPlayer.interact(entity, Hand.MAIN_HAND);
            break;
        }
    }

    public static boolean breakable(BlockState blockState, Float blockHardness) {
        return !blockState.getMaterial().isLiquid() && blockState.getBlock() != Blocks.AIR && blockHardness != -1f;
    }

    public int getDestroyTickCount() {
        return this.destroyTickCount;
    }

    public void setDestroyTickCount(int destroyTickCount) {
        this.destroyTickCount = destroyTickCount;
    }

    public int getEnergyDecreasePerUse() {
        return this.energyDecreasePerUse;
    }

    public int getTickCount() {
        return this.tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    public int getTickInterval() {
        return this.tickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public void setMode(int modeID) {
        this.mode = modeID;
    }

    public void setRoundRobin(boolean on) {
        this.roundRobin = on;
    }

    public void setRedstoneMode(int modeID) {
        this.redstoneMode = modeID;
    }

    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        this.mode = nbt.getInt("mode");
        this.roundRobin = nbt.getBoolean("roundRobin");
        this.energyStorage.amount = nbt.getLong("energyAmount");
        this.redstoneMode = nbt.getInt("redstoneMode");
        this.tickInterval = nbt.getInt("tickInterval");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        nbt.putInt("mode", this.mode);
        nbt.putBoolean("roundRobin", this.roundRobin);
        nbt.putLong("energyAmount", this.energyStorage.amount);
        nbt.putInt("redstoneMode", this.redstoneMode);
        nbt.putInt("tickInterval", this.tickInterval);
        super.writeNbt(nbt);
    }

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
}
