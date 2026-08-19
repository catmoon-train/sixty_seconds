package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

public interface WorldRenderContext {
    Camera camera();

    PoseStack matrixStack();

    MultiBufferSource consumers();

    record Simple(Camera camera, PoseStack matrixStack, MultiBufferSource consumers) implements WorldRenderContext {
    }
}
