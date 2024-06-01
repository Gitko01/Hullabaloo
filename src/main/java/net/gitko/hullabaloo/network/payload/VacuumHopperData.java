package net.gitko.hullabaloo.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record VacuumHopperData(BlockPos pos, int redstoneMode, int pushMode, int range) {
    public static final PacketCodec<RegistryByteBuf, VacuumHopperData> PACKET_CODEC = PacketCodec.of(
            VacuumHopperData::write, VacuumHopperData::new
    );

    private VacuumHopperData(RegistryByteBuf buf) {
        this(
            BlockPos.PACKET_CODEC.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.INTEGER.decode(buf),
            PacketCodecs.INTEGER.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, pos());
        PacketCodecs.INTEGER.encode(buf, redstoneMode());
        PacketCodecs.INTEGER.encode(buf, pushMode());
        PacketCodecs.INTEGER.encode(buf, range());
    }
}