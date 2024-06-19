package net.gitko.hullabaloo.network.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.BlockActivatorBlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record UpdateBlockActivatorRoundRobinPacket(boolean roundRobin, BlockPos pos) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, UpdateBlockActivatorRoundRobinPacket> CODEC = CustomPayload.codecOf(UpdateBlockActivatorRoundRobinPacket::write, UpdateBlockActivatorRoundRobinPacket::new);
    public static final CustomPayload.Id<UpdateBlockActivatorRoundRobinPacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "update_block_activator_round_robin_packet")));

    private UpdateBlockActivatorRoundRobinPacket(RegistryByteBuf buf) {
        this(
            PacketCodecs.BOOL.decode(buf),
            BlockPos.PACKET_CODEC.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        PacketCodecs.BOOL.encode(buf, roundRobin());
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
                    BlockActivatorBlockEntity be = (BlockActivatorBlockEntity) context.player().getServerWorld().getBlockEntity(payload.pos());
                    assert be != null;

                    be.setRoundRobin(payload.roundRobin());
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }
}
