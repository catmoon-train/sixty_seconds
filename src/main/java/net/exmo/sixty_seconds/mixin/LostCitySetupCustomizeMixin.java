package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.config.ProfileSetup;
import mcjty.lostcities.gui.LostCitySetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 修复 LostCities 1.21-8.4.1 配置界面的原版 NPE
 */
@Mixin(value = LostCitySetup.class, remap = false)
public abstract class LostCitySetupCustomizeMixin {

    @Shadow
    private List<String> profiles;

    @Shadow
    private String profile;

    @Inject(method = "customize", at = @At("HEAD"))
    private void ss$ensureProfilesLoaded(CallbackInfo ci) {
        if (profiles != null) {
            return;
        }
        // 与 toggleProfile 的懒初始化逻辑保持一致：公开 profile + "default" 置顶
        List<String> list = ProfileSetup.STANDARD_PROFILES.entrySet().stream()
                .filter(entry -> entry.getValue().isPublic())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
        if (profile != null && !list.contains(profile)) {
            list.add(profile); // 恢复的 "customized" 或隐藏 profile 也要在列，否则后续 indexOf 会越界回 null
        }
        list.sort((o1, o2) -> {
            if ("default".equals(o1)) {
                return -1;
            }
            if ("default".equals(o2)) {
                return 1;
            }
            return o1.compareTo(o2);
        });
        profiles = list;
    }
}
