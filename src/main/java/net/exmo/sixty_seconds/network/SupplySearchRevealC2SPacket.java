package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.content.block_entity.SupplyBoxBlockEntity;
import net.exmo.sixty_seconds.menu.SupplySearchMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 客户端→服务端：物资箱容器搜刮形式下，玩家左键某个放大镜、读条完成后请求揭示该格战利品。
 * 仅携带槽位索引；服务端从玩家当前打开的 {@link SupplySearchMenu} 取回对应的物资箱方块实体，无需客户端传递坐标。
 */
public record SupplySearchRevealC2SPacket(int slot) implements CustomPacketPayload {
    public static final Type<SupplySearchRevealC2SPacket> ID = new Type<>(SixtySeconds.id("supply_search_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SupplySearchRevealC2SPacket> CODEC =
            StreamCodec.ofMember(SupplySearchRevealC2SPacket::encode, SupplySearchRevealC2SPacket::decode);

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slot);
    }

    private static SupplySearchRevealC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new SupplySearchRevealC2SPacket(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }

    public static void handle(SupplySearchRevealC2SPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player == null || !SixtySecondsMod.isActive(player.level())) return;
            if (!(player.containerMenu instanceof SupplySearchMenu menu)) return;
            SupplyBoxBlockEntity be = menu.getBlockEntity();
            if (be == null) return;
            be.revealSlot(packet.slot(), player);
        });
    }
}
