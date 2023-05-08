package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.VacuumHopperBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UpdateVacuumHopperIOPacket {
    private static final Identifier UPDATE_VACUUM_HOPPER_IO_PACKET_ID = new Identifier(Hullabaloo.MOD_ID, "update_vacuum_hopper_io_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_VACUUM_HOPPER_IO_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int[] inputs = buf.readIntArray();
            int[] outputs = buf.readIntArray();

            BlockPos pos = buf.readBlockPos();
            World world = player.getWorld();

            server.execute(() -> {
                if (world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16)) {
                    VacuumHopperBlockEntity be = (VacuumHopperBlockEntity) world.getBlockEntity(pos);
                    assert be != null;

                    be.setInputs(inputs);
                    be.setOutputs(outputs);
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }
}
