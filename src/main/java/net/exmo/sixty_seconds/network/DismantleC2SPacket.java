package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.logic.SixtySecondsDismantle;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/** 客户端→服务端：在拆解台拆解 1 件指定物品（服务端校验站点/距离/物品在包）。 */
public record DismantleC2SPacket(String itemId, BlockPos pos) implements CustomPacketPayload {
    public static final Type<DismantleC2SPacket> ID = new Type<>(SixtySeconds.id("dismantle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DismantleC2SPacket> CODEC =
            StreamCodec.ofMember(DismantleC2SPacket::encode, DismantleC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(itemId);
        buf.writeBlockPos(pos);
    }

    public static DismantleC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new DismantleC2SPacket(buf.readUtf(), buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(DismantleC2SPacket payload, ServerPlayNetworking.Context context) {
        SixtySecondsDismantle.handleDismantle(context.player(), payload.itemId(), payload.pos());
    }
}
