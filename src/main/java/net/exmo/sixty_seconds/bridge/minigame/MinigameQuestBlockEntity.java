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
    }
}
