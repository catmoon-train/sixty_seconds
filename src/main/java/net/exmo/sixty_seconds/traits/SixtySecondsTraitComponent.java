package net.exmo.sixty_seconds.traits;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * 挂在每名玩家身上的天赋特质组件（自动同步到客户端）。
 * 仅保存“已选特质 id 集合”及少量一次性状态（如学习神速是否已触发）。
 * 实际效果由 {@link SixtySecondsTraitSystem} 读取并计算。
 */
public class SixtySecondsTraitComponent implements AutoSyncedComponent {

    public static final ComponentKey<SixtySecondsTraitComponent> KEY = ComponentRegistry.getOrCreate(
            ResourceLocation.fromNamespaceAndPath(SixtySeconds.MOD_ID, "sixty_seconds_trait"),
            SixtySecondsTraitComponent.class);

    private final Player player;
    public Set<String> chosen = new HashSet<>();
    public boolean fastLearnerUsed = false;
    public long lastSmokeTick = -1;
    public long lastAttackEffectTick = -1;

    public SixtySecondsTraitComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean has(String id) {
        return chosen.contains(id);
    }

    public int points() {
        return SixtySecondsTrait.pointsOf(chosen);
    }

    public boolean canAdd(String id) {
        return SixtySecondsTrait.canAdd(chosen, id);
    }

    /** 服务端调用：尝试加点。仅在 60s 游戏进行中（已启动）才允许。 */
    public boolean add(String id) {
        if (!net.exmo.sixty_seconds.SixtySecondsMod.isActive(player.level())) {
            return false;
        }
        if (!canAdd(id)) {
            return false;
        }
        chosen.add(id);
        sync();
        return true;
    }

    /** 游戏结束时调用：清空所有加点与一次性状态。实际清除药水效果由系统负责。 */
    public void reset() {
        chosen.clear();
        fastLearnerUsed = false;
        lastSmokeTick = -1;
        lastAttackEffectTick = -1;
        sync();
    }

    public void sync() {
        if (player instanceof ServerPlayer sp) {
            KEY.sync(sp);
        }
    }

    @Override
    public boolean shouldSyncWith(@NotNull ServerPlayer recipient) {
        return recipient == player;
    }

    @Override
    public void writeSyncPacket(@NotNull RegistryFriendlyByteBuf buf, @NotNull ServerPlayer recipient) {
        buf.writeVarInt(chosen.size());
        for (String id : chosen) {
            buf.writeUtf(id);
        }
        buf.writeBoolean(fastLearnerUsed);
    }

    @Override
    public void applySyncPacket(@NotNull RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        chosen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            chosen.add(buf.readUtf());
        }
        fastLearnerUsed = buf.readBoolean();
    }

    public void writeToNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider reg) {
        // 局内状态不落磁盘，重置即清空
    }

    public void readFromNbt(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider reg) {
        // 局内状态不落磁盘
    }
}
