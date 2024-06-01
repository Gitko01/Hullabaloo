package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateVacuumFilterModePacket(int mode) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, UpdateVacuumFilterModePacket> CODEC = CustomPayload.codecOf(UpdateVacuumFilterModePacket::write, UpdateVacuumFilterModePacket::new);
    public static final CustomPayload.Id<UpdateVacuumFilterModePacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_filter_mode_packet")));

    private UpdateVacuumFilterModePacket(RegistryByteBuf buf) {
        this(PacketCodecs.INTEGER.decode(buf));
    }

    public void write(RegistryByteBuf buf) {
        PacketCodecs.INTEGER.encode(buf, mode());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            int mode = payload.mode();

            context.player().server.execute(() -> {
                if (context.player().getStackInHand(context.player().getActiveHand()).getItem() == ModItems.VACUUM_FILTER) {
                    VacuumFilterItem.saveModeNbtData(context.player().getStackInHand(context.player().getActiveHand()), mode);
                }
            });
        });
    }
}
