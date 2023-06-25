package net.gitko.hullabaloo.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.gitko.hullabaloo.Hullabaloo;
import net.gitko.hullabaloo.block.custom.MobAttractorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class MobAttractorScreen extends HandledScreen<MobAttractorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Hullabaloo.MOD_ID, "textures/gui/non-container/mob_attractor_gui.png");

    MobAttractorScreenHandler screenHandler;
    private BlockPos blockPos;
    private Vector3f range;

    private SliderWidget rangeXScrollbar = null;
    private SliderWidget rangeYScrollbar = null;
    private SliderWidget rangeZScrollbar = null;

    public MobAttractorScreen(MobAttractorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.of(""));
        blockPos = getPos(handler).orElse(null);
        range = getRange(handler);
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

        // progress bar
        int maxUnitFill = MobAttractorBlockEntity.MAX_ENERGY_CAPACITY;
        int pBLength = 90;
        int pBHeight = 8;

        int xMargin = 77;
        int yMargin = 71;

        int energy = getEnergyAmount(screenHandler);
        int cooldown = getCooldown(screenHandler);
        int drainRate = getDrainAmount(screenHandler);

        int energyPercentage = Math.round(((float) energy / (float) maxUnitFill) * 100F);
        int singleUseEnergyPercentage = Math.round(((float) energy / (float) drainRate) * 100F);
        int cooldownPercentage = Math.round(((float) cooldown / MobAttractorBlockEntity.SPEED) * 100F);

        int amountToMoveUpBy1Pixel = Math.round((float) maxUnitFill / (float) pBLength);

        int fillLength = Math.round((float) energy / (float) amountToMoveUpBy1Pixel);

        if (fillLength >= pBLength) {
            fillLength = pBLength;
        }

        ctx.drawTexture(TEXTURE, x + xMargin, y + yMargin, 1, 167, fillLength, pBHeight);

        // render a tooltip containing energy amount
        if (mouseX >= x + xMargin && mouseX <= (x + xMargin) + pBLength) {
            if (mouseY >= y + yMargin && mouseY <= (y + yMargin) + pBHeight) {
                DefaultedList<Text> tooltip = DefaultedList.ofSize(0);

                if (Screen.hasShiftDown()) {
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", energy, maxUnitFill)));

                    if (energyPercentage <= 10) {
                        tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                    } else if (energyPercentage <= 75) {
                        tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                    } else {
                        tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                    }

                    tooltip.add(Text.of("<----------------->"));

                    tooltip.add(Text.of("§6Max Energy: 20,000,000 E§r"));
                    tooltip.add(Text.of("§6Max Input Rate: 20,000,000 E§r"));
                    tooltip.add(Text.of("§6Drain Amount: -" + drainRate + " E§r"));
                } else {
                    // §number §r
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", energy, maxUnitFill)));

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

        xMargin = 61;
        yMargin = 69;

        // render a tooltip containing more block specific data data
        if (mouseX >= x + xMargin && mouseX <= x + xMargin + 12) {
            if (mouseY >= y + yMargin && mouseY <= y + yMargin + 12) {
                DefaultedList<Text> tooltip = DefaultedList.ofSize(0);

                tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", energy, drainRate)));
                tooltip.add(Text.of("§6Amount per use: -" + drainRate + " E§r"));

                if (singleUseEnergyPercentage <= 10) {
                    tooltip.add(Text.of("§4" + singleUseEnergyPercentage + "% Charged (single-use)§r"));
                } else if (singleUseEnergyPercentage < 100) {
                    tooltip.add(Text.of("§e" + singleUseEnergyPercentage + "% Charged (single-use)§r"));
                } else if (singleUseEnergyPercentage == 100) {
                    tooltip.add(Text.of("§a" + singleUseEnergyPercentage + "% Charged (single-use)§r"));
                } else {
                    tooltip.add(Text.of("§a100% Charged (single-use)§r"));
                }

                if (cooldownPercentage <= 10) {
                    tooltip.add(Text.of("Cooldown: §4" + cooldownPercentage + "%§r"));
                } else if (cooldownPercentage < 100) {
                    tooltip.add(Text.of("Cooldown: §e" + cooldownPercentage + "%§r"));
                } else if (cooldownPercentage == 100) {
                    tooltip.add(Text.of("Cooldown: §a" + cooldownPercentage + "%§r"));
                } else {
                    tooltip.add(Text.of("Cooldown: §a100%§r"));
                }

                if (singleUseEnergyPercentage >= 100 && cooldownPercentage >= 100) {
                    tooltip.add(Text.of(""));
                    tooltip.add(Text.of("§a§lPower with redstone to activate."));
                    tooltip.add(Text.of("§4§lBe careful!"));
                }

                ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
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

        if (rangeXScrollbar == null) {
            rangeXScrollbar = this.addDrawableChild(createRangeWidget(
                    x, y, 5, 5, 166, 20, Text.translatable("gui." + Hullabaloo.MOD_ID + ".mob_attractor.rangeSlider.x"), range.x(), MobAttractorBlockEntity.MAX_RANGE, client, new Vector3f(1,0,0))
            );
        }

        if (rangeYScrollbar == null) {
            rangeYScrollbar = this.addDrawableChild(createRangeWidget(
                    x, y, 5, 26, 166, 20, Text.translatable("gui." + Hullabaloo.MOD_ID + ".mob_attractor.rangeSlider.y"), range.y(), MobAttractorBlockEntity.MAX_RANGE, client, new Vector3f(0,1,0))
            );
        }

        if (rangeZScrollbar == null) {
            rangeZScrollbar = this.addDrawableChild(createRangeWidget(
                    x, y, 5, 47, 166, 20, Text.translatable("gui." + Hullabaloo.MOD_ID + ".mob_attractor.rangeSlider.z"), range.z(), MobAttractorBlockEntity.MAX_RANGE, client, new Vector3f(0,0,1))
            );
        }

        if (range == null) {
            Hullabaloo.LOGGER.error("[Hullabaloo] Range for a mob attractor is null! (Failed to get value from screen handler)");
        }
    }

    public SliderWidget createRangeWidget(int x, int y, int xMargin, int yMargin, int width, int height, Text text, double defaultValue, double maxValue, MinecraftClient client, Vector3f axis) {
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

                updateRange(roundedValue, axis, client);
            }
        };
    }

    private void updateRange(float range, Vector3f axis, MinecraftClient client) {
        client.execute(() -> {
            PacketByteBuf buf = PacketByteBufs.create();

            if (axis.x == 1 && axis.y == 0 && axis.z == 0) {
                this.range = new Vector3f(range, this.range.y(), this.range.z());
                buf.writeVector3f(this.range);
            } else if (axis.x == 0 && axis.y == 1 && axis.z == 0) {
                this.range = new Vector3f(this.range.x(), range, this.range.z());
                buf.writeVector3f(this.range);
            } else if (axis.x == 0 && axis.y == 0 && axis.z == 1) {
                this.range = new Vector3f(this.range.x(), this.range.y(), range);
                buf.writeVector3f(this.range);
            } else {
                Hullabaloo.LOGGER.error("[Hullabaloo] Error in updating the range for a Mob Attractor! If you are a Fabric mod dev, please check out the update range method in MobAttractorScreen.");
            }

            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(Hullabaloo.MOD_ID, "update_mob_attractor_range_packet"), buf);
        });
    }

    private static Optional<BlockPos> getPos(ScreenHandler handler) {
        if (handler instanceof MobAttractorScreenHandler) {
            BlockPos pos = ((MobAttractorScreenHandler) handler).getPos();
            return pos != null ? Optional.of(pos) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static Vector3f getRange(ScreenHandler handler) {
        if (handler instanceof MobAttractorScreenHandler) {
            return ((MobAttractorScreenHandler) handler).getRange();
        } else {
            return null;
        }
    }

    private int getEnergyAmount(ScreenHandler handler) {
        if (handler instanceof MobAttractorScreenHandler) {
            return ((MobAttractorScreenHandler) handler).getEnergyAmount();
        } else {
            return -1;
        }
    }

    private int getDrainAmount(ScreenHandler handler) {
        if (handler instanceof MobAttractorScreenHandler) {
            return ((MobAttractorScreenHandler) handler).getDrainAmount();
        } else {
            return -1;
        }
    }

    private int getCooldown(ScreenHandler handler) {
        if (handler instanceof MobAttractorScreenHandler) {
            return ((MobAttractorScreenHandler) handler).getCooldown();
        } else {
            return -1;
        }
    }

}
