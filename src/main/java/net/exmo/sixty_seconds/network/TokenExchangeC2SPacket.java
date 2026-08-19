package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.bridge.SREPlayerMinigameTaskComponent;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.exmo.sixty_seconds.SixtySeconds;

/**
 * 客户端→服务端：把 {@code amount} 个人游戏币兑换成实体币（E 背包「兑换实体币」按钮 →
 * {@code TokenExchangeScreen}）。服务端重校验余额并按 64/组发放 {@code sixty_seconds_coin}
 * （背包满则落地）；实体币右键使用即存回余额（{@code SixtySecondsCoinItem}）。
 */
public record TokenExchangeC2SPacket(int amount) implements CustomPacketPayload {

    public static final Type<TokenExchangeC2SPacket> ID = new Type<>(SixtySeconds.id("sixty_seconds_token_exchange"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TokenExchangeC2SPacket> CODEC =
            StreamCodec.ofMember(TokenExchangeC2SPacket::encode, TokenExchangeC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(amount);
    }

    public static TokenExchangeC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new TokenExchangeC2SPacket(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TokenExchangeC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!SixtySecondsMod.isActive(player.level())) {
            return;
        }
        SREPlayerMinigameTaskComponent tokens = SREPlayerMinigameTaskComponent.KEY.get(player);
        int amount = Math.min(payload.amount(), tokens.getTokens());
        if (amount <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.sixty_seconds.sixty_seconds.coin_exchange_none"), true);
            return;
        }
        tokens.addTokens(-amount);
        int left = amount;
        while (left > 0) {
            int give = Math.min(64, left);
            left -= give;
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_COIN, give));
        }
        player.displayClientMessage(Component.translatable(
                "message.sixty_seconds.sixty_seconds.coin_withdrawn", amount, tokens.getTokens()), true);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.9F);
    }
}
