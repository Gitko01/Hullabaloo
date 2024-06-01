package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.CobblestoneGeneratorBlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record UpdateCobblestoneGeneratorPushModePacket(int modeId, BlockPos pos) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, UpdateCobblestoneGeneratorPushModePacket> CODEC = CustomPayload.codecOf(UpdateCobblestoneGeneratorPushModePacket::write, UpdateCobblestoneGeneratorPushModePacket::new);
    public static final CustomPayload.Id<UpdateCobblestoneGeneratorPushModePacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "update_cobblestone_generator_push_mode_packet")));

    private UpdateCobblestoneGeneratorPushModePacket(RegistryByteBuf buf) {
        this(
            PacketCodecs.INTEGER.decode(buf),
            BlockPos.PACKET_CODEC.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        PacketCodecs.INTEGER.encode(buf, modeId());
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
                    CobblestoneGeneratorBlockEntity be = (CobblestoneGeneratorBlockEntity) context.player().getServerWorld().getBlockEntity(payload.pos());
                    assert be != null;

                    be.setPushMode(payload.modeId());
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }
}
