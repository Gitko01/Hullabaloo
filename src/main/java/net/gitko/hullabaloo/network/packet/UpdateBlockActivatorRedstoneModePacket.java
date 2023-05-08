package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.BlockActivatorBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UpdateBlockActivatorRedstoneModePacket {
    private static final Identifier UPDATE_BLOCK_ACTIVATOR_REDSTONE_MODE_PACKET_ID = new Identifier(Hullabaloo.MOD_ID, "update_block_activator_redstone_mode_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BLOCK_ACTIVATOR_REDSTONE_MODE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int modeID = buf.readInt();
            BlockPos pos = buf.readBlockPos();
            World world = player.getWorld();

            server.execute(() -> {
                if (world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16)) {
                    BlockActivatorBlockEntity be = (BlockActivatorBlockEntity) world.getBlockEntity(pos);
                    assert be != null;

                    be.setRedstoneMode(modeID);
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }

}
