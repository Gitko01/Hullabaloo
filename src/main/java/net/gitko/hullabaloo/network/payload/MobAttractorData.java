package net.gitko.hullabaloo.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

public record MobAttractorData(BlockPos pos, Vector3f range) {
    public static final PacketCodec<RegistryByteBuf, MobAttractorData> PACKET_CODEC = PacketCodec.of(
            MobAttractorData::write, MobAttractorData::new
    );

    private MobAttractorData(RegistryByteBuf buf) {
        this(
            BlockPos.PACKET_CODEC.decode(buf),
            PacketCodecs.VECTOR3F.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        BlockPos.PACKET_CODEC.encode(buf, pos());
        PacketCodecs.VECTOR3F.encode(buf, range());
    }
}
