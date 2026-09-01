package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.traits.SixtySecondsTraitComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 客户端 → 服务端：请求为当前玩家分配/点亮某个特质。
 * 服务端校验：仅在 60s 游戏进行中（已启动）才允许；且需满足点数规则。成功后自动同步组件到客户端。
 */
public record TraitAllocateC2SPacket(String traitId) implements CustomPacketPayload {

    public static final Type<TraitAllocateC2SPacket> ID =
            new Type<>(SixtySeconds.id("sixty_seconds_trait_allocate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TraitAllocateC2SPacket> CODEC =
            StreamCodec.ofMember(TraitAllocateC2SPacket::encode, TraitAllocateC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(traitId);
    }

    public static TraitAllocateC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new TraitAllocateC2SPacket(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TraitAllocateC2SPacket payload, ServerPlayNetworking.SimpleContext ctx) {
        ServerPlayer player = ctx.player();
        if (player == null) {
            return;
        }
        // 仅在 60s 游戏进行中允许加点；分配逻辑内部也会再校验一次。
        if (!net.exmo.sixty_seconds.SixtySecondsMod.isActive(player.level())) {
            return;
        }
        SixtySecondsTraitComponent.KEY.get(player).add(payload.traitId());
    }
}
