package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.MobAttractorBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

public class UpdateMobAttractorRangePacket {
    private static final Identifier UPDATE_MOB_ATTRACTOR_RANGE_PACKET_ID = new Identifier(Hullabaloo.MOD_ID, "update_mob_attractor_range_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_MOB_ATTRACTOR_RANGE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPosRange = buf.readBlockPos();
            Vec3f range = new Vec3f(blockPosRange.getX(), blockPosRange.getY(), blockPosRange.getZ());

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
