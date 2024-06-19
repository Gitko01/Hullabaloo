package net.gitko.hullabaloo.network.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Hashtable;

public record UpdateVacuumFilterItemsPacket(Hashtable<Integer, ItemStack> itemsToFilter) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, UpdateVacuumFilterItemsPacket> CODEC = CustomPayload.codecOf(UpdateVacuumFilterItemsPacket::write, UpdateVacuumFilterItemsPacket::new);
    public static final CustomPayload.Id<UpdateVacuumFilterItemsPacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_filter_items_packet")));

    private UpdateVacuumFilterItemsPacket(RegistryByteBuf buf) {
        this(VacuumFilterItem.readItemsToFilterBuf(buf));
    }

    public void write(RegistryByteBuf buf) {
        VacuumFilterItem.createItemsToFilterBuf(itemsToFilter(), buf);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            Hashtable<Integer, ItemStack> itemsToFilter = payload.itemsToFilter();

            context.player().server.execute(() -> {
                if (context.player().getStackInHand(context.player().getActiveHand()).getItem() == ModItems.VACUUM_FILTER) {
                    VacuumFilterItem.saveItemsToFilterToNbt(context.player().getStackInHand(context.player().getActiveHand()), itemsToFilter);
                }
            });
        });
    }
}
