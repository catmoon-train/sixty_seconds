package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsTraitsModel;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * 末日60秒自研怪物渲染器：复用原版僵尸模型/动画，贴图按实体变体（拖行者/奔跑者/重锤兽/吐酸者）
 * 或 Boss 专属贴图切换（{@link SixtySecondsMonsterEntity#textureLocation()}）。
 * Boss 体型放大由 {@code Attributes.SCALE} 驱动，渲染层无需处理。
 *
 * <p>额外叠加 {@link SixtySecondsTraitsLayer}：在僵尸人形之上绘制变体专属特征几何
 * （犄角 / 骨冠 / 兜帽 / 背甲 / 脊刺 / 肋骨 / 气囊 / 幽翼 / 臂刃 / 利爪 等）。
 */
public class SixtySecondsMonsterRenderer extends ZombieRenderer {

    private final SixtySecondsTraitsModel traits;

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.traits = new SixtySecondsTraitsModel(SixtySecondsTraitsModel.createLayer().bakeRoot());
        this.addLayer(new SixtySecondsTraitsLayer(this, this.traits));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        if (entity instanceof SixtySecondsMonsterEntity monster) {
            return monster.textureLocation();
        }
        return super.getTextureLocation(entity);
    }
}
