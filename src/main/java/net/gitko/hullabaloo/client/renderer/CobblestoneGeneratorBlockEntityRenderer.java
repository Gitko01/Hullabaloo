package net.gitko.hullabaloo.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.gitko.hullabaloo.block.custom.CobblestoneGeneratorBlockEntity;
import net.gitko.hullabaloo.item.ModItems;
import net.gitko.hullabaloo.util.QuaternionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class CobblestoneGeneratorBlockEntityRenderer implements BlockEntityRenderer<CobblestoneGeneratorBlockEntity> {
    // Item to use as a pointer
    private static ItemStack arrow = new ItemStack(Items.ARROW, 1);

    public CobblestoneGeneratorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(CobblestoneGeneratorBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        float outputOffset = 0.475f;
        float positiveCoordDistance = 1.5f;
        float negativeCoordDistance = 0.5f;

        // Ensure player is holding a screwdriver
        if (inv.getStack(inv.selectedSlot).getItem() == ModItems.SCREWDRIVER) {
            // north, east, south, west, up, down
            // 1, 2, 3, 4, 5, 6
            int[] outputs = be.getOutputs();

            for (int output: outputs) {
                matrices.push();

                double offset = Math.sin((be.getWorld().getTime() + tickDelta) / 8.0) / 4.0;

                int newLight = 0;

                boolean renderItem = false;

                switch (output) {
                    case 1 -> {
                        // north
                        matrices.translate(outputOffset, outputOffset, -negativeCoordDistance - offset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().north());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(0, 90, -45)));

                        renderItem = true;
                    }
                    case 2 -> {
                        // east
                        matrices.translate(positiveCoordDistance + offset, outputOffset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().east());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(0, 0, -45)));

                        renderItem = true;
                    }
                    case 3 -> {
                        // south
                        matrices.translate(outputOffset, outputOffset, positiveCoordDistance + offset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().south());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(45, -90, 0)));

                        renderItem = true;
                    }
                    case 4 -> {
                        // west
                        matrices.translate(-negativeCoordDistance - offset, outputOffset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().west());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(180, 0, 135)));

                        renderItem = true;
                    }
                    case 5 -> {
                        // up
                        matrices.translate(outputOffset, positiveCoordDistance + offset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().up());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(0, 180, 45)));

                        renderItem = true;
                    }
                    case 6 -> {
                        // down
                        matrices.translate(outputOffset, -negativeCoordDistance - offset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().down());
                        matrices.multiply(QuaternionUtil.fromEulerXyzDegrees(new Vector3f(0, 0, -135)));

                        renderItem = true;
                    }
                }

                if (renderItem) {MinecraftClient.getInstance().getItemRenderer().renderItem(arrow, ModelTransformationMode.GROUND, newLight, overlay, matrices, vertexConsumers, be.getWorld(), 0);}

                // Mandatory call after GL calls
                matrices.pop();
            }
        }
    }
}
