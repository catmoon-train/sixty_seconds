package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSubmarineEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 客户端→服务端：潜水艇上浮 / 下潜意图。
 *
 * <p><b>为什么需要它</b>：{@code SynchedEntityData} 由服务端权威持有，
 * 客户端直接 {@code entityData.set(...)} 修改<b>不会</b>同步到服务端。
 * 若服务端读不到玩家按键，潜水艇的空格（上浮）/ 左 Ctrl（下潜）就完全失效。
 * 因此按键必须经网络包送到服务端，再由服务端写入实体输入态并驱动移动。
 */
public record SubmarineControlC2SPacket(boolean ascend, boolean descend) implements CustomPacketPayload {

    public static final Type<SubmarineControlC2SPacket> ID =
            new Type<>(SixtySeconds.id("submarine_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmarineControlC2SPacket> CODEC =
            StreamCodec.ofMember(SubmarineControlC2SPacket::encode, SubmarineControlC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(ascend);
        buf.writeBoolean(descend);
    }

    public static SubmarineControlC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new SubmarineControlC2SPacket(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(SubmarineControlC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        // 仅当玩家确实在驾驶潜水艇时生效（防止越权操控别人的载具）
        if (player == null || !(player.getVehicle() instanceof SixtySecondsSubmarineEntity sub)) {
            return;
        }
        sub.setServerInput(payload.ascend(), payload.descend());
    }
}
