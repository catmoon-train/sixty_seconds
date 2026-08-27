package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.logic.SixtySecondsHotlineSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 拨号C2S包
 */
public record PhoneDialC2SPacket(String number) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PhoneDialC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(SixtySeconds.id("phone_dial"));

    public static final StreamCodec<FriendlyByteBuf, PhoneDialC2SPacket> CODEC = StreamCodec.ofMember(
            PhoneDialC2SPacket::encode, PhoneDialC2SPacket::decode
    );

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(number);
    }

    private static PhoneDialC2SPacket decode(FriendlyByteBuf buf) {
        return new PhoneDialC2SPacket(buf.readUtf(6));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PhoneDialC2SPacket payload, net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (player == null) return;
        String ret = SixtySecondsHotlineSystem.handleDial(player, payload.number());
        switch (ret) {
            case "connected_express" -> SixtySecondsHotlineSystem.handleExpressGreeting(player);
            case "connected_shop" -> SixtySecondsHotlineSystem.handleShopGreeting(player);
            case "connected_rescue" -> SixtySecondsHotlineSystem.handleRescueGreeting(player);
            case "connected_intel" -> SixtySecondsHotlineSystem.handleIntelGreeting(player);
            case "connected_weather" -> SixtySecondsHotlineSystem.handleWeatherGreeting(player);
            case "connected_counsel" -> SixtySecondsHotlineSystem.handleCounselGreeting(player);
            case "connected_hire" -> SixtySecondsHotlineSystem.handleHireGreeting(player);
            case "connected_black_market" -> SixtySecondsHotlineSystem.handleBlackMarketGreeting(player);
            case "connected_recycle" -> SixtySecondsHotlineSystem.handleRecycleGreeting(player);
            case "connected_poverty_relief" -> SixtySecondsHotlineSystem.handlePovertyReliefGreeting(player);
            case "connected_report" -> SixtySecondsHotlineSystem.handleReportGreeting(player);
            case "daily_limit" -> player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.hotline.daily_limit"), false);
            case "already_dialed" -> player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.hotline.already_dialed"), false);
            case "invalid" -> player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.hotline.invalid_number"), false);
        }
    }
}
