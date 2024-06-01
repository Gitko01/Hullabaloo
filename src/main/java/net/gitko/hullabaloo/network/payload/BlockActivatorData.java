package net.gitko.hullabaloo.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record BlockActivatorData(BlockPos pos, int mode, boolean roundRobin, int speed, int redstoneMode) {
    public static final PacketCodec<RegistryByteBuf, BlockActivatorData> PACKET_CODEC = PacketCodec.of(
            BlockActivatorData::write, BlockActivatorData::new
    );

    private BlockActivatorData(RegistryByteBuf buf) {
        this(
            BlockPos.PACKET_CODEC.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.BOOL.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.INTEGER.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, pos());
        PacketCodecs.INTEGER.encode(buf, mode());
        PacketCodecs.BOOL.encode(buf, roundRobin());
        PacketCodecs.INTEGER.encode(buf, speed());
        PacketCodecs.INTEGER.encode(buf, redstoneMode());
    }
}