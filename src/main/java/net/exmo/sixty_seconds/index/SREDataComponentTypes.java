package net.exmo.sixty_seconds.index;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.item.component.SREWritableBookContent;
import net.exmo.sixty_seconds.content.item.component.SREWrittenBookContent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 报纸专用的数据组件（更大的字数限制书组件）。
 * 注册时机：mod 构造时（registry 冻结前）调用 {@link #register()}。
 */
public final class SREDataComponentTypes {
    private SREDataComponentTypes() {
    }

    public static DataComponentType<SREWritableBookContent> WRITABLE_BOOK_CONTENT;
    public static DataComponentType<SREWrittenBookContent> WRITTEN_BOOK_CONTENT;

    public static void register() {
        WRITABLE_BOOK_CONTENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                SixtySeconds.id("writable_book_content"),
                DataComponentType.<SREWritableBookContent>builder()
                        .persistent(SREWritableBookContent.CODEC)
                        .networkSynchronized(SREWritableBookContent.STREAM_CODEC)
                        .cacheEncoding().build());
        WRITTEN_BOOK_CONTENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                SixtySeconds.id("written_book_content"),
                DataComponentType.<SREWrittenBookContent>builder()
                        .persistent(SREWrittenBookContent.CODEC)
                        .networkSynchronized(SREWrittenBookContent.STREAM_CODEC)
                        .cacheEncoding().build());
    }
}
