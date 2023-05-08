package net.gitko.hullabaloo.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup TAB = FabricItemGroup.builder(new Identifier(Hullabaloo.MOD_ID, "tab"))
            .icon(() -> new ItemStack(ModBlocks.MOB_ATTRACTOR.asItem()))
            .build();
}
