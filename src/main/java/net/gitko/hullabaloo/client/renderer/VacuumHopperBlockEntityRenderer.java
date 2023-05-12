package net.gitko.hullabaloo.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.gitko.hullabaloo.block.custom.VacuumHopperBlockEntity;
import net.gitko.hullabaloo.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class VacuumHopperBlockEntityRenderer implements BlockEntityRenderer<VacuumHopperBlockEntity> {
    // Item to use as a pointer
    private static ItemStack arrow = new ItemStack(Items.ARROW, 1);

    public VacuumHopperBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(VacuumHopperBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        float inputOffset = 0.475f;
        float outputOffset = 0.475f;
        float positiveCoordDistance = 1.5f;
        float negativeCoordDistance = 0.5f;
        float inputDistanceOffset = 0.25f;

        // Ensure player is holding a screwdriver
        if (inv.getStack(inv.selectedSlot).getItem() == ModItems.SCREWDRIVER) {
            // north, east, south, west, up, down
            // 1, 2, 3, 4, 5, 6
            int[] inputs = be.getInputs();
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
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 90, -45)));

                        renderItem = true;
                    }
                    case 2 -> {
                        // east
                        matrices.translate(positiveCoordDistance + offset, outputOffset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().east());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 0, -45)));

                        renderItem = true;
                    }
                    case 3 -> {
                        // south
                        matrices.translate(outputOffset, outputOffset, positiveCoordDistance + offset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().south());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(45, -90, 0)));

                        renderItem = true;
                    }
                    case 4 -> {
                        // west
                        matrices.translate(-negativeCoordDistance - offset, outputOffset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().west());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(180, 0, 135)));

                        renderItem = true;
                    }
                    case 5 -> {
                        // up
                        matrices.translate(outputOffset, positiveCoordDistance + offset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().up());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 180, 45)));

                        renderItem = true;
                    }
                    case 6 -> {
                        // down
                        matrices.translate(outputOffset, -negativeCoordDistance - offset, outputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().down());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 0, -135)));

                        renderItem = true;
                    }
                }

                if (renderItem) {MinecraftClient.getInstance().getItemRenderer().renderItem(arrow, ModelTransformation.Mode.GROUND, newLight, overlay, matrices, vertexConsumers, 0);}

                // Mandatory call after GL calls
                matrices.pop();
            }

            for (int input: inputs) {
                matrices.push();

                double offset = Math.sin((be.getWorld().getTime() + tickDelta) / 8.0) / 4.0;

                int newLight = 0;

                boolean renderItem = false;

                switch (input) {
                    case 1 -> {
                        // north
                        matrices.translate(inputOffset, inputOffset, -negativeCoordDistance - offset - inputDistanceOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().north());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 90, 135)));

                        renderItem = true;
                    }
                    case 2 -> {
                        // east
                        matrices.translate(positiveCoordDistance + offset + inputDistanceOffset, inputOffset, inputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().east());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 0, 135)));

                        renderItem = true;
                    }
                    case 3 -> {
                        // south
                        matrices.translate(inputOffset, inputOffset, positiveCoordDistance + offset + inputDistanceOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().south());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(-135, -90, 0)));

                        renderItem = true;
                    }
                    case 4 -> {
                        // west
                        matrices.translate(-negativeCoordDistance - offset - inputDistanceOffset, inputOffset, inputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().west());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(180, 0, -45)));

                        renderItem = true;
                    }
                    case 5 -> {
                        // up
                        matrices.translate(inputOffset, positiveCoordDistance + offset + inputDistanceOffset, inputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().up());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 180, -135)));

                        renderItem = true;
                    }
                    case 6 -> {
                        // down
                        matrices.translate(inputOffset, -negativeCoordDistance - offset - inputDistanceOffset, inputOffset);
                        newLight = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().down());
                        matrices.multiply(Quaternion.fromEulerXyzDegrees(new Vec3f(0, 0, 45)));

                        renderItem = true;
                    }
                }

                if (renderItem) {MinecraftClient.getInstance().getItemRenderer().renderItem(arrow, ModelTransformation.Mode.GROUND, newLight, overlay, matrices, vertexConsumers, 0);}

                // Mandatory call after GL calls
                matrices.pop();
            }
        }
    }
}
