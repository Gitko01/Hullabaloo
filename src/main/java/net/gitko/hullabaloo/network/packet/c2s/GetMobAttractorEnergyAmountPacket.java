package net.gitko.hullabaloo.network.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.MobAttractorBlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record GetMobAttractorEnergyAmountPacket(BlockPos pos) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, GetMobAttractorEnergyAmountPacket> CODEC = CustomPayload.codecOf(GetMobAttractorEnergyAmountPacket::write, GetMobAttractorEnergyAmountPacket::new);
    public static final Id<GetMobAttractorEnergyAmountPacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "get_mob_attractor_energy_amount_packet")));

    private GetMobAttractorEnergyAmountPacket(RegistryByteBuf buf) {
        this(
            BlockPos.PACKET_CODEC.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, pos());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.player().server.execute(() -> {
                if (context.player().getServerWorld().isChunkLoaded(payload.pos().getX() / 16, payload.pos().getZ() / 16)) {
                    MobAttractorBlockEntity be = (MobAttractorBlockEntity) context.player().getServerWorld().getBlockEntity(payload.pos());
                    assert be != null;

                    be.sendEnergyAmountToClient(context.player());
                }
            });
        });
    }
}
