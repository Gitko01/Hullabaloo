package net.gitko.hullabaloo.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.gui.widget.CustomTexturedButtonWidget;
import net.gitko.hullabaloo.item.custom.VacuumFilterItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;

@Environment(EnvType.CLIENT)
public class VacuumFilterScreen extends HandledScreen<VacuumFilterScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Hullabaloo.MOD_ID, "textures/gui/item/vacuum_filter_gui.png");

    VacuumFilterScreenHandler screenHandler;
    private Hashtable<Integer, ItemStack> itemsToFilter;
    private ItemStack heldStack = ItemStack.EMPTY;
    private boolean holdingStack = false;
    private int mode = -1;
    private CustomTexturedButtonWidget modeButton = null;

    public VacuumFilterScreen(VacuumFilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        itemsToFilter = getItemsToFilter(handler);
        mode = getMode(handler);

        screenHandler = handler;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);

        itemsToFilter = getItemsToFilter(screenHandler);
        if (itemsToFilter != null) {
            for (int m = 0; m < 3; ++m) {
                for (int l = 0; l < 8; ++l) {
                    int itemIndex = l + m * 8;
                    int x = (17 + l * 18) + (width - backgroundWidth) / 2;
                    int y = (17 + m * 18) + (height - backgroundHeight) / 2;

                    ItemStack itemStack = itemsToFilter.get(itemIndex);
                    if (itemStack != null) {
                        itemRenderer.renderInGui(itemStack, x, y);
                    }
                }
            }
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if (slot != null) {
            slotId = slot.id;

            if (!holdingStack && !slot.getStack().isEmpty() && slotId > 23) {
                holdingStack = true;
                heldStack = slot.getStack().copy();
            } else {
                // Holding stack, check to see if placed in inventory
                if (slotId > 23) {
                    // clicked in inventory
                    holdingStack = false;
                    heldStack = ItemStack.EMPTY;
                } else {
                    // clicked in vacuum filter
                    if (!heldStack.isEmpty()) {
                        this.itemsToFilter.remove(slotId);
                        this.itemsToFilter.put(slotId, heldStack);
                    } else {
                        this.itemsToFilter.remove(slotId);
                    }
                }
            }

            if (client != null) {
                updateItemsToFilter(this.itemsToFilter, client);
            }
        }

        super.onMouseClick(slot, slotId, button, actionType);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        if (modeButton == null && mode != -1) {
            int u = 176;
            int v = 1;

            if (mode == 1) {
                v = 9;
            }

            modeButton = this.addDrawableChild(createModeWidget(
                    x, y, 6, 6, 7, 7, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress,
                    (button, matrices, mouseX, mouseY) -> renderTooltip(matrices, Text.translatable("gui." + Hullabaloo.MOD_ID + ".vacuum_filter.mode." + mode), mouseX, mouseY),
                    Text.literal(""), client));
        }

        if (mode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Mode for a vacuum filter is -1! (Failed to get value from screen handler)");
        }
    }

    public CustomTexturedButtonWidget createModeWidget(int x, int y, int xMargin, int yMargin, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, ButtonWidget.PressAction pressAction, ButtonWidget.TooltipSupplier tooltip, Text text, MinecraftClient client) {
        CustomTexturedButtonWidget buttonWidget = new CustomTexturedButtonWidget(x + xMargin, y + yMargin, width, height, u, v, hoveredVOffset, guiTexture, guiTextureWidth, guiTextureHeight, pressAction, tooltip, text) {
            @Override
            public void onPress() {
                if (mode == 1) {
                    mode = 0;
                } else mode++;

                switch (mode) {
                    case 0 -> {
                        this.setUV(176, 1);
                    }
                    case 1 -> {
                        this.setUV(176, 9);
                    }
                }

                updateMode(mode, client);
            }
        };
        return buttonWidget;
    }

    @Nullable
    private static Hashtable<Integer, ItemStack> getItemsToFilter(ScreenHandler handler) {
        if (handler instanceof VacuumFilterScreenHandler) {
            return ((VacuumFilterScreenHandler) handler).getItemsToFilter();
        } else {
            return null;
        }
    }

    private void updateItemsToFilter(Hashtable<Integer, ItemStack> itemsToFilter, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();

            VacuumFilterItem.createItemsToFilterBuf(itemsToFilter, buf);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_filter_items_packet"), buf);
        });
    }

    private void updateMode(int newMode, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(newMode);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_filter_mode_packet"), buf);
        });
    }

    private static int getMode(ScreenHandler handler) {
        if (handler instanceof VacuumFilterScreenHandler) {
            return ((VacuumFilterScreenHandler) handler).getMode();
        } else {
            return -1;
        }
    }
}
