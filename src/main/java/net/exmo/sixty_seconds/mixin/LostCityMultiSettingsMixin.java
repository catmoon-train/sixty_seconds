package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.lost.regassets.data.MultiSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyReturnValue;

/**
 * 让「超出 areasize 的超尺寸 multibuilding」能够真正生成出来（而非被跳过/崩溃）。
 */
@Mixin(MultiSettings.class)
public class LostCityMultiSettingsMixin {

    /** 必须 >= 数据包里最大的 multibuilding 尺寸（当前为 16：aircraftcarrier / base_ground_ew|ns）。 */
    private static final int MIN_AREASIZE = 20;

    @ModifyReturnValue(method = "areasize", at = @At("RETURN"))
    private int sixtySecondsEnlargeAreasize(int original) {
        return Math.max(original, MIN_AREASIZE);
    }
}
