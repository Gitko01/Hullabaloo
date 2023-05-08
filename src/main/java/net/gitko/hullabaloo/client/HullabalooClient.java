package net.gitko.hullabaloo.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.client.renderer.CobblestoneGeneratorBlockEntityRenderer;
import net.gitko.hullabaloo.client.renderer.VacuumHopperBlockEntityRenderer;
import net.gitko.hullabaloo.gui.*;
import net.gitko.hullabaloo.network.packet.*;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class HullabalooClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Hullabaloo.VACUUM_HOPPER_SCREEN_HANDLER, VacuumHopperScreen::new);
        HandledScreens.register(Hullabaloo.VACUUM_FILTER_SCREEN_HANDLER, VacuumFilterScreen::new);
        HandledScreens.register(Hullabaloo.BLOCK_ACTIVATOR_SCREEN_HANDLER, BlockActivatorScreen::new);
        HandledScreens.register(Hullabaloo.COBBLESTONE_GENERATOR_SCREEN_HANDLER, CobblestoneGeneratorScreen::new);
        HandledScreens.register(Hullabaloo.MOB_ATTRACTOR_SCREEN_HANDLER, MobAttractorScreen::new);

        BlockEntityRendererRegistry.register(ModBlocks.VACUUM_HOPPER_BLOCK_ENTITY, VacuumHopperBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.COBBLESTONE_GENERATOR_BLOCK_ENTITY, CobblestoneGeneratorBlockEntityRenderer::new);

        UpdateVacuumHopperRedstoneModePacket.register();
        UpdateVacuumHopperPushModePacket.register();
        UpdateVacuumHopperReachPacket.register();
        UpdateVacuumHopperIOPacket.register();
        UpdateVacuumFilterItemsPacket.register();
        UpdateVacuumFilterModePacket.register();
        UpdateBlockActivatorClickModePacket.register();
        UpdateBlockActivatorRoundRobinPacket.register();
        UpdateCobblestoneGeneratorPushModePacket.register();
        UpdateCobblestoneGeneratorRedstoneModePacket.register();
        UpdateMobAttractorRangePacket.register();
        UpdateBlockActivatorSpeedPacket.register();
        UpdateBlockActivatorRedstoneModePacket.register();

        Hullabaloo.LOGGER.info("[Hullabaloo] (client) Finished preparing the hullabaloo!");
    }
}