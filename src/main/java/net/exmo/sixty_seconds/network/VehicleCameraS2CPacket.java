package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端→客户端：通知切换车辆摄像机（上车切第三人称，下车切回第一人称）。
 * <p>
 * 只保留数据与编解码；客户端接收逻辑在 {@code SixtySecondsClient} 注册
 * （切换 {@code CameraType} 必须发生在客户端类，不能在 payload 内嵌接收器——
 * payload 在专用服务端注册时也会被加载，引用客户端类会导致 dist 崩溃）。
 */
public record VehicleCameraS2CPacket(boolean thirdPerson) implements CustomPacketPayload {
    public static final Type<VehicleCameraS2CPacket> ID = new Type<>(
            SixtySeconds.id("vehicle_camera"));

    public static final StreamCodec<FriendlyByteBuf, VehicleCameraS2CPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, VehicleCameraS2CPacket::thirdPerson,
                    VehicleCameraS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
