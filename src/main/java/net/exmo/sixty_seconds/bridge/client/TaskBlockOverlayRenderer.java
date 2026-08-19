package net.exmo.sixty_seconds.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.sixty_seconds.bridge.fabric.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;

public final class TaskBlockOverlayRenderer {
    public static final ArrayList<BlockPos> RoomDoorPositions = new ArrayList<>();

    private TaskBlockOverlayRenderer() {}

    public static void render(WorldRenderContext context) {
        for (BlockPos pos : RoomDoorPositions) {
            renderBlockOverlay(context, pos, Color.CYAN, 0.45f, false, 1f);
        }
    }

    public static void renderBlockOverlay(WorldRenderContext context, BlockPos blockPos, Color color,
            float alpha, boolean colorize, float textScale) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        PoseStack matrices = context.matrixStack();
        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        AABB box = new AABB(blockPos);
        LevelRenderer.renderLineBox(matrices, context.consumers().getBuffer(RenderType.lines()), box,
                color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha);
        matrices.popPose();
    }
}
