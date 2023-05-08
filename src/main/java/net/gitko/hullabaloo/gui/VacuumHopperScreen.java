package net.gitko.hullabaloo.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.VacuumHopperBlockEntity;
import net.gitko.hullabaloo.gui.widget.CustomTexturedButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class VacuumHopperScreen extends HandledScreen<VacuumHopperScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Hullabaloo.MOD_ID, "textures/gui/container/vacuum_hopper_gui.png");

    VacuumHopperScreenHandler screenHandler;
    private CustomTexturedButtonWidget redstoneModeButton = null;
    private CustomTexturedButtonWidget pushModeButton = null;
    private SliderWidget rangeScroller = null;
    private BlockPos blockPos = null;
    private int redstoneMode = -1;
    private int pushMode = -1;
    private int range = -1;

    public VacuumHopperScreen(VacuumHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        blockPos = getPos(handler).orElse(null);
        redstoneMode = getRedstoneMode(handler);
        pushMode = getPushMode(handler);
        range = getRange(handler);
        screenHandler = handler;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
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

        if (redstoneModeButton != null) {
            redstoneModeButton.setTooltip(Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)));
        }
        if (pushModeButton != null) {
            pushModeButton.setTooltip(Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".pushMode." + pushMode)));
        }
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // Create redstone mode button
        if (redstoneModeButton == null && redstoneMode != -1) {
            int u = 176;
            int v = 1;

            switch (redstoneMode) {
                case 1 -> {
                    v = 18;
                }
                case 2 -> {
                    v = 35;
                }
            }

            redstoneModeButton = this.addDrawableChild(createRedstoneModeWidget(
                    x, y, 8, 20, 16, 16, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress, Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)), Text.literal(""), client));
        }

        // Create push mode button
        if (pushModeButton == null && pushMode != -1) {
            int u = 176;
            int v = 1;

            switch (pushMode) {
                case 0 -> {
                    v = 69;
                }
                case 1 -> {
                    v = 52;
                }
            }

            pushModeButton = this.addDrawableChild(createPushModeWidget(
                    x, y, 8, 39, 32, 16, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress, Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".pushMode." + pushMode)), Text.literal(""), client));
        }

        // Create range slider
        if (rangeScroller == null) {
            rangeScroller = this.addDrawableChild(createVacuumRangeWidget(
                    x, y, 61, 58, 90, 20, Text.translatable("gui." + Hullabaloo.MOD_ID + ".vacuum_hopper.rangeSlider"), range, VacuumHopperBlockEntity.MAX_REACH, client)
            );
        }

        if (redstoneMode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Redstone mode for a vacuum hopper is -1! (Failed to get value from screen handler)");
        }

        if (pushMode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Push mode for a vacuum hopper is -1! (Failed to get value from screen handler)");
        }

        if (range == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Range for a vacuum hopper is -1! (Failed to get value from screen handler)");
        }
    }

    public CustomTexturedButtonWidget createRedstoneModeWidget(int x, int y, int xMargin, int yMargin, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, ButtonWidget.PressAction pressAction, Tooltip tooltip, Text text, MinecraftClient client) {
        CustomTexturedButtonWidget buttonWidget = new CustomTexturedButtonWidget(x + xMargin, y + yMargin, width, height, u, v, hoveredVOffset, guiTexture, guiTextureWidth, guiTextureHeight, pressAction, text) {
            @Override
            public void onPress() {
                if (redstoneMode == 2) {
                    redstoneMode = 0;
                } else { redstoneMode++; }

                switch (redstoneMode) {
                    case 0 -> {
                        this.setUV(176, 1);
                    }
                    case 1 -> {
                        this.setUV(176, 18);
                    }
                    case 2 -> {
                        this.setUV(176, 35);
                    }
                }

                updateRedstoneMode(redstoneMode, client);
            }
        };
        buttonWidget.setTooltip(tooltip);
        return buttonWidget;
    }

    public CustomTexturedButtonWidget createPushModeWidget(int x, int y, int xMargin, int yMargin, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, ButtonWidget.PressAction pressAction, Tooltip tooltip, Text text, MinecraftClient client) {
        CustomTexturedButtonWidget buttonWidget = new CustomTexturedButtonWidget(x + xMargin, y + yMargin, width, height, u, v, hoveredVOffset, guiTexture, guiTextureWidth, guiTextureHeight, pressAction, text) {
            @Override
            public void onPress() {
                if (pushMode == 1) {
                    pushMode = 0;
                } else { pushMode++; }

                switch (pushMode) {
                    case 0 -> {
                        this.setUV(176, 69);
                    }
                    case 1 -> {
                        this.setUV(176, 52);
                    }
                }

                updatePushMode(pushMode, client);
            }
        };
        buttonWidget.setTooltip(tooltip);
        return buttonWidget;
    }

    public SliderWidget createVacuumRangeWidget( int x, int y, int xMargin, int yMargin, int width, int height, Text text, double defaultValue, double maxValue, MinecraftClient client) {
        return new SliderWidget(x + xMargin, y + yMargin, width, height, Text.literal(text.getString() + defaultValue), defaultValue / maxValue) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal(text.getString() + this.value * maxValue));
            }

            @Override
            protected void applyValue() {
                double extrapolatedValue = this.value * maxValue;
                int roundedValue = (int) Math.round(extrapolatedValue);
                value = roundedValue / maxValue;
                updateRange(roundedValue, client);
            }
        };
    }

    private void updateRange(int newRange, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(newRange);
            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_hopper_reach_packet"), buf);
        });
    }

    private void updateRedstoneMode(int newRedstoneMode, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(newRedstoneMode);
            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_hopper_redstone_mode_packet"), buf);
        });
    }

    private void updatePushMode(int newPushMode, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(newPushMode);
            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_vacuum_hopper_push_mode_packet"), buf);
        });
    }

    private static Optional<BlockPos> getPos(ScreenHandler handler) {
        if (handler instanceof VacuumHopperScreenHandler) {
            BlockPos pos = ((VacuumHopperScreenHandler) handler).getPos();
            return pos != null ? Optional.of(pos) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static int getRedstoneMode(ScreenHandler handler) {
        if (handler instanceof VacuumHopperScreenHandler) {
            return ((VacuumHopperScreenHandler) handler).getRedstoneMode();
        } else {
            return -1;
        }
    }

    private static int getPushMode(ScreenHandler handler) {
        if (handler instanceof VacuumHopperScreenHandler) {
            return ((VacuumHopperScreenHandler) handler).getPushMode();
        } else {
            return -1;
        }
    }

    private static int getRange(ScreenHandler handler) {
        if (handler instanceof VacuumHopperScreenHandler) {
            return ((VacuumHopperScreenHandler) handler).getRange();
        } else {
            return -1;
        }
    }
}
