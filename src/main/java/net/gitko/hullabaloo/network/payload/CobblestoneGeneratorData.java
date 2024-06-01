package net.gitko.hullabaloo.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record CobblestoneGeneratorData(BlockPos pos, int redstoneMode, int pushMode) {
    public static final PacketCodec<RegistryByteBuf, CobblestoneGeneratorData> PACKET_CODEC = PacketCodec.of(
            CobblestoneGeneratorData::write, CobblestoneGeneratorData::new
    );

    private CobblestoneGeneratorData(RegistryByteBuf buf) {
        this(
            BlockPos.PACKET_CODEC.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.INTEGER.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, pos());
        PacketCodecs.INTEGER.encode(buf, redstoneMode());
        PacketCodecs.INTEGER.encode(buf, pushMode());
    }
}
