package net.exmo.sixty_seconds.bridge.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class PlayerBodyEntity extends Mob implements MenuProvider {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
            PlayerBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final BodyComponent component = new BodyComponent();

    public PlayerBodyEntity(EntityType<? extends PlayerBodyEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public void setOwner(ServerPlayer player) {
        this.entityData.set(OWNER, Optional.of(player.getUUID()));
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);
    }

    public UUID getPlayerUuid() {
        return this.entityData.get(OWNER).orElse(this.getUUID());
    }

    public BodyComponent getComponent() {
        return component;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return this.getCustomName() != null ? this.getCustomName() : Component.translatable("entity.sixty_seconds.player_body");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.sixRows(id, inventory, component.inventory);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.put("Corpse", component.inventory.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        }
        component.inventory.fromTag(tag.getList("Corpse", 10), this.registryAccess());
    }

    public static final class BodyComponent {
        private final SimpleContainer inventory = new SimpleContainer(54);

        public SimpleContainer getCorpseInventory() {
            return inventory;
        }

        public void sync() {
        }
    }
}
