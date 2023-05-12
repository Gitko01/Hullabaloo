package net.gitko.hullabaloo.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup TAB = FabricItemGroupBuilder.build(new Identifier(Hullabaloo.MOD_ID, "tab"),
            () -> new ItemStack(ModBlocks.MOB_ATTRACTOR.asItem())
    );
}
