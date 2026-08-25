package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsFlyingVehicleEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsGrenadeEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsRvEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSeaVehicleEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsVehicleEntity;
import net.exmo.sixty_seconds.content.entity.WheelchairEntity;
import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.content.entity.WheelchairFieldItemEntity;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.entity.SuperPigHorseEntity;
import net.exmo.sixty_seconds.entity.RainbowHorseEntity;
import net.exmo.sixty_seconds.entity.CanyuesaHorseEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsAcidSpitEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsArrowEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsNpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SixtySeconds.MOD_ID);

    public static EntityType<PlayerBodyEntity> PLAYER_BODY;
    public static final DeferredHolder<EntityType<?>, EntityType<PlayerBodyEntity>> HOLD_PLAYER_BODY =
            ENTITY_TYPES.register("player_body", () -> {
                PLAYER_BODY = EntityType.Builder.of(PlayerBodyEntity::new, MobCategory.MISC)
                        .sized(0.6f, 0.6f).clientTrackingRange(8).build("player_body");
                return PLAYER_BODY;
            });

    public static EntityType<WheelchairEntity> WHEELCHAIR;
    public static final DeferredHolder<EntityType<?>, EntityType<WheelchairEntity>> HOLD_WHEELCHAIR =
            ENTITY_TYPES.register("wheelchair", () -> {
                WHEELCHAIR = EntityType.Builder.of(WheelchairEntity::new, MobCategory.MISC)
                        .sized(0.8f, 1.6f).clientTrackingRange(8).build("wheelchair");
                return WHEELCHAIR;
            });

    public static EntityType<WheelchairFieldItemEntity> WHEELCHAIR_FIELD_ITEM;
    public static final DeferredHolder<EntityType<?>, EntityType<WheelchairFieldItemEntity>> HOLD_WHEELCHAIR_FIELD_ITEM =
            ENTITY_TYPES.register("wheelchair_field_item", () -> {
                WHEELCHAIR_FIELD_ITEM = EntityType.Builder
                        .<WheelchairFieldItemEntity>of((type, world) -> new WheelchairFieldItemEntity(type, world), MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(10).build("wheelchair_field_item");
                return WHEELCHAIR_FIELD_ITEM;
            });

    public static EntityType<SixtySecondsVehicleEntity> SIXTY_SECONDS_MOTORCYCLE;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsVehicleEntity>> HOLD_MOTORCYCLE =
            ENTITY_TYPES.register("sixty_seconds_motorcycle", () -> {
                SIXTY_SECONDS_MOTORCYCLE = EntityType.Builder
                        .<SixtySecondsVehicleEntity>of((type, world) -> new SixtySecondsVehicleEntity(type, world,
                                SixtySecondsVehicleEntity.Kind.MOTORCYCLE), MobCategory.MISC)
                        .sized(1.8f, 2.8f).clientTrackingRange(10).build("sixty_seconds_motorcycle");
                return SIXTY_SECONDS_MOTORCYCLE;
            });

    public static EntityType<SixtySecondsVehicleEntity> SIXTY_SECONDS_CAR;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsVehicleEntity>> HOLD_CAR =
            ENTITY_TYPES.register("sixty_seconds_car", () -> {
                SIXTY_SECONDS_CAR = EntityType.Builder
                        .<SixtySecondsVehicleEntity>of((type, world) -> new SixtySecondsVehicleEntity(type, world,
                                SixtySecondsVehicleEntity.Kind.CAR), MobCategory.MISC)
                        .sized(4.2f, 4.5f).clientTrackingRange(10).build("sixty_seconds_car");
                return SIXTY_SECONDS_CAR;
            });

    public static EntityType<SixtySecondsRvEntity> SIXTY_SECONDS_RV;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsRvEntity>> HOLD_RV =
            ENTITY_TYPES.register("sixty_seconds_rv", () -> {
                SIXTY_SECONDS_RV = EntityType.Builder.of(SixtySecondsRvEntity::new, MobCategory.MISC)
                        .sized(4.8f, 3.2f).clientTrackingRange(10).build("sixty_seconds_rv");
                return SIXTY_SECONDS_RV;
            });

    public static EntityType<SixtySecondsSeaVehicleEntity> SIXTY_SECONDS_RAFT;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsSeaVehicleEntity>> HOLD_RAFT =
            ENTITY_TYPES.register("sixty_seconds_raft", () -> {
                SIXTY_SECONDS_RAFT = sea(SixtySecondsSeaVehicleEntity.Kind.RAFT, 3.2F, 0.9F, "sixty_seconds_raft");
                return SIXTY_SECONDS_RAFT;
            });
    public static EntityType<SixtySecondsSeaVehicleEntity> SIXTY_SECONDS_MOTORBOAT;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsSeaVehicleEntity>> HOLD_MOTORBOAT =
            ENTITY_TYPES.register("sixty_seconds_motorboat", () -> {
                SIXTY_SECONDS_MOTORBOAT = sea(SixtySecondsSeaVehicleEntity.Kind.MOTORBOAT, 4.5F, 1.8F, "sixty_seconds_motorboat");
                return SIXTY_SECONDS_MOTORBOAT;
            });
    public static EntityType<SixtySecondsSeaVehicleEntity> SIXTY_SECONDS_FISHING_BOAT;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsSeaVehicleEntity>> HOLD_FISHING_BOAT =
            ENTITY_TYPES.register("sixty_seconds_fishing_boat", () -> {
                SIXTY_SECONDS_FISHING_BOAT = sea(SixtySecondsSeaVehicleEntity.Kind.FISHING_BOAT, 9.6F, 4.0F, "sixty_seconds_fishing_boat");
                return SIXTY_SECONDS_FISHING_BOAT;
            });

    public static EntityType<SixtySecondsFlyingVehicleEntity> SIXTY_SECONDS_FLYER;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsFlyingVehicleEntity>> HOLD_FLYER =
            ENTITY_TYPES.register("sixty_seconds_flyer", () -> {
                SIXTY_SECONDS_FLYER = fly(SixtySecondsFlyingVehicleEntity.Kind.FLYER, 2.5F, 1.8F, "sixty_seconds_flyer");
                return SIXTY_SECONDS_FLYER;
            });
    public static EntityType<SixtySecondsFlyingVehicleEntity> SIXTY_SECONDS_HELICOPTER;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsFlyingVehicleEntity>> HOLD_HELICOPTER =
            ENTITY_TYPES.register("sixty_seconds_helicopter", () -> {
                SIXTY_SECONDS_HELICOPTER = fly(SixtySecondsFlyingVehicleEntity.Kind.HELICOPTER, 4.0F, 3.0F, "sixty_seconds_helicopter");
                return SIXTY_SECONDS_HELICOPTER;
            });
    public static EntityType<SixtySecondsFlyingVehicleEntity> SIXTY_SECONDS_AIRPLANE;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsFlyingVehicleEntity>> HOLD_AIRPLANE =
            ENTITY_TYPES.register("sixty_seconds_airplane", () -> {
                SIXTY_SECONDS_AIRPLANE = fly(SixtySecondsFlyingVehicleEntity.Kind.AIRPLANE, 7.0F, 4.5F, "sixty_seconds_airplane");
                return SIXTY_SECONDS_AIRPLANE;
            });

    public static EntityType<SixtySecondsGrenadeEntity> SIXTY_SECONDS_GRENADE;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsGrenadeEntity>> HOLD_GRENADE =
            ENTITY_TYPES.register("sixty_seconds_grenade", () -> {
                SIXTY_SECONDS_GRENADE = EntityType.Builder.of(SixtySecondsGrenadeEntity::new, MobCategory.MISC)
                        .sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(10).build("sixty_seconds_grenade");
                return SIXTY_SECONDS_GRENADE;
            });

    public static EntityType<SixtySecondsMonsterEntity> SIXTY_SECONDS_MONSTER;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsMonsterEntity>> HOLD_MONSTER =
            ENTITY_TYPES.register("sixty_seconds_monster", () -> {
                SIXTY_SECONDS_MONSTER = EntityType.Builder.of(SixtySecondsMonsterEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f).clientTrackingRange(64).build("sixty_seconds_monster");
                return SIXTY_SECONDS_MONSTER;
            });
    public static EntityType<SixtySecondsBossEntity> SIXTY_SECONDS_BOSS;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsBossEntity>> HOLD_BOSS =
            ENTITY_TYPES.register("sixty_seconds_boss", () -> {
                SIXTY_SECONDS_BOSS = EntityType.Builder.of(SixtySecondsBossEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f).clientTrackingRange(96).build("sixty_seconds_boss");
                return SIXTY_SECONDS_BOSS;
            });
    public static EntityType<SixtySecondsArrowEntity> SIXTY_SECONDS_ARROW;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsArrowEntity>> HOLD_ARROW =
            ENTITY_TYPES.register("sixty_seconds_arrow", () -> {
                SIXTY_SECONDS_ARROW = EntityType.Builder.<SixtySecondsArrowEntity>of(SixtySecondsArrowEntity::new, MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(64).updateInterval(20).build("sixty_seconds_arrow");
                return SIXTY_SECONDS_ARROW;
            });
    public static EntityType<SixtySecondsAcidSpitEntity> SIXTY_SECONDS_ACID_SPIT;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsAcidSpitEntity>> HOLD_ACID =
            ENTITY_TYPES.register("sixty_seconds_acid_spit", () -> {
                SIXTY_SECONDS_ACID_SPIT = EntityType.Builder.<SixtySecondsAcidSpitEntity>of(SixtySecondsAcidSpitEntity::new, MobCategory.MISC)
                        .sized(0.25f, 0.25f).clientTrackingRange(64).updateInterval(10).build("sixty_seconds_acid_spit");
                return SIXTY_SECONDS_ACID_SPIT;
            });
    public static EntityType<SixtySecondsNpcEntity> SIXTY_SECONDS_NPC;
    public static final DeferredHolder<EntityType<?>, EntityType<SixtySecondsNpcEntity>> HOLD_NPC =
            ENTITY_TYPES.register("sixty_seconds_npc", () -> {
                SIXTY_SECONDS_NPC = EntityType.Builder.of(SixtySecondsNpcEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.95f).clientTrackingRange(64).build("sixty_seconds_npc");
                return SIXTY_SECONDS_NPC;
            });

    public static EntityType<OceanSharkEntity> OCEAN_SHARK;
    public static final DeferredHolder<EntityType<?>, EntityType<OceanSharkEntity>> HOLD_SHARK =
            ENTITY_TYPES.register("ocean_shark", () -> {
                OCEAN_SHARK = EntityType.Builder.of(OceanSharkEntity::new, MobCategory.WATER_CREATURE)
                        .sized(1.2f, 1.2f).clientTrackingRange(80).updateInterval(2).build("ocean_shark");
                return OCEAN_SHARK;
            });
    public static EntityType<OceanSeaMonsterEntity> OCEAN_SEA_MONSTER;
    public static final DeferredHolder<EntityType<?>, EntityType<OceanSeaMonsterEntity>> HOLD_SEA_MONSTER =
            ENTITY_TYPES.register("ocean_sea_monster", () -> {
                OCEAN_SEA_MONSTER = EntityType.Builder.of(OceanSeaMonsterEntity::new, MobCategory.WATER_CREATURE)
                        .sized(2.0f, 2.5f).clientTrackingRange(128).updateInterval(2).build("ocean_sea_monster");
                return OCEAN_SEA_MONSTER;
            });

    // ── 三种临时坐骑马 ──
    public static EntityType<SuperPigHorseEntity> SUPER_PIG_HORSE;
    public static final DeferredHolder<EntityType<?>, EntityType<SuperPigHorseEntity>> HOLD_SUPER_PIG_HORSE =
            ENTITY_TYPES.register("super_pig_horse", () -> {
                SUPER_PIG_HORSE = EntityType.Builder.of(SuperPigHorseEntity::new, MobCategory.CREATURE)
                        .sized(1.4f, 1.6f).clientTrackingRange(10).build("super_pig_horse");
                return SUPER_PIG_HORSE;
            });
    public static EntityType<RainbowHorseEntity> RAINBOW_HORSE;
    public static final DeferredHolder<EntityType<?>, EntityType<RainbowHorseEntity>> HOLD_RAINBOW_HORSE =
            ENTITY_TYPES.register("rainbow_horse", () -> {
                RAINBOW_HORSE = EntityType.Builder.of(RainbowHorseEntity::new, MobCategory.CREATURE)
                        .sized(1.4f, 1.6f).clientTrackingRange(10).build("rainbow_horse");
                return RAINBOW_HORSE;
            });
    public static EntityType<CanyuesaHorseEntity> CANYUESA_HORSE;
    public static final DeferredHolder<EntityType<?>, EntityType<CanyuesaHorseEntity>> HOLD_CANYUESA_HORSE =
            ENTITY_TYPES.register("canyuesa_horse", () -> {
                CANYUESA_HORSE = EntityType.Builder.of(CanyuesaHorseEntity::new, MobCategory.CREATURE)
                        .sized(1.4f, 1.6f).clientTrackingRange(10).build("canyuesa_horse");
                return CANYUESA_HORSE;
            });

    private static EntityType<SixtySecondsSeaVehicleEntity> sea(SixtySecondsSeaVehicleEntity.Kind kind,
            float w, float h, String name) {
        return EntityType.Builder.<SixtySecondsSeaVehicleEntity>of(
                (type, world) -> new SixtySecondsSeaVehicleEntity(type, world, kind), MobCategory.MISC)
                .sized(w, h).clientTrackingRange(10).build(name);
    }

    private static EntityType<SixtySecondsFlyingVehicleEntity> fly(SixtySecondsFlyingVehicleEntity.Kind kind,
            float w, float h, String name) {
        return EntityType.Builder.<SixtySecondsFlyingVehicleEntity>of(
                (type, world) -> new SixtySecondsFlyingVehicleEntity(type, world, kind), MobCategory.MISC)
                .sized(w, h).clientTrackingRange(10).build(name);
    }

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        bus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(PLAYER_BODY, PlayerBodyEntity.createAttributes().build());
        event.put(WHEELCHAIR, WheelchairEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_MOTORCYCLE, WheelchairEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_CAR, WheelchairEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_RV, WheelchairEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_FLYER, SixtySecondsFlyingVehicleEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_HELICOPTER, SixtySecondsFlyingVehicleEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_AIRPLANE, SixtySecondsFlyingVehicleEntity.createAttributes().build());
        event.put(SIXTY_SECONDS_MONSTER, Zombie.createAttributes().build());
        event.put(SIXTY_SECONDS_BOSS, Zombie.createAttributes().build());
        event.put(SIXTY_SECONDS_NPC, SixtySecondsNpcEntity.createAttributes().build());
        event.put(OCEAN_SHARK, OceanSharkEntity.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 40.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.30)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 8.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 32.0)
                .build());
        event.put(SUPER_PIG_HORSE, SuperPigHorseEntity.createAttributes().build());
        event.put(RAINBOW_HORSE, RainbowHorseEntity.createAttributes().build());
        event.put(CANYUESA_HORSE, CanyuesaHorseEntity.createAttributes().build());
        event.put(OCEAN_SEA_MONSTER, OceanSeaMonsterEntity.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 600.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 35.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 48.0)
                .build());
    }
}
