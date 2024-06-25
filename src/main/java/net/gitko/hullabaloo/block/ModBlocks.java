package net.gitko.hullabaloo.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.*;
import net.gitko.hullabaloo.item.ModItemGroup;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModBlocks {

    // Blocks
    public static Block VACUUM_HOPPER;
    public static Block BLOCK_ACTIVATOR;
    public static Block COBBLESTONE_GENERATOR;
    public static Block MOB_ATTRACTOR;

    // Block entities
    public static BlockEntityType<VacuumHopperBlockEntity> VACUUM_HOPPER_BLOCK_ENTITY;
    public static BlockEntityType<BlockActivatorBlockEntity> BLOCK_ACTIVATOR_BLOCK_ENTITY;
    public static BlockEntityType<CobblestoneGeneratorBlockEntity> COBBLESTONE_GENERATOR_BLOCK_ENTITY;
    public static BlockEntityType<MobAttractorBlockEntity> MOB_ATTRACTOR_BLOCK_ENTITY;

    // No tooltip
    public static Block registerBlock(FabricBlockSettings blockSettings, String name, RegistryKey<ItemGroup> itemGroup) {
        Block newBlock = new Block(blockSettings);
        Identifier blockId = new Identifier(Hullabaloo.MOD_ID, name);

        Registry.register(Registries.BLOCK, blockId, newBlock);
        registerBlockItem(newBlock, blockId, itemGroup);

        return newBlock;
    }

    // No tooltip
    public static void registerBlockItem(Block block, Identifier blockId, RegistryKey<ItemGroup> itemGroup) {
        BlockItem newBlockItem = new BlockItem(block, new FabricItemSettings());

        Registry.register(Registries.ITEM, blockId, newBlockItem);
        // add to item group
        ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> {
            content.add(newBlockItem);
        });
    }

    // With tooltip
    public static Block registerBlock(FabricBlockSettings blockSettings, String name, RegistryKey<ItemGroup> itemGroup, String tooltipKey, Integer tooltipLineCount, Boolean holdDownShift) {
        Block newBlock = new Block(blockSettings);
        Identifier blockId = new Identifier(Hullabaloo.MOD_ID, name);

        Registry.register(Registries.BLOCK, blockId, newBlock);
        registerBlockItem(newBlock, blockId, itemGroup, tooltipKey, tooltipLineCount, holdDownShift);

        return newBlock;
    }

    // With tooltip
    public static void registerBlockItem(Block block, Identifier blockId, RegistryKey<ItemGroup> itemGroup, String tooltipKey, Integer tooltipLineCount, Boolean holdDownShift) {
        BlockItem newBlockItem = new BlockItem(block, new FabricItemSettings()) {
            @Override
            public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                if (holdDownShift) {
                    if (Screen.hasShiftDown()) {
                        int currentLine = 1;

                        while (tooltipLineCount >= currentLine) {
                            tooltip.add(Text.translatable(tooltipKey + "_" + currentLine));
                            currentLine += 1;
                        }
                    } else {
                        tooltip.add(Text.translatable("tooltip." + Hullabaloo.MOD_ID + ".hold_shift"));
                    }
                } else {
                    int currentLine = 1;

                    while (tooltipLineCount >= currentLine) {
                        tooltip.add(Text.translatable(tooltipKey + "_" + currentLine));
                        currentLine += 1;
                    }
                }
            }
        };

        Registry.register(Registries.ITEM, blockId, newBlockItem);
        // add to item group
        ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> {
            content.add(newBlockItem);
        });
    }

    // No tooltip, advanced block
    public static Block registerBlock(Block newBlock, String name, RegistryKey<ItemGroup> itemGroup) {
        Identifier blockId = new Identifier(Hullabaloo.MOD_ID, name);

        Registry.register(Registries.BLOCK, blockId, newBlock);
        registerBlockItem(newBlock, blockId, itemGroup);

        return newBlock;
    }

    // With tooltip, advanced block
    public static Block registerBlock(Block newBlock, String name, RegistryKey<ItemGroup> itemGroup, String tooltipKey, Integer tooltipLineCount, Boolean holdDownShift) {
        Identifier blockId = new Identifier(Hullabaloo.MOD_ID, name);

        Registry.register(Registries.BLOCK, blockId, newBlock);
        registerBlockItem(newBlock, blockId, itemGroup, tooltipKey, tooltipLineCount, holdDownShift);

        return newBlock;
    }

    public static void initBlocks() {
        // Blocks
        VACUUM_HOPPER = registerBlock(
                new VacuumHopperBlock(FabricBlockSettings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(5f, 6f)
                        .requiresTool()),
                "vacuum_hopper", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".vacuum_hopper", 4, true
        );

        BLOCK_ACTIVATOR = registerBlock(
                new BlockActivatorBlock(FabricBlockSettings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(5f, 6f)
                        .requiresTool()),
                "block_activator", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".block_activator", 4, true
        );

        COBBLESTONE_GENERATOR = registerBlock(
                new CobblestoneGeneratorBlock(FabricBlockSettings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(5f, 6f)
                        .requiresTool()),
                "cobblestone_generator", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".cobblestone_generator", 3, true
        );

        MOB_ATTRACTOR = registerBlock(
                new MobAttractorBlock(FabricBlockSettings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(5f, 6f)
                        .requiresTool()
                ),
                "mob_attractor", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".mob_attractor", 4, true
        );

        // Block entities
        VACUUM_HOPPER_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Hullabaloo.MOD_ID, "vacuum_hopper_block_entity"),
                FabricBlockEntityTypeBuilder.create(VacuumHopperBlockEntity::new, VACUUM_HOPPER).build()
        );

        BLOCK_ACTIVATOR_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Hullabaloo.MOD_ID, "block_activator_block_entity"),
                FabricBlockEntityTypeBuilder.create(BlockActivatorBlockEntity::new, BLOCK_ACTIVATOR).build()
        );

        COBBLESTONE_GENERATOR_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Hullabaloo.MOD_ID, "cobblestone_generator_block_entity"),
                FabricBlockEntityTypeBuilder.create(CobblestoneGeneratorBlockEntity::new, COBBLESTONE_GENERATOR).build()
        );

        MOB_ATTRACTOR_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Hullabaloo.MOD_ID, "mob_attractor_block_entity"),
                FabricBlockEntityTypeBuilder.create(MobAttractorBlockEntity::new, MOB_ATTRACTOR).build()
        );
    }
}
