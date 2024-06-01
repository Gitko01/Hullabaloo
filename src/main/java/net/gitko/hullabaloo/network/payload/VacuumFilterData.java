package net.gitko.hullabaloo.network.payload;

import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Hashtable;

public record VacuumFilterData(int mode, Hashtable<Integer, ItemStack> itemsToFilter) {
    public static final PacketCodec<RegistryByteBuf, VacuumFilterData> PACKET_CODEC = PacketCodec.of(
            VacuumFilterData::write, VacuumFilterData::new
    );

    private VacuumFilterData(RegistryByteBuf buf) {
        this(
            PacketCodecs.INTEGER.decode(buf),
            VacuumFilterItem.readItemsToFilterBuf(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        PacketCodecs.INTEGER.encode(buf, mode());
        VacuumFilterItem.createItemsToFilterBuf(itemsToFilter(), buf);
    }
}