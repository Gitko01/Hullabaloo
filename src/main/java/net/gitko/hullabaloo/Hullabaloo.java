package net.gitko.hullabaloo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.gitko.hullabaloo.block.ModBlocks;
import net.gitko.hullabaloo.gui.*;
import net.gitko.hullabaloo.item.ModItemGroup;
import net.gitko.hullabaloo.item.ModItems;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.reborn.energy.api.EnergyStorage;

public class Hullabaloo implements ModInitializer {

    public static final String MOD_ID = "hullabaloo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Screens
    public static ScreenHandlerType<VacuumHopperScreenHandler> VACUUM_HOPPER_SCREEN_HANDLER;
    public static ScreenHandlerType<VacuumFilterScreenHandler> VACUUM_FILTER_SCREEN_HANDLER;
    public static ScreenHandlerType<BlockActivatorScreenHandler> BLOCK_ACTIVATOR_SCREEN_HANDLER;
    public static ScreenHandlerType<CobblestoneGeneratorScreenHandler> COBBLESTONE_GENERATOR_SCREEN_HANDLER;
    public static ScreenHandlerType<MobAttractorScreenHandler> MOB_ATTRACTOR_SCREEN_HANDLER;

    private void initScreens() {
        VACUUM_HOPPER_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(VacuumHopperScreenHandler::new);
        VACUUM_FILTER_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(VacuumFilterScreenHandler::new);
        BLOCK_ACTIVATOR_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(BlockActivatorScreenHandler::new);
        COBBLESTONE_GENERATOR_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(CobblestoneGeneratorScreenHandler::new);
        MOB_ATTRACTOR_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(MobAttractorScreenHandler::new);

        Registry.register(Registries.SCREEN_HANDLER, new Identifier("vacuum_hopper_screen_handler"), VACUUM_HOPPER_SCREEN_HANDLER);
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("vacuum_filter_screen_handler"), VACUUM_FILTER_SCREEN_HANDLER);
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("block_activator_screen_handler"), BLOCK_ACTIVATOR_SCREEN_HANDLER);
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("cobblestone_generator_screen_handler"), COBBLESTONE_GENERATOR_SCREEN_HANDLER);
        Registry.register(Registries.SCREEN_HANDLER, new Identifier("mob_attractor_screen_handler"), MOB_ATTRACTOR_SCREEN_HANDLER);
    }

    private void initEnergyStorages() {
        EnergyStorage.SIDED.registerForBlockEntity((blockActivatorBlockEntity, direction) -> blockActivatorBlockEntity.energyStorage, ModBlocks.BLOCK_ACTIVATOR_BLOCK_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((mobAttractorBlockEntity, direction) -> mobAttractorBlockEntity.energyStorage, ModBlocks.MOB_ATTRACTOR_BLOCK_ENTITY);
    }

    @Override
    public void onInitialize() {
        ModItemGroup.registerItemGroup();
        ModItems.initItems();
        ModBlocks.initBlocks();
        this.initScreens();
        this.initEnergyStorages();

        LOGGER.info("[Hullabaloo] Finished preparing the hullabaloo!");
    }
}