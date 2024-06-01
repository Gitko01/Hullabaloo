package net.gitko.hullabaloo.item.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.gui.VacuumFilterScreenHandler;
import net.gitko.hullabaloo.network.payload.VacuumFilterData;
import net.gitko.hullabaloo.util.ImplementedInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class VacuumFilterItem extends Item implements ImplementedInventory {
    public VacuumFilterItem(Item.Settings settings) {
        super(settings);
    }

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(24, ItemStack.EMPTY);

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        if (!world.isClient() && hand == Hand.MAIN_HAND) {
            // Open up config GUI
            ExtendedScreenHandlerFactory<VacuumFilterData> extendedScreenHandlerFactory = new ExtendedScreenHandlerFactory<>() {
                @Override
                public VacuumFilterData getScreenOpeningData(ServerPlayerEntity player) {
                    return new VacuumFilterData(
                        getModeFromNbt(playerEntity.getStackInHand(hand)),
                        getItemsToFilterFromNbt(playerEntity.getStackInHand(hand))
                    );
                }

                @Override
                public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                    return new VacuumFilterScreenHandler(syncId, inv, getInv());
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable(getTranslationKey());
                }
            };

            playerEntity.openHandledScreen(extendedScreenHandlerFactory);
        }

        return TypedActionResult.success(playerEntity.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (Screen.hasShiftDown()) {
            int lineCount = 4;

            Hashtable<Integer, ItemStack> itemsToFilter = getItemsToFilterFromNbt(stack);
            String[] items = new String[2];

            int count = 0;
            for (int key : itemsToFilter.keySet()) {
                if (count <= 1) {
                    items[count] = Text.translatable(itemsToFilter.get(key).getItem().getTranslationKey()).getString();
                    count++;
                } else {
                    break;
                }
            }

            if (items[0] == null) {
                items[0] = "empty";
            }
            if (items[1] == null) {
                items[1] = "empty";
            }

            for (int i = 1; i <= lineCount; i++) {
                if (i == lineCount) {
                    if (!stack.getComponents().isEmpty()) {
                        tooltip.add(Text.literal("§7§o" + items[0] + ", " + items[1] + "..."));
                    }
                } else {
                    tooltip.add(Text.translatable("tooltip." + Hullabaloo.MOD_ID + ".vacuum_filter" + "_" + i));
                }
            }
        } else {
            tooltip.add(Text.translatable("tooltip." + Hullabaloo.MOD_ID + ".hold_shift"));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }

    public static Hashtable<Integer, ItemStack> readItemsToFilterBuf(RegistryByteBuf buf) {
        int[] itemIndexes = buf.readIntArray();
        Hashtable<Integer, ItemStack> itemStacks = new Hashtable<>();

        for (int index : itemIndexes) {
            ItemStack is = ItemStack.PACKET_CODEC.decode(buf);
            itemStacks.put(index, is);
        }

        return itemStacks;
    }

    public static void createItemsToFilterBuf(Hashtable<Integer, ItemStack> itemsToFilter, RegistryByteBuf buf) {
        int[] indexes = new int[itemsToFilter.size()];
        List<ItemStack> itemStacks = new ArrayList<>();

        int count = 0;

        for (int i : itemsToFilter.keySet()) {
            indexes[count] = i;
            itemStacks.add(itemsToFilter.get(i));
            count = count + 1;
        }

        buf.writeIntArray(indexes);
        itemStacks.forEach((stack) -> ItemStack.PACKET_CODEC.encode(buf, stack));
    }

    public static Hashtable<Integer, ItemStack> getItemsToFilterFromNbt(ItemStack itemStackInHand) {
        NbtComponent nbtComp = itemStackInHand.get(DataComponentTypes.CUSTOM_DATA);
        Hashtable<Integer, ItemStack> itemsToFilter = new Hashtable<>();

        if (nbtComp != null) {
            NbtCompound nbt = nbtComp.copyNbt();
            try {
                int[] itemIndexes = nbt.getIntArray(Hullabaloo.MOD_ID + ".itemsToFilterIndexes");

                for (int i : itemIndexes) {
                    ItemStack itemStack = new ItemStack(Item.byRawId(nbt.getInt(Hullabaloo.MOD_ID + "." + i)));
                    itemsToFilter.put(i, itemStack);
                }

            } catch (Exception ignored) {
                // Set default value
                saveItemsToFilterToNbt(itemStackInHand, itemsToFilter);
            }
        }

        return itemsToFilter;
    }

    public static Hashtable<Integer, Item> getItemsToFilterFromNbtAsItems(ItemStack itemStackInHand) {
        NbtComponent nbtComp = itemStackInHand.get(DataComponentTypes.CUSTOM_DATA);
        Hashtable<Integer, Item> itemsToFilter = new Hashtable<>();

        if (nbtComp != null) {
            NbtCompound nbt = nbtComp.copyNbt();
            try {
                int[] itemIndexes = nbt.getIntArray(Hullabaloo.MOD_ID + ".itemsToFilterIndexes");

                for (int i : itemIndexes) {
                    ItemStack itemStack = new ItemStack(Item.byRawId(nbt.getInt(Hullabaloo.MOD_ID + "." + i)));
                    itemsToFilter.put(i, itemStack.getItem());
                }

            } catch (Exception ignored) {
                // Set default value
                saveItemsToFilterToNbt(itemStackInHand, new Hashtable<>());
            }
        }

        return itemsToFilter;
    }

    public static void saveItemsToFilterToNbt(ItemStack itemStackInHand, Hashtable<Integer, ItemStack> itemsToFilter) {
        NbtComponent newNbtComp = itemStackInHand.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound newNbt = new NbtCompound();
        if (newNbtComp != null) {
            newNbt = newNbtComp.copyNbt();
        }

        // Split items to filter up and save to nbt
        int[] indexes = new int[itemsToFilter.size()];

        int count = 0;

        for (int i : itemsToFilter.keySet()) {
            indexes[count] = i;

            newNbt.putInt(Hullabaloo.MOD_ID + "." + i, Item.getRawId(itemsToFilter.get(i).getItem()));
            //VacuumHopper.LOGGER.info("[Vacuum Hopper] Saving " + Item.getRawId(itemsToFilter.get(i).getItem()) + " in slot " + i);

            count = count + 1;
        }

        newNbt.putIntArray(Hullabaloo.MOD_ID + ".itemsToFilterIndexes", indexes);

        itemStackInHand.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(newNbt));
    }

    public Inventory getInv() {
        return this;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    public static void saveModeNbtData(ItemStack itemStackInHand, int mode) {
        NbtComponent newNbtComp = itemStackInHand.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound newNbt = new NbtCompound();
        if (newNbtComp != null) {
            newNbt = newNbtComp.copyNbt();
        }

        newNbt.putInt(Hullabaloo.MOD_ID + ".mode", mode);

        itemStackInHand.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(newNbt));
    }

    public static int getModeFromNbt(ItemStack itemStackInHand) {
        NbtComponent nbtComp = itemStackInHand.get(DataComponentTypes.CUSTOM_DATA);
        int mode;

        if (nbtComp != null) {
            NbtCompound nbt = nbtComp.copyNbt();
            try {
                mode = nbt.getInt(Hullabaloo.MOD_ID + ".mode");
            } catch (Exception e) {
                // Set default mode
                saveModeNbtData(itemStackInHand, 0);
                mode = 0;
            }
        } else {
            // Set default mode
            saveModeNbtData(itemStackInHand, 0);
            mode = 0;
        }

        return mode;
    }
}
