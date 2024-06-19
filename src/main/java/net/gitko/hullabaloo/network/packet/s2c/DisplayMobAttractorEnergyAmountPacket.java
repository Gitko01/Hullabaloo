package net.gitko.hullabaloo.network.packet.s2c;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.gui.MobAttractorScreenHandler;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DisplayMobAttractorEnergyAmountPacket(long energyAmount) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, DisplayMobAttractorEnergyAmountPacket> CODEC = CustomPayload.codecOf(DisplayMobAttractorEnergyAmountPacket::write, DisplayMobAttractorEnergyAmountPacket::new);
    public static final Id<DisplayMobAttractorEnergyAmountPacket> ID = CustomPayload.id(String.valueOf(new Identifier(Hullabaloo.MOD_ID, "display_mob_attractor_energy_amount_packet")));

    private DisplayMobAttractorEnergyAmountPacket(RegistryByteBuf buf) {
        this(
            PacketCodecs.VAR_LONG.decode(buf)
        );
    }

    public void write(RegistryByteBuf buf) {
        PacketCodecs.VAR_LONG.encode(buf, energyAmount());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            if (context.player().currentScreenHandler instanceof MobAttractorScreenHandler screenHandler) {
                screenHandler.setSavedEnergyAmount(payload.energyAmount());
            }
        });
    }
}
