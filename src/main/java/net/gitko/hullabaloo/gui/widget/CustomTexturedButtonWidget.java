package net.gitko.hullabaloo.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

// Essentially just a TexturedButtonWidget with access to U and V
@Environment(EnvType.CLIENT)
public class CustomTexturedButtonWidget extends ButtonWidget {
    private final Identifier guiTexture;
    private int u;
    private int v;
    private final int hoveredVOffset;
    private final int guiTextureWidth;
    private final int guiTextureHeight;

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, Identifier guiTexture, ButtonWidget.PressAction pressAction) {
        this(x, y, width, height, u, v, height, guiTexture, 256, 256, pressAction);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, ButtonWidget.PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, guiTexture, 256, 256, pressAction);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, int textureWidth, int textureHeight, ButtonWidget.PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, ScreenTexts.EMPTY);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, ButtonWidget.PressAction pressAction, Text text) {
        this(x, y, width, height, u, v, hoveredVOffset, guiTexture, guiTextureWidth, guiTextureHeight, pressAction, EMPTY, text);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, ButtonWidget.PressAction pressAction, ButtonWidget.TooltipSupplier tooltipSupplier, Text text) {
        super(x, y, width, height, text, pressAction, tooltipSupplier);
        this.guiTextureWidth = guiTextureWidth;
        this.guiTextureHeight = guiTextureHeight;
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.guiTexture = guiTexture;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setUV(int u, int v) {
        this.u = u;
        this.v = v;
    }

    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.guiTexture);
        int i = this.v;
        if (!this.isNarratable()) {
            i += this.hoveredVOffset * 2;
        } else if (this.isHovered()) {
            i += this.hoveredVOffset;
        }

        RenderSystem.enableDepthTest();
        drawTexture(matrices, this.x, this.y, (float)this.u, (float)i, this.width, this.height, this.guiTextureWidth, this.guiTextureHeight);
        if (this.hovered) {
            this.renderTooltip(matrices, mouseX, mouseY);
        }
    }
}
