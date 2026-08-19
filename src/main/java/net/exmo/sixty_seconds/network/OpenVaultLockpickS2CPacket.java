package net.exmo.sixty_seconds.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/** 服务端→客户端：打开保险库撬锁小游戏 */
public record OpenVaultLockpickS2CPacket(BlockPos vaultPos) implements CustomPacketPayload {
    public static final Type<OpenVaultLockpickS2CPacket> ID = new Type<>(SixtySeconds.id("open_vault_lockpick"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVaultLockpickS2CPacket> CODEC =
            StreamCodec.ofMember(OpenVaultLockpickS2CPacket::encode, OpenVaultLockpickS2CPacket::decode);

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(vaultPos);
    }

    private static OpenVaultLockpickS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenVaultLockpickS2CPacket(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
