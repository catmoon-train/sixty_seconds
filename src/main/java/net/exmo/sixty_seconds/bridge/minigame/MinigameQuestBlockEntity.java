package net.exmo.sixty_seconds.bridge.minigame;

import net.exmo.sixty_seconds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MinigameQuestBlockEntity extends BlockEntity {

    private String minigameId = QuestMinigames.getDefaultId();
    private int markerColor = 0xFFD700;
    private boolean isTaskMarker = true;
    private boolean isSabotageTrigger = false;
    private int sabotageDuration = 60;
    private int sabotageCooldown = 300;
    private long lastSabotageTime = 0;

    public MinigameQuestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SIXTY_SECONDS_MINIGAME_QUEST_ENTITY, pos, state);
    }

    public String getMinigameId() {
        return minigameId;
    }

    public void setMinigameId(String id) {
        this.minigameId = id;
        setChanged();
    }

    public QuestMinigame getSelectedMinigame() {
        return QuestMinigames.get(minigameId);
    }

    public int getMarkerColor() {
        return markerColor;
    }

    public void setMarkerColor(int color) {
        this.markerColor = color;
        setChanged();
    }

    public boolean isTaskMarker() {
        return isTaskMarker;
    }

    public void setTaskMarker(boolean marker) {
        this.isTaskMarker = marker;
        setChanged();
    }

    public boolean isSabotageTrigger() {
        return isSabotageTrigger;
    }

    public void setSabotageTrigger(boolean v) {
        this.isSabotageTrigger = v;
        setChanged();
    }

    public int getSabotageDuration() {
        return sabotageDuration;
    }

    public void setSabotageDuration(int seconds) {
        this.sabotageDuration = Math.max(1, seconds);
        setChanged();
    }

    public int getSabotageCooldown() {
        return sabotageCooldown;
    }

    public void setSabotageCooldown(int seconds) {
        this.sabotageCooldown = Math.max(0, seconds);
        setChanged();
    }

    public long getLastSabotageTime() {
        return lastSabotageTime;
    }

    public void setLastSabotageTime(long time) {
        this.lastSabotageTime = time;
        sync();
    }

    public boolean isSabotageOnCooldown(long currentGameTime) {
        long cooldownTicks = (long) this.sabotageCooldown * 20;
        return cooldownTicks > 0 && this.lastSabotageTime > 0
                && currentGameTime - this.lastSabotageTime < cooldownTicks;
    }

    public void loadConfigFromTag(CompoundTag tag) {
        if (tag.contains("MinigameId")) {
            this.minigameId = tag.getString("MinigameId");
        }
        if (tag.contains("MarkerColor")) {
            this.markerColor = tag.getInt("MarkerColor");
        }
        if (tag.contains("IsTaskMarker")) {
            this.isTaskMarker = tag.getBoolean("IsTaskMarker");
        }
        if (tag.contains("IsSabotageTrigger")) {
            this.isSabotageTrigger = tag.getBoolean("IsSabotageTrigger");
        }
        if (tag.contains("SabotageDuration")) {
            this.sabotageDuration = tag.getInt("SabotageDuration");
        }
        if (tag.contains("SabotageCooldown")) {
            this.sabotageCooldown = tag.getInt("SabotageCooldown");
        }
        setChanged();
    }

    public void openConfigUI(ServerPlayer player) {
        MinigameQuestServerNetwork.sendOpenConfig(player, this.worldPosition, this);
    }

    public void sync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("MinigameId", minigameId);
        tag.putInt("MarkerColor", markerColor);
        tag.putBoolean("IsTaskMarker", isTaskMarker);
        tag.putBoolean("IsSabotageTrigger", isSabotageTrigger);
        tag.putInt("SabotageDuration", sabotageDuration);
        tag.putInt("SabotageCooldown", sabotageCooldown);
        tag.putLong("LastSabotageTime", lastSabotageTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MinigameId")) {
            this.minigameId = tag.getString("MinigameId");
        }
        if (tag.contains("MarkerColor")) {
            this.markerColor = tag.getInt("MarkerColor");
        }
        if (tag.contains("IsTaskMarker")) {
            this.isTaskMarker = tag.getBoolean("IsTaskMarker");
        }
        if (tag.contains("IsSabotageTrigger")) {
            this.isSabotageTrigger = tag.getBoolean("IsSabotageTrigger");
        }
        if (tag.contains("SabotageDuration")) {
            this.sabotageDuration = tag.getInt("SabotageDuration");
        }
        if (tag.contains("SabotageCooldown")) {
            this.sabotageCooldown = tag.getInt("SabotageCooldown");
        }
        if (tag.contains("LastSabotageTime")) {
            this.lastSabotageTime = tag.getLong("LastSabotageTime");
        }
    }
}
