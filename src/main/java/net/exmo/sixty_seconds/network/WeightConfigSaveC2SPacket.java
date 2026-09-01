package net.exmo.sixty_seconds.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端：保存负重配置。服务端处理（不引用任何客户端类）。
 */
public record WeightConfigSaveC2SPacket(SixtySecondsWeightConfig config) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().create();

    public static final Type<WeightConfigSaveC2SPacket> ID =
            new Type<>(SixtySeconds.id("weight_config_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightConfigSaveC2SPacket> CODEC =
            StreamCodec.ofMember(WeightConfigSaveC2SPacket::encode, WeightConfigSaveC2SPacket::decode);

    public static WeightConfigSaveC2SPacket of(SixtySecondsWeightConfig cfg) {
        return new WeightConfigSaveC2SPacket(cfg);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(GSON.toJson(config));
    }

    public static WeightConfigSaveC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new WeightConfigSaveC2SPacket(GSON.fromJson(buf.readUtf(), SixtySecondsWeightConfig.class));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(WeightConfigSaveC2SPacket payload, ServerPlayNetworking.Context context) {
        net.minecraft.server.MinecraftServer server = context.player().getServer();
        if (server != null) {
            SixtySecondsWeightConfigStore.save(server, payload.config());
        }
    }
}
