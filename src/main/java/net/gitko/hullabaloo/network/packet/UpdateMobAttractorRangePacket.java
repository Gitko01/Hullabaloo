package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.MobAttractorBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class UpdateMobAttractorRangePacket {
    private static final Identifier UPDATE_MOB_ATTRACTOR_RANGE_PACKET_ID = new Identifier(Hullabaloo.MOD_ID, "update_mob_attractor_range_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_MOB_ATTRACTOR_RANGE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            Vector3f range = buf.readVector3f();

            BlockPos pos = buf.readBlockPos();
            World world = player.getWorld();

            server.execute(() -> {
                if (world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16)) {
                    MobAttractorBlockEntity be = (MobAttractorBlockEntity) world.getBlockEntity(pos);
                    assert be != null;

                    be.setRange(range);
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }
}
