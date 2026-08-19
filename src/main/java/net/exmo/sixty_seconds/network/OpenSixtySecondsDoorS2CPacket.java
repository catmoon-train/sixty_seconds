package net.exmo.sixty_seconds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/** 服务端→客户端：打开事件/拜访门 GUI 壳（purpose = {@code DoorPurpose.ordinal()}）。 */
public record OpenSixtySecondsDoorS2CPacket(int purpose, BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenSixtySecondsDoorS2CPacket> ID =
            new Type<>(SixtySeconds.id("open_sixty_seconds_door"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSixtySecondsDoorS2CPacket> CODEC =
            StreamCodec.ofMember(OpenSixtySecondsDoorS2CPacket::encode, OpenSixtySecondsDoorS2CPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(purpose);
        buf.writeBlockPos(pos);
    }

    public static OpenSixtySecondsDoorS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenSixtySecondsDoorS2CPacket(buf.readVarInt(), buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
