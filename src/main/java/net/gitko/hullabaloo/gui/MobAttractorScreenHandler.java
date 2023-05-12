package net.gitko.hullabaloo.gui;

import net.gitko.hullabaloo.Hullabaloo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;

public class MobAttractorScreenHandler extends ScreenHandler {
    private BlockPos pos;
    private Vec3f range;
    PropertyDelegate energyAmountPropertyDelegate;

    // This constructor gets called on the client when the server wants it to open the screenHandler,
    // The client will call the other constructor with an empty Inventory and the screenHandler will automatically
    // sync this empty inventory with the inventory on the server.
    public MobAttractorScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packetByteBuf) {
        this(syncId, playerInventory, new ArrayPropertyDelegate(3));
        this.pos = packetByteBuf.readBlockPos();
        BlockPos range = packetByteBuf.readBlockPos();
        this.range = new Vec3f(range.getX(), range.getY(), range.getZ());
    }

    // This constructor gets called from the BlockEntity on the server without calling the other constructor first, the server knows the inventory of the container
    // and can therefore directly provide it as an argument. This inventory will then be synced to the client.
    public MobAttractorScreenHandler(int syncId, PlayerInventory playerInventory, PropertyDelegate energyAmountPropertyDelegate) {
        super(Hullabaloo.MOB_ATTRACTOR_SCREEN_HANDLER, syncId);

        // placeholder for server
        pos = BlockPos.ORIGIN;
        range = new Vec3f();

        // energy amount and drain amount
        this.energyAmountPropertyDelegate = energyAmountPropertyDelegate;
        this.addProperties(energyAmountPropertyDelegate);

        int m;
        int l;

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
    public ItemStack transferSlot(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Vec3f getRange() {
        return this.range;
    }

    public int getEnergyAmount(){
        return energyAmountPropertyDelegate.get(0);
    }

    public int getDrainAmount(){
        return energyAmountPropertyDelegate.get(1);
    }

    public int getCooldown(){
        return energyAmountPropertyDelegate.get(2);
    }
}
