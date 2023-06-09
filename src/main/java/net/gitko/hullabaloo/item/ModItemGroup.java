package net.gitko.hullabaloo.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final RegistryKey<ItemGroup> TAB = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(Hullabaloo.MOD_ID, "item_group"));

    public static void registerItemGroup() {
        Registry.register(Registries.ITEM_GROUP, TAB, FabricItemGroup.builder()
            .displayName(Text.of("Hullabaloo"))
            .icon(() -> new ItemStack(ModBlocks.MOB_ATTRACTOR.asItem()))
            .build()); // build() no longer registers by itself
    }
}
