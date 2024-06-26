package net.gitko.hullabaloo.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModItems {
    // Items
    public static Item SCREWDRIVER;
    public static Item VACUUM_FILTER;
    public static Item IRON_COBBLESTONE_GENERATOR_UPGRADE;
    public static Item GOLD_COBBLESTONE_GENERATOR_UPGRADE;
    public static Item DIAMOND_COBBLESTONE_GENERATOR_UPGRADE;
    public static Item AMETHYST_COBBLESTONE_GENERATOR_UPGRADE;
    public static Item NETHERITE_COBBLESTONE_GENERATOR_UPGRADE;
    public static Item ULTIMATE_COBBLESTONE_GENERATOR_UPGRADE;


    // basic item with no tooltip
    public static Item registerItem(Item.Settings itemSettings, String name, RegistryKey<ItemGroup> itemGroup) {
        Item newItem = new Item(itemSettings);

        Registry.register(Registries.ITEM, new Identifier(Hullabaloo.MOD_ID, name), newItem);
        // add to item group
        ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> {
            content.add(newItem);
        });

        return newItem;
    }

    // basic item with tooltip
    public static Item registerItem(Item.Settings itemSettings, String name, RegistryKey<ItemGroup> itemGroup, String tooltipKey, Integer tooltipLineCount, Boolean holdDownShift) {
        Item newItem = new Item(itemSettings) {
            @Override
            public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
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

        Registry.register(Registries.ITEM, new Identifier(Hullabaloo.MOD_ID, name), newItem);
        // add to item group
        ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> {
            content.add(newItem);
        });

        return newItem;
    }

    // advanced item
    public static Item registerItem(Item item, String name, RegistryKey<ItemGroup> itemGroup) {
        Registry.register(Registries.ITEM, new Identifier(Hullabaloo.MOD_ID, name), item);
        // add to item group
        ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> {
            content.add(item);
        });

        return item;
    }

    public static void initItems() {
        SCREWDRIVER = registerItem(new Item.Settings().maxCount(1),
                "screwdriver", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".screwdriver", 2, true
        );

        VACUUM_FILTER = registerItem(new VacuumFilterItem(new Item.Settings().maxCount(1)), "vacuum_filter", ModItemGroup.TAB);

        IRON_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "iron_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".iron_cobblestone_generator_upgrade", 1, false
        );

        GOLD_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "gold_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".gold_cobblestone_generator_upgrade", 1, false
        );

        DIAMOND_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "diamond_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".diamond_cobblestone_generator_upgrade", 1, false
        );

        AMETHYST_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "amethyst_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".amethyst_cobblestone_generator_upgrade", 1, false
        );

        NETHERITE_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "netherite_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".netherite_cobblestone_generator_upgrade", 1, false
        );

        ULTIMATE_COBBLESTONE_GENERATOR_UPGRADE = registerItem(new Item.Settings().maxCount(1),
                "ultimate_cobblestone_generator_upgrade", ModItemGroup.TAB, "tooltip." + Hullabaloo.MOD_ID + ".ultimate_cobblestone_generator_upgrade", 1, false
        );
    }
}
