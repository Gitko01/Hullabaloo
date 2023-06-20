package net.gitko.hullabaloo.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, Identifier guiTexture, PressAction pressAction) {
        this(x, y, width, height, u, v, height, guiTexture, 256, 256, pressAction);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, guiTexture, 256, 256, pressAction);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, guiTexture, guiTextureWidth, guiTextureHeight, pressAction, ScreenTexts.EMPTY);
    }

    public CustomTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier guiTexture, int guiTextureWidth, int guiTextureHeight, PressAction pressAction, Text text) {
        super(x, y, width, height, text, pressAction, DEFAULT_NARRATION_SUPPLIER);
        this.guiTextureWidth = guiTextureWidth;
        this.guiTextureHeight = guiTextureHeight;
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.guiTexture = guiTexture;
    }

    public void setUV(int u, int v) {
        this.u = u;
        this.v = v;
    }

    public void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.drawTexture(ctx, this.guiTexture, this.getX(), this.getY(), this.u, this.v, this.hoveredVOffset, this.width, this.height, this.guiTextureWidth, this.guiTextureHeight);
    }
}
