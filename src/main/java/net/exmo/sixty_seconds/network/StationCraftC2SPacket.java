package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.logic.SixtySecondsStations;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/** 客户端→服务端：在指定合成站合成配方 count 次（服务端校验站点/科技/供电/材料，能合几次合几次）。 */
public record StationCraftC2SPacket(String recipeId, BlockPos pos, int count) implements CustomPacketPayload {
    public static final Type<StationCraftC2SPacket> ID = new Type<>(SixtySeconds.id("station_craft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StationCraftC2SPacket> CODEC =
            StreamCodec.ofMember(StationCraftC2SPacket::encode, StationCraftC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(recipeId);
        buf.writeBlockPos(pos);
        buf.writeVarInt(count);
    }

    public static StationCraftC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new StationCraftC2SPacket(buf.readUtf(), buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(StationCraftC2SPacket payload, ServerPlayNetworking.Context context) {
        SixtySecondsStations.handleCraft(context.player(), payload.recipeId(), payload.pos(), payload.count());
    }
}
