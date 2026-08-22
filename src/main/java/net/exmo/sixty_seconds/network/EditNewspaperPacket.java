package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.content.item.component.SREWritableBookContent;
import net.exmo.sixty_seconds.content.item.component.SREWrittenBookContent;
import net.exmo.sixty_seconds.index.SREDataComponentTypes;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.Filterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 客户端→服务端：报纸编辑保存（{@code title} 为空 = 存为草稿）。 */
public record EditNewspaperPacket(List<String> pages, Optional<String> title) implements CustomPacketPayload {
    public static final int MAX_BYTES_PER_CHAR = 4;
    public static final StreamCodec<FriendlyByteBuf, EditNewspaperPacket> STREAM_CODEC;
    public static final Type<EditNewspaperPacket> ID = new Type<>(
            SixtySeconds.id("newspaper/edit"));

    static {
        STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(8192).apply(ByteBufCodecs.list(200)), EditNewspaperPacket::pages,
                ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs::optional), EditNewspaperPacket::title,
                EditNewspaperPacket::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** 服务端处理：仅当主手持有报纸时保存编辑内容（title 有值 = 定稿，否则存草稿）。 */
    public static void handle(EditNewspaperPacket payload, ServerPlayNetworking.Context context) {
        var player = context.player();
        var mainHandItem = player.getMainHandItem();
        if (!mainHandItem.is(ModItems.NEWSPAPER)) {
            return;
        }
        var titOpt = payload.title();
        if (titOpt.isPresent()) {
            var list = new ArrayList<Filterable<Component>>();
            for (var p : payload.pages()) {
                list.add(Filterable.passThrough(Component.literal(p)));
            }
            String title = titOpt.get();
            if (title.length() >= SREWrittenBookContent.TITLE_MAX_LENGTH) {
                title = title.substring(0, SREWrittenBookContent.TITLE_MAX_LENGTH);
            }
            String shortTitle = title;
            if (shortTitle.length() >= 10) {
                shortTitle = shortTitle.substring(0, 8) + "...";
            }
            mainHandItem.set(SREDataComponentTypes.WRITTEN_BOOK_CONTENT,
                    new SREWrittenBookContent(Filterable.passThrough(title), player.getScoreboardName(), list, true));
            mainHandItem.set(DataComponents.ITEM_NAME,
                    Component.translatable("item.sixty_seconds.newspaper.name",
                            Component.translatable("item.sixty_seconds.newspaper.title.warp", shortTitle)
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));

            if (mainHandItem.has(SREDataComponentTypes.WRITABLE_BOOK_CONTENT)) {
                mainHandItem.remove(SREDataComponentTypes.WRITABLE_BOOK_CONTENT);
            }
        } else {
            var list = new ArrayList<Filterable<String>>();
            for (var p : payload.pages()) {
                list.add(Filterable.passThrough(p));
            }
            mainHandItem.set(DataComponents.ITEM_NAME,
                    Component.translatable("item.sixty_seconds.newspaper.draft",
                            Component.translatable("item.sixty_seconds.newspaper.draft.warp", player.getName())
                                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
            mainHandItem.set(SREDataComponentTypes.WRITABLE_BOOK_CONTENT, new SREWritableBookContent(list));
        }
    }
}
