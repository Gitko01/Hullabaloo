package net.gitko.hullabaloo.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.gui.widget.CustomTexturedButtonWidget;
import net.gitko.hullabaloo.network.packet.c2s.UpdateCobblestoneGeneratorPushModePacket;
import net.gitko.hullabaloo.network.packet.c2s.UpdateCobblestoneGeneratorRedstoneModePacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CobblestoneGeneratorScreen extends HandledScreen<CobblestoneGeneratorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Hullabaloo.MOD_ID, "textures/gui/container/cobblestone_generator_gui.png");

    CobblestoneGeneratorScreenHandler screenHandler;
    private CustomTexturedButtonWidget redstoneModeButton = null;
    private CustomTexturedButtonWidget pushModeButton = null;
    private BlockPos blockPos = null;
    private int redstoneMode = -1;
    private int pushMode = -1;

    public CobblestoneGeneratorScreen(CobblestoneGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        blockPos = getPos(handler).orElse(null);
        redstoneMode = getRedstoneMode(handler);
        pushMode = getPushMode(handler);
        screenHandler = handler;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        ctx.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
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
                    x, y, 8, 17, 16, 16, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress, Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)), Text.literal(""), client));
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
                    x, y, 8, 36, 32, 16, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress, Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".pushMode." + pushMode)), Text.literal(""), client));
        }

        if (redstoneMode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Redstone mode for a cobblestone generator is -1! (Failed to get value from screen handler)");
        }

        if (pushMode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Push mode for a cobblestone generator is -1! (Failed to get value from screen handler)");
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

                this.setTooltip(Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)));
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

                this.setTooltip(Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".pushMode." + pushMode)));
                updatePushMode(pushMode, client);
            }
        };
        buttonWidget.setTooltip(tooltip);
        return buttonWidget;
    }

    private void updateRedstoneMode(int newRedstoneMode, MinecraftClient client) {
        client.execute(() -> {
            ClientPlayNetworking.send(new UpdateCobblestoneGeneratorRedstoneModePacket(newRedstoneMode, this.blockPos));
        });
    }

    private void updatePushMode(int newPushMode, MinecraftClient client) {
        client.execute(() -> {
            ClientPlayNetworking.send(new UpdateCobblestoneGeneratorPushModePacket(newPushMode, this.blockPos));
        });
    }

    private static Optional<BlockPos> getPos(ScreenHandler handler) {
        if (handler instanceof CobblestoneGeneratorScreenHandler) {
            BlockPos pos = ((CobblestoneGeneratorScreenHandler) handler).getPos();
            return pos != null ? Optional.of(pos) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static int getRedstoneMode(ScreenHandler handler) {
        if (handler instanceof CobblestoneGeneratorScreenHandler) {
            return ((CobblestoneGeneratorScreenHandler) handler).getRedstoneMode();
        } else {
            return -1;
        }
    }

    private static int getPushMode(ScreenHandler handler) {
        if (handler instanceof CobblestoneGeneratorScreenHandler) {
            return ((CobblestoneGeneratorScreenHandler) handler).getPushMode();
        } else {
            return -1;
        }
    }
}
