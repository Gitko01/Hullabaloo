package net.gitko.hullabaloo.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.BlockActivatorBlockEntity;
import net.gitko.hullabaloo.gui.widget.CustomTexturedButtonWidget;
import net.gitko.hullabaloo.network.packet.c2s.UpdateBlockActivatorClickModePacket;
import net.gitko.hullabaloo.network.packet.c2s.UpdateBlockActivatorRedstoneModePacket;
import net.gitko.hullabaloo.network.packet.c2s.UpdateBlockActivatorRoundRobinPacket;
import net.gitko.hullabaloo.network.packet.c2s.UpdateBlockActivatorSpeedPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

// Handles the rendering side of the GUI (like making sure there is a button image where the ScreenHandler says a button is)
@Environment(EnvType.CLIENT)
public class BlockActivatorScreen extends HandledScreen<BlockActivatorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Hullabaloo.MOD_ID, "textures/gui/container/block_activator_gui.png");

    BlockActivatorScreenHandler screenHandler;

    private CyclingButtonWidget<Modes> modeButton = null;
    private CyclingButtonWidget<RoundRobinModes> roundRobinButton = null;
    private SliderWidget speedScrollbar = null;
    private CustomTexturedButtonWidget redstoneModeButton = null;

    private BlockPos blockPos = null;
    private int mode = -1;
    private boolean roundRobin = false;
    private int speed = -1;
    private int redstoneMode = -1;

    public enum Modes {
        LEFT_CLICK(1, "leftClick"),
        RIGHT_CLICK(0, "rightClick");

        private static final Modes[] BY_NAME = (Modes[]) Arrays.stream(values()).sorted(Comparator.comparingInt(Modes::getId)).toArray(Modes[]::new);
        private final int id;
        private final String name;

        private Modes(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public Text getTranslatableName() {
            return Text.translatable("gui." + Hullabaloo.MOD_ID + ".block_activator." + this.name);
        }

        public static Modes byOrdinal(int ordinal) {
            return BY_NAME[ordinal % BY_NAME.length];
        }

        @Nullable
        public static BlockActivatorScreen.Modes byName(String name) {
            Modes[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Modes mode = var1[var3];
                if (mode.name.equals(name)) {
                    return mode;
                }
            }

            return null;
        }
    }

    public enum RoundRobinModes {
        ON(1, "on", true),
        OFF(0, "off", false);

        private static final RoundRobinModes[] BY_NAME = (RoundRobinModes[]) Arrays.stream(values()).sorted(Comparator.comparingInt(RoundRobinModes::getId)).toArray(RoundRobinModes[]::new);
        private final int id;
        private final String name;
        private final boolean on;

        private RoundRobinModes(int id, String name, boolean on) {
            this.id = id;
            this.name = name;
            this.on = on;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean isOn() {
            return this.on;
        }

        public Text getTranslatableName() {
            return Text.translatable("gui." + Hullabaloo.MOD_ID + ".block_activator." + this.name);
        }

        public static RoundRobinModes byOrdinal(int ordinal) {
            return BY_NAME[ordinal % BY_NAME.length];
        }

        @Nullable
        public static BlockActivatorScreen.RoundRobinModes byName(String name) {
            RoundRobinModes[] var1 = values();
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                RoundRobinModes mode = var1[var3];
                if (mode.name.equals(name)) {
                    return mode;
                }
            }

            return null;
        }

        @Nullable
        public static BlockActivatorScreen.RoundRobinModes byValue(boolean value) {
            RoundRobinModes[] var1 = values();
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                RoundRobinModes mode = var1[var3];
                if (mode.on == value) {
                    return mode;
                }
            }

            return null;
        }
    }

    public BlockActivatorScreen(BlockActivatorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        blockPos = getPos(handler).orElse(null);
        mode = getMode(handler);
        roundRobin = getRoundRobin(handler);
        speed = getSpeed(handler);
        redstoneMode = getRedstoneMode(handler);
        screenHandler = handler;

        this.backgroundWidth = 176;
        this.backgroundHeight = 184;
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        ctx.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // progress bar
        int maxUnitFill = BlockActivatorBlockEntity.maxEnergyCapacity;
        int pBLength = 104;
        int pBHeight = 8;

        int xMargin = 63;
        int yMargin = 89;

        int energy = getEnergyAmount(screenHandler);
        int drainRate = getDrainRate(screenHandler);
        int singleUseEnergy = getEnergyPerUse(screenHandler);

        int energyPercentage = Math.round(((float) energy / (float) maxUnitFill) * 100F);

        int amountToMoveUpBy1Pixel = Math.round((float) maxUnitFill / (float) pBLength);

        int fillLength = Math.round((float) energy / (float) amountToMoveUpBy1Pixel);

        if (fillLength >= pBLength) {
            fillLength = pBLength;
        }

        ctx.drawTexture(TEXTURE, x + xMargin, y + yMargin, 1, 185, fillLength, pBHeight);

        // render a tooltip containing energy amount
        if (mouseX >= x + xMargin && mouseX <= (x + xMargin) + pBLength) {
            if (mouseY >= y + yMargin && mouseY <= (y + yMargin) + pBHeight) {
                DefaultedList<Text> tooltip = DefaultedList.ofSize(0);
                
                if (Screen.hasShiftDown()) {
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", Hullabaloo.DF.format(energy), Hullabaloo.DF.format(maxUnitFill))));

                    if (energyPercentage <= 10) {
                        tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                    } else if (energyPercentage <= 75) {
                        tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                    } else {
                        tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                    }

                    tooltip.add(Text.of("<----------------->"));

                    tooltip.add(Text.of("§6Max Energy: 10,000 E§r"));
                    tooltip.add(Text.of("§6Max Input Rate: 5,000 E§r"));
                    tooltip.add(Text.of("§6Drain Rate: -" + Hullabaloo.DF.format(drainRate) + " E/t§r"));
                } else {
                    // §number §r
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", Hullabaloo.DF.format(energy), Hullabaloo.DF.format(maxUnitFill))));

                    if (energyPercentage <= 10) {
                        tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                    } else if (energyPercentage <= 75) {
                        tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                    } else {
                        tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                    }

                    tooltip.add(Text.of(""));
                    tooltip.add(Text.translatable("tooltip." + Hullabaloo.MOD_ID + ".hold_shift"));
                }
                ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }

        xMargin = 5;
        yMargin = 60;

        // render a tooltip containing energy amount
        if (mouseX >= x + xMargin && mouseX <= (x + xMargin) + 108) {
            if (mouseY >= y + yMargin && mouseY <= (y + yMargin) + 21) {
                DefaultedList<Text> tooltip = DefaultedList.ofSize(0);

                tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", Hullabaloo.DF.format(energy), Hullabaloo.DF.format(maxUnitFill))));

                if (energyPercentage <= 10) {
                    tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                } else if (energyPercentage <= 75) {
                    tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                } else {
                    tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                }

                tooltip.add(Text.of(""));

                tooltip.add(Text.of("§6Cooldown: " + speed + " ticks§r"));
                tooltip.add(Text.of("§6Drain Rate: -" + Hullabaloo.DF.format(drainRate) + " E/t§r"));
                tooltip.add(Text.of("§6Energy to Run Once: -" + Hullabaloo.DF.format(singleUseEnergy) + " E§r"));

                ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }
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

        // Add the switch mode button and the round-robin button
        // center screen: this.width / 2 - 66 (-66 bc width is 64 and 2 offset)
        // top left gui: (width - backgroundWidth) / 2 as X, (height - backgroundHeight) / 2 as Y

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        this.x = x;
        this.y = y;

        if (modeButton == null && mode != -1) {
            modeButton = this.addDrawableChild(createModeButtonWidget(
                    0, x, y, 7, 19, 106, 20, "gui." + Hullabaloo.MOD_ID + ".block_activator.switchMode", client)
            );
        }

        if (roundRobinButton == null) {
            roundRobinButton = this.addDrawableChild(createRoundRobinButtonWidget(
                    0, x, y, 7, 41, 106, 20, "gui." + Hullabaloo.MOD_ID + ".block_activator.switchRoundRobin", client)
            );
        }

        if (speedScrollbar == null && speed != -1) {
            speedScrollbar = this.addDrawableChild(createSpeedWidget(
                    x, y, 7, 63, 106, 21, Text.translatable("gui." + Hullabaloo.MOD_ID + ".block_activator.speed"), speed, BlockActivatorBlockEntity.MAX_TICK_INTERVAL, client)
            );
        }

        // Create redstone mode button
        if (redstoneModeButton == null && redstoneMode != -1) {
            int u = 176;
            int v = 1;

            switch (redstoneMode) {
                case 1 -> {
                    v = 10;
                }
                case 2 -> {
                    v = 19;
                }
            }

            redstoneModeButton = this.addDrawableChild(createRedstoneModeWidget(
                    x, y, 8, 8, 8, 8, u, v, 0, TEXTURE, 256, 256, ButtonWidget::onPress, Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)), Text.literal(""), client));
        }

        if (mode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Mode for a block activator is -1! (Failed to get value from screen handler)");
        }

        if (speed == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Speed for a block activator is -1! (Failed to get value from screen handler)");
        }

        if (redstoneMode == -1) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Redstone mode for a block activator is -1! (Failed to get value from screen handler)");
        }
    }

    public CyclingButtonWidget<Modes> createModeButtonWidget(int buttonIndex, int x, int y, int xMargin, int yMargin, int buttonWidth, int buttonHeight, String translationKey, MinecraftClient client) {
        return CyclingButtonWidget.builder(Modes::getTranslatableName).values(Modes.values()).initially(Modes.byOrdinal(mode)).build(x + xMargin, y + yMargin, buttonWidth, buttonHeight, Text.translatable(translationKey), (button, mode) -> {
            switchMode(mode, client);
        });
    }

    public CyclingButtonWidget<RoundRobinModes> createRoundRobinButtonWidget(int buttonIndex, int x, int y, int xMargin, int yMargin, int buttonWidth, int buttonHeight, String translationKey, MinecraftClient client) {
        return CyclingButtonWidget.builder(RoundRobinModes::getTranslatableName).values(RoundRobinModes.values()).initially(RoundRobinModes.byValue(roundRobin)).build(x + xMargin, y + yMargin, buttonWidth, buttonHeight, Text.translatable(translationKey), (button, mode) -> {
            switchRoundRobin(mode, client);
        });
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
                        this.setUV(176, 10);
                    }
                    case 2 -> {
                        this.setUV(176, 19);
                    }
                }

                this.setTooltip(Tooltip.of(Text.translatable("gui." + Hullabaloo.MOD_ID + ".redstoneMode." + redstoneMode)));
                updateRedstoneMode(redstoneMode, client);
            }
        };
        buttonWidget.setTooltip(tooltip);
        return buttonWidget;
    }

    public SliderWidget createSpeedWidget(int x, int y, int xMargin, int yMargin, int width, int height, Text text, double defaultValue, double maxValue, MinecraftClient client) {
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

                updateSpeed(roundedValue, client);
            }
        };
    }

    private void updateSpeed(int speed, MinecraftClient client) {
        client.execute(() -> {
            this.speed = speed;
            ClientPlayNetworking.send(new UpdateBlockActivatorSpeedPacket(speed, this.blockPos));
        });
    }

    private void switchMode(Modes mode, MinecraftClient client) {
        client.execute(() -> {
            ClientPlayNetworking.send(new UpdateBlockActivatorClickModePacket(mode.getId(), this.blockPos));
        });
    }

    private void switchRoundRobin(RoundRobinModes mode, MinecraftClient client) {
        client.execute(() -> {
            ClientPlayNetworking.send(new UpdateBlockActivatorRoundRobinPacket(mode.isOn(), this.blockPos));
        });
    }

    private void updateRedstoneMode(int newRedstoneMode, MinecraftClient client) {
        client.execute(() -> {
            ClientPlayNetworking.send(new UpdateBlockActivatorRedstoneModePacket(newRedstoneMode, this.blockPos));
        });
    }

    private static Optional<BlockPos> getPos(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            BlockPos pos = ((BlockActivatorScreenHandler) handler).getPos();
            return pos != null ? Optional.of(pos) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static int getMode(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getMode();
        } else {
            return -1;
        }
    }

    private static boolean getRoundRobin(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getRoundRobin();
        } else {
            return false;
        }
    }

    private int getEnergyAmount(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getEnergyAmount();
        } else {
            return -1;
        }
    }

    private int getEnergyPerUse(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getEnergyPerUse();
        } else {
            return -1;
        }
    }

    private int getDrainRate(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getDrainRate();
        } else {
            return -1;
        }
    }

    private static int getSpeed(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getSpeed();
        } else {
            return -1;
        }
    }

    private static int getRedstoneMode(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getRedstoneMode();
        } else {
            return -1;
        }
    }
}