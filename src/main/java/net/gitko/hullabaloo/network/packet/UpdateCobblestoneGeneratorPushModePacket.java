package net.gitko.hullabaloo.network.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.CobblestoneGeneratorBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UpdateCobblestoneGeneratorPushModePacket {
    private static final Identifier UPDATE_COBBLESTONE_GENERATOR_PUSH_MODE_PACKET_ID = new Identifier(Hullabaloo.MOD_ID, "update_cobblestone_generator_push_mode_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_COBBLESTONE_GENERATOR_PUSH_MODE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int modeID = buf.readInt();
            BlockPos pos = buf.readBlockPos();
            World world = player.getWorld();

            server.execute(() -> {
                if (world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16)) {
                    CobblestoneGeneratorBlockEntity be = (CobblestoneGeneratorBlockEntity) world.getBlockEntity(pos);
                    assert be != null;

                    be.setPushMode(modeID);
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }
}
