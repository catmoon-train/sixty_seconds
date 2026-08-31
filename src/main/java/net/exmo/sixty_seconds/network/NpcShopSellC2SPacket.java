package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.entity.SixtySecondsNpcEntity;
import net.exmo.sixty_seconds.logic.SixtySecondsNpcMenu;
import net.exmo.sixty_seconds.logic.SixtySecondsNpcShop;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/**
 * 客户端→服务端：在商人购买界面点了「卖」（回收）。
 * {@link SixtySecondsNpcShop#sell} 会重校验模式/相位/类型/存活/距离/下标/背包物品——
 * 隔空对 20 格外的商人发本包会被静默拒绝。
 */
public record NpcShopSellC2SPacket(int entityId, int rowIndex, int count) implements CustomPacketPayload {
    public static final Type<NpcShopSellC2SPacket> ID = new Type<>(SixtySeconds.id("npc_shop_sell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcShopSellC2SPacket> CODEC =
            StreamCodec.ofMember(NpcShopSellC2SPacket::encode, NpcShopSellC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(rowIndex);
        buf.writeVarInt(count);
    }

    public static NpcShopSellC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new NpcShopSellC2SPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(NpcShopSellC2SPacket payload, ServerPlayNetworking.Context context) {
        SixtySecondsNpcEntity npc = SixtySecondsNpcMenu.resolve(context.player(), payload.entityId());
        if (npc != null) {
            SixtySecondsNpcShop.sell(context.player(), npc, payload.rowIndex(), payload.count());
        }
    }
}
