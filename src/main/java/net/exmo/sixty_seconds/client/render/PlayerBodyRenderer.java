package net.exmo.sixty_seconds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 尸体渲染器：按死亡玩家 UUID 从客户端玩家列表取其原本皮肤（而非固定的史蒂夫），
 * 并在生成后的约 1.5 秒内播放由直立翻倒到平躺的死亡动画。
 */
public class PlayerBodyRenderer extends HumanoidMobRenderer<PlayerBodyEntity, PlayerCorpseModel> {

    public PlayerBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerCorpseModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerBodyEntity entity) {
        // 从客户端玩家列表按 UUID 取该玩家的皮肤（含自定义皮肤），取不到再回退史蒂夫
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(entity.getPlayerUuid());
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public void render(PlayerBodyEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 倒地动画：生成后约 30 tick（1.5 秒）内从直立翻倒到平躺
        float progress = Mth.clamp((entity.tickCount + partialTick) / 30F, 0F, 1F);
        float lieAngle = (float) (Math.PI / 2) * progress;
        poseStack.pushPose();
        // 以脚部为轴把身体放平，使其贴合地面
        poseStack.mulPose(Axis.XP.rotation(lieAngle));
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
