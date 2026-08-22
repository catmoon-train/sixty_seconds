package net.exmo.sixty_seconds.index;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.item.component.SixtySecWritableBookContent;
import net.exmo.sixty_seconds.content.item.component.SixtySecWrittenBookContent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 报纸专用的数据组件（更大的字数限制书组件）。
 * 注册时机：挂在 DataComponentType 的 RegisterEvent 上（此时 registry 尚未冻结），
 * 不能在 @Mod 构造器里用 Registry.register 直接注册（那时已被冻结）。
 */
public final class SixtySecDataComponentTypes {
    private SixtySecDataComponentTypes() {
    }

    public static DataComponentType<SixtySecWritableBookContent> WRITABLE_BOOK_CONTENT;
    public static DataComponentType<SixtySecWrittenBookContent> WRITTEN_BOOK_CONTENT;

    public static void register(IEventBus bus) {
        bus.addListener(SixtySecDataComponentTypes::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            return;
        }
        WRITABLE_BOOK_CONTENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                SixtySeconds.id("writable_book_content"),
                DataComponentType.<SixtySecWritableBookContent>builder()
                        .persistent(SixtySecWritableBookContent.CODEC)
                        .networkSynchronized(SixtySecWritableBookContent.STREAM_CODEC)
                        .cacheEncoding().build());
        WRITTEN_BOOK_CONTENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                SixtySeconds.id("written_book_content"),
                DataComponentType.<SixtySecWrittenBookContent>builder()
                        .persistent(SixtySecWrittenBookContent.CODEC)
                        .networkSynchronized(SixtySecWrittenBookContent.STREAM_CODEC)
                        .cacheEncoding().build());
    }
}
