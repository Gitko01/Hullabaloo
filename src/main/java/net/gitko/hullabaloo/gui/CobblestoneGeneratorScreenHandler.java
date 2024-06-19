package net.gitko.hullabaloo.gui;

import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.CobblestoneGeneratorBlockEntity;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.network.payload.CobblestoneGeneratorData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class CobblestoneGeneratorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private BlockPos pos;
    private int redstoneMode;
    private int pushMode;

    // This constructor gets called on the client when the server wants it to open the screenHandler,
    // The client will call the other constructor with an empty Inventory and the screenHandler will automatically
    // sync this empty inventory with the inventory on the server.
    public CobblestoneGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, CobblestoneGeneratorData payload) {
        this(syncId, playerInventory, new SimpleInventory(CobblestoneGeneratorBlockEntity.INV_SIZE));
        this.pos = payload.pos();
        this.redstoneMode = payload.redstoneMode();
        this.pushMode = payload.pushMode();
    }

    // This constructor gets called from the BlockEntity on the server without calling the other constructor first, the server knows the inventory of the container
    // and can therefore directly provide it as an argument. This inventory will then be synced to the client.
    public CobblestoneGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(Hullabaloo.COBBLESTONE_GENERATOR_SCREEN_HANDLER, syncId);

        // placeholder for server
        pos = BlockPos.ORIGIN;
        redstoneMode = 0;
        pushMode = 0;

        checkSize(inventory, CobblestoneGeneratorBlockEntity.INV_SIZE);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        int m;
        int l;
        // Our inventory
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 6; ++l) {
                this.addSlot(new Slot(inventory, l + m * 6, 62 + l * 18, 17 + m * 18));
            }
        }
        // Upgrade slot
        this.addSlot(new Slot(inventory, CobblestoneGeneratorBlockEntity.UPGRADE_SLOT_INDEX, 27, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == ModItems.IRON_COBBLESTONE_GENERATOR_UPGRADE ||
                                stack.getItem() == ModItems.GOLD_COBBLESTONE_GENERATOR_UPGRADE ||
                                stack.getItem() == ModItems.DIAMOND_COBBLESTONE_GENERATOR_UPGRADE ||
                                stack.getItem() == ModItems.AMETHYST_COBBLESTONE_GENERATOR_UPGRADE ||
                                stack.getItem() == ModItems.NETHERITE_COBBLESTONE_GENERATOR_UPGRADE ||
                                stack.getItem() == ModItems.ULTIMATE_COBBLESTONE_GENERATOR_UPGRADE;
            }
        });

        // The player inventory
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        // The player Hotbar
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    // Shift + Player Inv Slot
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getRedstoneMode() {
        return this.redstoneMode;
    }

    public int getPushMode() {
        return this.pushMode;
    }
}
