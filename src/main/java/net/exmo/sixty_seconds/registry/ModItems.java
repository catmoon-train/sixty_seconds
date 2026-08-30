package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.item.SeaChartItem;
import net.exmo.sixty_seconds.content.item.StarMapItem;
import net.exmo.sixty_seconds.content.item.CanyuesaHorseshoeItem;
import net.exmo.sixty_seconds.content.item.PredecessorHorseArmorItem;
import net.exmo.sixty_seconds.content.item.RainbowHorseshoeItem;
import net.exmo.sixty_seconds.content.item.SuperPigHorseshoeItem;
import net.exmo.sixty_seconds.content.entity.SixtySecondsRvPart;
import net.exmo.sixty_seconds.content.item.SixtySecondsRvPartItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.core.component.DataComponents;
import net.exmo.sixty_seconds.content.item.MiningShearsItem;
import net.exmo.sixty_seconds.content.item.MiningToolItem;
import net.exmo.sixty_seconds.content.item.SixtySecondsBoxPryItem;
import net.exmo.sixty_seconds.content.item.SixtySecondsPliersItem;
import net.exmo.sixty_seconds.content.item.SixtySecondsPhoneItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Tool;
import java.util.ArrayList;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SixtySeconds.MOD_ID);

    public static Item SEA_CHART;
    public static final DeferredItem<Item> HOLD_SEA_CHART = ITEMS.register("sea_chart", () -> {
        SEA_CHART = new SeaChartItem(new Item.Properties().stacksTo(1));
        return SEA_CHART;
    });

    public static Item STAR_MAP;
    public static final DeferredItem<Item> HOLD_STAR_MAP = ITEMS.register("star_map", () -> {
        STAR_MAP = new StarMapItem(new Item.Properties().stacksTo(1));
        return STAR_MAP;
    });

    public static Item EVAC_COMPASS;
    public static final DeferredItem<Item> HOLD_EVAC_COMPASS = ITEMS.register("sixty_seconds_evac_compass", () -> {
        EVAC_COMPASS = new net.exmo.sixty_seconds.item.EvacCompassItem(new Item.Properties().stacksTo(1));
        return EVAC_COMPASS;
    });

    public static Item SIXTY_SECONDS_WATER_SMALL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WATER_SMALL = ITEMS.register("sixty_seconds_water_small", () -> {
        SIXTY_SECONDS_WATER_SMALL = new net.exmo.sixty_seconds.content.item.SixtySecondsWaterItem(
                    new Item.Properties().stacksTo(1), "small", 15);
        return SIXTY_SECONDS_WATER_SMALL;
    });

    public static Item SIXTY_SECONDS_WATER_MEDIUM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WATER_MEDIUM = ITEMS.register("sixty_seconds_water_medium", () -> {
        SIXTY_SECONDS_WATER_MEDIUM = new net.exmo.sixty_seconds.content.item.SixtySecondsWaterItem(
                    new Item.Properties().stacksTo(1), "medium", 35);
        return SIXTY_SECONDS_WATER_MEDIUM;
    });

    public static Item SIXTY_SECONDS_WATER_HIGH;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WATER_HIGH = ITEMS.register("sixty_seconds_water_high", () -> {
        SIXTY_SECONDS_WATER_HIGH = new net.exmo.sixty_seconds.content.item.SixtySecondsWaterItem(
                    new Item.Properties().stacksTo(1), "high", 60);
        return SIXTY_SECONDS_WATER_HIGH;
    });

    public static Item SIXTY_SECONDS_UMBRELLA;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_UMBRELLA = ITEMS.register("sixty_seconds_umbrella", () -> {
        SIXTY_SECONDS_UMBRELLA = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_UMBRELLA;
    });

    public static Item SIXTY_SECONDS_CROWBAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CROWBAR = ITEMS.register("sixty_seconds_crowbar", () -> {
        SIXTY_SECONDS_CROWBAR = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), true);
        return SIXTY_SECONDS_CROWBAR;
    });

    public static Item SIXTY_SECONDS_LOCKPICK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LOCKPICK = ITEMS.register("sixty_seconds_lockpick", () -> {
        SIXTY_SECONDS_LOCKPICK = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), false, 2);
        return SIXTY_SECONDS_LOCKPICK;
    });

    public static Item SIXTY_SECONDS_DOOR_LOCK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOR_LOCK = ITEMS.register("sixty_seconds_door_lock", () -> {
        SIXTY_SECONDS_DOOR_LOCK = new net.exmo.sixty_seconds.content.item.SixtySecondsDoorLockItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_DOOR_LOCK;
    });

    public static Item SIXTY_SECONDS_DOOR_TRAP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOR_TRAP = ITEMS.register("sixty_seconds_door_trap", () -> {
        SIXTY_SECONDS_DOOR_TRAP = new net.exmo.sixty_seconds.content.item.SixtySecondsDoorTrapItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_DOOR_TRAP;
    });

    public static Item SIXTY_SECONDS_AREA_WAND;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_AREA_WAND = ITEMS.register("sixty_seconds_area_wand", () -> {
        SIXTY_SECONDS_AREA_WAND = new net.exmo.sixty_seconds.content.item.SixtySecondsAreaWandItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_AREA_WAND;
    });

    public static Item SIXTY_SECONDS_NPC_PLACER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NPC_PLACER = ITEMS.register("sixty_seconds_npc_placer", () -> {
        SIXTY_SECONDS_NPC_PLACER = new net.exmo.sixty_seconds.content.item.SixtySecondsNpcPlacerItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_NPC_PLACER;
    });

    public static Item SIXTY_SECONDS_NPC_TUNER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NPC_TUNER = ITEMS.register("sixty_seconds_npc_tuner", () -> {
        SIXTY_SECONDS_NPC_TUNER = new net.exmo.sixty_seconds.content.item.SixtySecondsNpcTunerItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_NPC_TUNER;
    });

    public static Item SIXTY_SECONDS_ANCHOR_WAND;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ANCHOR_WAND = ITEMS.register("sixty_seconds_anchor_wand", () -> {
        SIXTY_SECONDS_ANCHOR_WAND = new net.exmo.sixty_seconds.content.item.SixtySecondsAnchorWandItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_ANCHOR_WAND;
    });

    public static Item SIXTY_SECONDS_LEVEL_WAND;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LEVEL_WAND = ITEMS.register("sixty_seconds_level_wand", () -> {
        SIXTY_SECONDS_LEVEL_WAND = new net.exmo.sixty_seconds.content.item.SixtySecondsLevelWandItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_LEVEL_WAND;
    });

    public static Item SIXTY_SECONDS_BOSS_WAND;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BOSS_WAND = ITEMS.register("sixty_seconds_boss_wand", () -> {
        SIXTY_SECONDS_BOSS_WAND = new net.exmo.sixty_seconds.content.item.SixtySecondsBossWandItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_BOSS_WAND;
    });

    public static Item SIXTY_SECONDS_RESCUE_BEACON;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RESCUE_BEACON = ITEMS.register("sixty_seconds_rescue_beacon", () -> {
        SIXTY_SECONDS_RESCUE_BEACON = new net.exmo.sixty_seconds.content.item.SixtySecondsRescueBeaconItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_RESCUE_BEACON;
    });

    public static Item SIXTY_SECONDS_MEDICINE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MEDICINE = ITEMS.register("sixty_seconds_medicine", () -> {
        SIXTY_SECONDS_MEDICINE = new net.exmo.sixty_seconds.content.item.SixtySecondsMedicineItem(
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_MEDICINE;
    });

    public static Item SIXTY_SECONDS_SCRAP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP = ITEMS.register("sixty_seconds_scrap", () -> {
        SIXTY_SECONDS_SCRAP = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_SCRAP;
    });

    public static Item SIXTY_SECONDS_RAG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RAG = ITEMS.register("sixty_seconds_rag", () -> {
        SIXTY_SECONDS_RAG = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_RAG;
    });

    public static Item SIXTY_SECONDS_ALCOHOL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALCOHOL = ITEMS.register("sixty_seconds_alcohol", () -> {
        SIXTY_SECONDS_ALCOHOL = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(16), 0, 0, 15, 15, 0, false, null,
                    40, UseAnim.DRINK);
        return SIXTY_SECONDS_ALCOHOL;
    });

    public static Item SIXTY_SECONDS_DIRTY_WATER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIRTY_WATER = ITEMS.register("sixty_seconds_dirty_water", () -> {
        SIXTY_SECONDS_DIRTY_WATER = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_DIRTY_WATER;
    });

    public static Item SIXTY_SECONDS_BANDAGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BANDAGE = ITEMS.register("sixty_seconds_bandage", () -> {
        SIXTY_SECONDS_BANDAGE = new net.exmo.sixty_seconds.content.item.SixtySecondsBandageItem(
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_BANDAGE;
    });

    public static Item SIXTY_SECONDS_TORCH;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TORCH = ITEMS.register("sixty_seconds_torch", () -> {
        SIXTY_SECONDS_TORCH = new net.exmo.sixty_seconds.content.item.SixtySecondsTorchItem(
                    new Item.Properties().stacksTo(1).durability(200));
        return SIXTY_SECONDS_TORCH;
    });

    public static Item SIXTY_SECONDS_CLOCK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CLOCK = ITEMS.register("sixty_seconds_clock", () -> {
        SIXTY_SECONDS_CLOCK = new net.exmo.sixty_seconds.content.item.SixtySecondsClockItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_CLOCK;
    });

    public static Item SIXTY_SECONDS_WRENCH;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WRENCH = ITEMS.register("sixty_seconds_wrench", () -> {
        SIXTY_SECONDS_WRENCH = new net.exmo.sixty_seconds.content.item.SixtySecondsWrenchItem(
                    new Item.Properties().stacksTo(1).durability(60));
        return SIXTY_SECONDS_WRENCH;
    });

    public static Item SIXTY_SECONDS_PIPE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PIPE = ITEMS.register("sixty_seconds_pipe", () -> {
        SIXTY_SECONDS_PIPE = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(36)
                            .attributes(SwordItem.createAttributes(Tiers.STONE, 6, -2.4F)), 23);
        return SIXTY_SECONDS_PIPE;
    });

    public static Item SIXTY_SECONDS_SPIKED_BAT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SPIKED_BAT = ITEMS.register("sixty_seconds_spiked_bat", () -> {
        SIXTY_SECONDS_SPIKED_BAT = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(42)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 6, -2.6F)), 27);
        return SIXTY_SECONDS_SPIKED_BAT;
    });

    public static Item SIXTY_SECONDS_MACHETE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MACHETE = ITEMS.register("sixty_seconds_machete", () -> {
        SIXTY_SECONDS_MACHETE = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(60)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 8, -2.2F)), 32);
        return SIXTY_SECONDS_MACHETE;
    });

    public static Item SIXTY_SECONDS_ASSAULT_SPAWNER_WEAK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ASSAULT_SPAWNER_WEAK = ITEMS.register("sixty_seconds_assault_spawner_weak", () -> {
        SIXTY_SECONDS_ASSAULT_SPAWNER_WEAK = new net.exmo.sixty_seconds.content.item.SixtySecondsAssaultSpawnItem(
                    new Item.Properties().stacksTo(16),
                    net.exmo.sixty_seconds.logic.SixtySecondsDefenseSystem.AssaultTier.WEAK);
        return SIXTY_SECONDS_ASSAULT_SPAWNER_WEAK;
    });

    public static Item SIXTY_SECONDS_ASSAULT_SPAWNER_MEDIUM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ASSAULT_SPAWNER_MEDIUM = ITEMS.register("sixty_seconds_assault_spawner_medium", () -> {
        SIXTY_SECONDS_ASSAULT_SPAWNER_MEDIUM = new net.exmo.sixty_seconds.content.item.SixtySecondsAssaultSpawnItem(
                    new Item.Properties().stacksTo(16),
                    net.exmo.sixty_seconds.logic.SixtySecondsDefenseSystem.AssaultTier.MEDIUM);
        return SIXTY_SECONDS_ASSAULT_SPAWNER_MEDIUM;
    });

    public static Item SIXTY_SECONDS_ASSAULT_SPAWNER_STRONG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ASSAULT_SPAWNER_STRONG = ITEMS.register("sixty_seconds_assault_spawner_strong", () -> {
        SIXTY_SECONDS_ASSAULT_SPAWNER_STRONG = new net.exmo.sixty_seconds.content.item.SixtySecondsAssaultSpawnItem(
                    new Item.Properties().stacksTo(16),
                    net.exmo.sixty_seconds.logic.SixtySecondsDefenseSystem.AssaultTier.STRONG);
        return SIXTY_SECONDS_ASSAULT_SPAWNER_STRONG;
    });

    public static Item SIXTY_SECONDS_COIN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COIN = ITEMS.register("sixty_seconds_coin", () -> {
        SIXTY_SECONDS_COIN = new net.exmo.sixty_seconds.content.item.SixtySecondsCoinItem(
                    new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_COIN;
    });

    public static Item SIXTY_SECONDS_AMMO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_AMMO = ITEMS.register("sixty_seconds_ammo", () -> {
        SIXTY_SECONDS_AMMO = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_AMMO;
    });

    public static Item SIXTY_SECONDS_PISTOL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PISTOL = ITEMS.register("sixty_seconds_pistol", () -> {
        SIXTY_SECONDS_PISTOL = new net.exmo.sixty_seconds.content.item.SixtySecondsGunItem(
                    new Item.Properties().durability(30),
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_PISTOL_COOLDOWN,
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_PISTOL_RANGE,30,1);
        return SIXTY_SECONDS_PISTOL;
    });

    public static Item SIXTY_SECONDS_HUNTING_SHOTGUN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HUNTING_SHOTGUN = ITEMS.register("sixty_seconds_hunting_shotgun", () -> {
        SIXTY_SECONDS_HUNTING_SHOTGUN = new net.exmo.sixty_seconds.content.item.SixtySecondsGunItem(
                    new Item.Properties().durability(18),
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_SHOTGUN_COOLDOWN,
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_SHOTGUN_RANGE,40,1);
        return SIXTY_SECONDS_HUNTING_SHOTGUN;
    });

    public static Item SIXTY_SECONDS_RIFLE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RIFLE = ITEMS.register("sixty_seconds_rifle", () -> {
        SIXTY_SECONDS_RIFLE = new net.exmo.sixty_seconds.content.item.SixtySecondsGunItem(
                    new Item.Properties().durability(36),
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_RIFLE_COOLDOWN,
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_RIFLE_RANGE, 50, 1, false,
                    () -> ModItems.SIXTY_SECONDS_RIFLE_AMMO, 0, 0);
        return SIXTY_SECONDS_RIFLE;
    });

    public static Item SIXTY_SECONDS_SNIPER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SNIPER = ITEMS.register("sixty_seconds_sniper", () -> {
        SIXTY_SECONDS_SNIPER = new net.exmo.sixty_seconds.content.item.SixtySecondsGunItem(
                    new Item.Properties().durability(12),
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_SNIPER_COOLDOWN,
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_SNIPER_RANGE,
                    net.exmo.sixty_seconds.SixtySecondsBalance.GUN_SNIPER_DAMAGE, 1, true,
                    () -> ModItems.SIXTY_SECONDS_MAGNUM_AMMO, 0, 0);
        return SIXTY_SECONDS_SNIPER;
    });

    public static Item SIXTY_SECONDS_RPG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RPG = ITEMS.register("sixty_seconds_rpg", () -> {
        SIXTY_SECONDS_RPG = new net.exmo.sixty_seconds.content.item.SixtySecondsRpgItem(
                    new Item.Properties().durability(12));
        return SIXTY_SECONDS_RPG;
    });

    public static Item SIXTY_SECONDS_SCRAP_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP_HELMET = ITEMS.register("sixty_seconds_scrap_helmet", () -> {
        SIXTY_SECONDS_SCRAP_HELMET = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_SCRAP_HELMET;
    });

    public static Item SIXTY_SECONDS_SCRAP_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP_CHESTPLATE = ITEMS.register("sixty_seconds_scrap_chestplate", () -> {
        SIXTY_SECONDS_SCRAP_CHESTPLATE = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_SCRAP_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_IRON_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_HELMET = ITEMS.register("sixty_seconds_iron_helmet", () -> {
        SIXTY_SECONDS_IRON_HELMET = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_IRON_HELMET;
    });

    public static Item SIXTY_SECONDS_IRON_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_CHESTPLATE = ITEMS.register("sixty_seconds_iron_chestplate", () -> {
        SIXTY_SECONDS_IRON_CHESTPLATE = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_IRON_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_BACKPACK_SMALL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BACKPACK_SMALL = ITEMS.register("sixty_seconds_backpack_small", () -> {
        SIXTY_SECONDS_BACKPACK_SMALL = new net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem(
                    new Item.Properties().stacksTo(1), 1);
        return SIXTY_SECONDS_BACKPACK_SMALL;
    });

    public static Item SIXTY_SECONDS_BACKPACK_MEDIUM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BACKPACK_MEDIUM = ITEMS.register("sixty_seconds_backpack_medium", () -> {
        SIXTY_SECONDS_BACKPACK_MEDIUM = new net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem(
                    new Item.Properties().stacksTo(1), 2);
        return SIXTY_SECONDS_BACKPACK_MEDIUM;
    });

    public static Item SIXTY_SECONDS_BACKPACK_LARGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BACKPACK_LARGE = ITEMS.register("sixty_seconds_backpack_large", () -> {
        SIXTY_SECONDS_BACKPACK_LARGE = new net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem(
                    new Item.Properties().stacksTo(1), 3);
        return SIXTY_SECONDS_BACKPACK_LARGE;
    });

    public static Item SIXTY_SECONDS_UNLOCK_SLOTS_SMALL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_UNLOCK_SLOTS_SMALL = ITEMS.register("sixty_seconds_unlock_slots_small", () -> {
        SIXTY_SECONDS_UNLOCK_SLOTS_SMALL = new net.exmo.sixty_seconds.content.item.SixtySecondsUnlockItem(
                    new Item.Properties().stacksTo(1), 1);
        return SIXTY_SECONDS_UNLOCK_SLOTS_SMALL;
    });

    public static Item SIXTY_SECONDS_UNLOCK_SLOTS_MEDIUM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_UNLOCK_SLOTS_MEDIUM = ITEMS.register("sixty_seconds_unlock_slots_medium", () -> {
        SIXTY_SECONDS_UNLOCK_SLOTS_MEDIUM = new net.exmo.sixty_seconds.content.item.SixtySecondsUnlockItem(
                    new Item.Properties().stacksTo(1), 2);
        return SIXTY_SECONDS_UNLOCK_SLOTS_MEDIUM;
    });

    public static Item SIXTY_SECONDS_UNLOCK_SLOTS_LARGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_UNLOCK_SLOTS_LARGE = ITEMS.register("sixty_seconds_unlock_slots_large", () -> {
        SIXTY_SECONDS_UNLOCK_SLOTS_LARGE = new net.exmo.sixty_seconds.content.item.SixtySecondsUnlockItem(
                    new Item.Properties().stacksTo(1), 3);
        return SIXTY_SECONDS_UNLOCK_SLOTS_LARGE;
    });

    public static Item SIXTY_SECONDS_DUCT_TAPE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DUCT_TAPE = ITEMS.register("sixty_seconds_duct_tape", () -> {
        SIXTY_SECONDS_DUCT_TAPE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_DUCT_TAPE;
    });

    public static Item SIXTY_SECONDS_BATTERY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BATTERY = ITEMS.register("sixty_seconds_battery", () -> {
        SIXTY_SECONDS_BATTERY = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_BATTERY;
    });

    public static Item SIXTY_SECONDS_FLASHLIGHT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FLASHLIGHT = ITEMS.register("sixty_seconds_flashlight", () -> {
        SIXTY_SECONDS_FLASHLIGHT = new net.exmo.sixty_seconds.content.item.SixtySecondsFlashlightItem(
                    new Item.Properties().stacksTo(1).durability(150));
        return SIXTY_SECONDS_FLASHLIGHT;
    });

    public static Item SIXTY_SECONDS_POKER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_POKER = ITEMS.register("sixty_seconds_poker", () -> {
        SIXTY_SECONDS_POKER = new net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem(
                    new Item.Properties().stacksTo(1).durability(3),
                    net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem.Kind.POKER, 8);
        return SIXTY_SECONDS_POKER;
    });

    public static Item SIXTY_SECONDS_CHESS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHESS = ITEMS.register("sixty_seconds_chess", () -> {
        SIXTY_SECONDS_CHESS = new net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem(
                    new Item.Properties().stacksTo(1).durability(2),
                    net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem.Kind.CHESS, 10);
        return SIXTY_SECONDS_CHESS;
    });

    public static Item SIXTY_SECONDS_HARMONICA;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HARMONICA = ITEMS.register("sixty_seconds_harmonica", () -> {
        SIXTY_SECONDS_HARMONICA = new net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem(
                    new Item.Properties().stacksTo(1).durability(3),
                    net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem.Kind.HARMONICA, 6);
        return SIXTY_SECONDS_HARMONICA;
    });

    public static Item SIXTY_SECONDS_GUITAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GUITAR = ITEMS.register("sixty_seconds_guitar", () -> {
        SIXTY_SECONDS_GUITAR = new net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem(
                    new Item.Properties().stacksTo(1).durability(2),
                    net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem.Kind.GUITAR, 12);
        return SIXTY_SECONDS_GUITAR;
    });

    public static Item SIXTY_SECONDS_TEDDY_BEAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TEDDY_BEAR = ITEMS.register("sixty_seconds_teddy_bear", () -> {
        SIXTY_SECONDS_TEDDY_BEAR = new net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem(
                    new Item.Properties().stacksTo(1).durability(2),
                    net.exmo.sixty_seconds.content.item.SixtySecondsEntertainmentItem.Kind.TEDDY_BEAR, 15);
        return SIXTY_SECONDS_TEDDY_BEAR;
    });

    public static Item SIXTY_SECONDS_PLASTIC;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLASTIC = ITEMS.register("sixty_seconds_plastic", () -> {
        SIXTY_SECONDS_PLASTIC = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_PLASTIC;
    });

    public static Item SIXTY_SECONDS_GLASS_SHARD;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GLASS_SHARD = ITEMS.register("sixty_seconds_glass_shard", () -> {
        SIXTY_SECONDS_GLASS_SHARD = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_GLASS_SHARD;
    });

    public static Item SIXTY_SECONDS_WIRE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WIRE = ITEMS.register("sixty_seconds_wire", () -> {
        SIXTY_SECONDS_WIRE = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_WIRE;
    });

    public static Item SIXTY_SECONDS_GEAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GEAR = ITEMS.register("sixty_seconds_gear", () -> {
        SIXTY_SECONDS_GEAR = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_GEAR;
    });

    public static Item SIXTY_SECONDS_FUEL_CAN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FUEL_CAN = ITEMS.register("sixty_seconds_fuel_can", () -> {
        SIXTY_SECONDS_FUEL_CAN = new Item(new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_FUEL_CAN;
    });

    public static Item SIXTY_SECONDS_CLOTH_ROLL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CLOTH_ROLL = ITEMS.register("sixty_seconds_cloth_roll", () -> {
        SIXTY_SECONDS_CLOTH_ROLL = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_CLOTH_ROLL;
    });

    public static Item SIXTY_SECONDS_ROPE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ROPE = ITEMS.register("sixty_seconds_rope", () -> {
        SIXTY_SECONDS_ROPE = new net.exmo.sixty_seconds.content.item.SixtySecondsRopeItem(
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_ROPE;
    });

    public static Item SIXTY_SECONDS_GRAPPLING_HOOK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GRAPPLING_HOOK = ITEMS.register("sixty_seconds_grappling_hook", () -> {
        SIXTY_SECONDS_GRAPPLING_HOOK = new net.exmo.sixty_seconds.content.item.SixtySecondsGrapplingHookItem(
                    new Item.Properties().stacksTo(1)
                            .durability(net.exmo.sixty_seconds.SixtySecondsBalance.GRAPPLE_DURABILITY));
        return SIXTY_SECONDS_GRAPPLING_HOOK;
    });

    public static Item SIXTY_SECONDS_CLAW_HOOK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CLAW_HOOK = ITEMS.register("sixty_seconds_claw_hook", () -> {
        SIXTY_SECONDS_CLAW_HOOK = new net.exmo.sixty_seconds.content.item.SixtySecondsClawHookItem(
                    new Item.Properties().stacksTo(1).durability(36));
        return SIXTY_SECONDS_CLAW_HOOK;
    });

    public static Item SIXTY_SECONDS_CRUDE_BOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CRUDE_BOW = ITEMS.register("sixty_seconds_crude_bow", () -> {
        SIXTY_SECONDS_CRUDE_BOW = new net.exmo.sixty_seconds.content.item.SixtySecondsBowItem(
                    new Item.Properties().stacksTo(1).durability(480), 0.9F, 24);
        return SIXTY_SECONDS_CRUDE_BOW;
    });

    public static Item SIXTY_SECONDS_HUNTING_BOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HUNTING_BOW = ITEMS.register("sixty_seconds_hunting_bow", () -> {
        SIXTY_SECONDS_HUNTING_BOW = new net.exmo.sixty_seconds.content.item.SixtySecondsBowItem(
                    new Item.Properties().stacksTo(1).durability(900), 1.1F, 20);
        return SIXTY_SECONDS_HUNTING_BOW;
    });

    public static Item SIXTY_SECONDS_RECURVE_BOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RECURVE_BOW = ITEMS.register("sixty_seconds_recurve_bow", () -> {
        SIXTY_SECONDS_RECURVE_BOW = new net.exmo.sixty_seconds.content.item.SixtySecondsBowItem(
                    new Item.Properties().stacksTo(1).durability(1350), 1.3F, 20);
        return SIXTY_SECONDS_RECURVE_BOW;
    });

    public static Item SIXTY_SECONDS_COMPOUND_BOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COMPOUND_BOW = ITEMS.register("sixty_seconds_compound_bow", () -> {
        SIXTY_SECONDS_COMPOUND_BOW = new net.exmo.sixty_seconds.content.item.SixtySecondsBowItem(
                    new Item.Properties().stacksTo(1).durability(1800), 1.5F, 18);
        return SIXTY_SECONDS_COMPOUND_BOW;
    });

    public static Item SIXTY_SECONDS_HAND_CROSSBOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HAND_CROSSBOW = ITEMS.register("sixty_seconds_hand_crossbow", () -> {
        SIXTY_SECONDS_HAND_CROSSBOW = new net.exmo.sixty_seconds.content.item.SixtySecondsCrossbowItem(
                    new Item.Properties().stacksTo(1).durability(960), 1.25F, 25);
        return SIXTY_SECONDS_HAND_CROSSBOW;
    });

    public static Item SIXTY_SECONDS_HEAVY_CROSSBOW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HEAVY_CROSSBOW = ITEMS.register("sixty_seconds_heavy_crossbow", () -> {
        SIXTY_SECONDS_HEAVY_CROSSBOW = new net.exmo.sixty_seconds.content.item.SixtySecondsCrossbowItem(
                    new Item.Properties().stacksTo(1).durability(1560), 1.7F, 25);
        return SIXTY_SECONDS_HEAVY_CROSSBOW;
    });

    public static Item SIXTY_SECONDS_CRUDE_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CRUDE_ARROW = ITEMS.register("sixty_seconds_crude_arrow", () -> {
        SIXTY_SECONDS_CRUDE_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.CRUDE);
        return SIXTY_SECONDS_CRUDE_ARROW;
    });

    public static Item SIXTY_SECONDS_IRON_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_ARROW = ITEMS.register("sixty_seconds_iron_arrow", () -> {
        SIXTY_SECONDS_IRON_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.IRON);
        return SIXTY_SECONDS_IRON_ARROW;
    });

    public static Item SIXTY_SECONDS_STEEL_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_ARROW = ITEMS.register("sixty_seconds_steel_arrow", () -> {
        SIXTY_SECONDS_STEEL_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.STEEL);
        return SIXTY_SECONDS_STEEL_ARROW;
    });

    public static Item SIXTY_SECONDS_FIRE_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FIRE_ARROW = ITEMS.register("sixty_seconds_fire_arrow", () -> {
        SIXTY_SECONDS_FIRE_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.FIRE);
        return SIXTY_SECONDS_FIRE_ARROW;
    });

    public static Item SIXTY_SECONDS_POISON_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_POISON_ARROW = ITEMS.register("sixty_seconds_poison_arrow", () -> {
        SIXTY_SECONDS_POISON_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.POISON);
        return SIXTY_SECONDS_POISON_ARROW;
    });

    public static Item SIXTY_SECONDS_EXPLOSIVE_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EXPLOSIVE_ARROW = ITEMS.register("sixty_seconds_explosive_arrow", () -> {
        SIXTY_SECONDS_EXPLOSIVE_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.EXPLOSIVE);
        return SIXTY_SECONDS_EXPLOSIVE_ARROW;
    });

    public static Item SIXTY_SECONDS_TAINTED_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TAINTED_ARROW = ITEMS.register("sixty_seconds_tained_arrow", () -> {
        SIXTY_SECONDS_TAINTED_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.TAINTED);
        return SIXTY_SECONDS_TAINTED_ARROW;
    });

    public static Item SIXTY_SECONDS_WHEEL_BREAKER_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WHEEL_BREAKER_ARROW = ITEMS.register("sixty_seconds_wheel_breaker_arrow", () -> {
        SIXTY_SECONDS_WHEEL_BREAKER_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.WHEEL_BREAKER);
        return SIXTY_SECONDS_WHEEL_BREAKER_ARROW;
    });

    public static Item SIXTY_SECONDS_ARMOR_PIERCING_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ARMOR_PIERCING_ARROW = ITEMS.register("sixty_seconds_armor_piercing_arrow", () -> {
        SIXTY_SECONDS_ARMOR_PIERCING_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.ARMOR_PIERCING);
        return SIXTY_SECONDS_ARMOR_PIERCING_ARROW;
    });

    public static Item SIXTY_SECONDS_GLOWING_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GLOWING_ARROW = ITEMS.register("sixty_seconds_glowing_arrow", () -> {
        SIXTY_SECONDS_GLOWING_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.GLOWING);
        return SIXTY_SECONDS_GLOWING_ARROW;
    });

    public static Item SIXTY_SECONDS_BLINDING_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BLINDING_ARROW = ITEMS.register("sixty_seconds_blinding_arrow", () -> {
        SIXTY_SECONDS_BLINDING_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.BLINDING);
        return SIXTY_SECONDS_BLINDING_ARROW;
    });

    public static Item SIXTY_SECONDS_ALLOY_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_ARROW = ITEMS.register("sixty_seconds_alloy_arrow", () -> {
        SIXTY_SECONDS_ALLOY_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.ALLOY);
        return SIXTY_SECONDS_ALLOY_ARROW;
    });

    public static Item SIXTY_SECONDS_HUNTING_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HUNTING_ARROW = ITEMS.register("sixty_seconds_hunting_arrow", () -> {
        SIXTY_SECONDS_HUNTING_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.HUNTING);
        return SIXTY_SECONDS_HUNTING_ARROW;
    });

    public static Item SIXTY_SECONDS_EFFECT_REMOVE_ARROW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EFFECT_REMOVE_ARROW = ITEMS.register("sixty_seconds_effect_remove_arrow", () -> {
        SIXTY_SECONDS_EFFECT_REMOVE_ARROW = new net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem(new Item.Properties().stacksTo(64),
                    net.exmo.sixty_seconds.content.item.SixtySecondsArrowItem.ArrowType.EFFECT_REMOVE);
        return SIXTY_SECONDS_EFFECT_REMOVE_ARROW;
    });

    public static Item SIXTY_SECONDS_CHEMICALS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHEMICALS = ITEMS.register("sixty_seconds_chemicals", () -> {
        SIXTY_SECONDS_CHEMICALS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_CHEMICALS;
    });

    public static Item SIXTY_SECONDS_ELECTRONICS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ELECTRONICS = ITEMS.register("sixty_seconds_electronics", () -> {
        SIXTY_SECONDS_ELECTRONICS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_ELECTRONICS;
    });

    public static Item SIXTY_SECONDS_GUNPOWDER_PACK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GUNPOWDER_PACK = ITEMS.register("sixty_seconds_gunpowder_pack", () -> {
        SIXTY_SECONDS_GUNPOWDER_PACK = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_GUNPOWDER_PACK;
    });

    public static Item SIXTY_SECONDS_CANNED_FOOD;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CANNED_FOOD = ITEMS.register("sixty_seconds_canned_food", () -> {
        SIXTY_SECONDS_CANNED_FOOD = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build()));
        return SIXTY_SECONDS_CANNED_FOOD;
    });

    public static Item SIXTY_SECONDS_MRE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MRE = ITEMS.register("sixty_seconds_mre", () -> {
        SIXTY_SECONDS_MRE = new Item(new Item.Properties().stacksTo(4).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(12).saturationModifier(0.8F).build()));
        return SIXTY_SECONDS_MRE;
    });

    public static Item SIXTY_SECONDS_BISCUIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BISCUIT = ITEMS.register("sixty_seconds_biscuit", () -> {
        SIXTY_SECONDS_BISCUIT = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.5F).build()));
        return SIXTY_SECONDS_BISCUIT;
    });

    public static Item SIXTY_SECONDS_JERKY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_JERKY = ITEMS.register("sixty_seconds_jerky", () -> {
        SIXTY_SECONDS_JERKY = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build()));
        return SIXTY_SECONDS_JERKY;
    });

    public static Item SIXTY_SECONDS_INSTANT_NOODLES;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_INSTANT_NOODLES = ITEMS.register("sixty_seconds_instant_noodles", () -> {
        SIXTY_SECONDS_INSTANT_NOODLES = new Item(new Item.Properties().stacksTo(4).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationModifier(0.6F).build()));
        return SIXTY_SECONDS_INSTANT_NOODLES;
    });

    public static Item SIXTY_SECONDS_CHOCOLATE_BAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHOCOLATE_BAR = ITEMS.register("sixty_seconds_chocolate_bar", () -> {
        SIXTY_SECONDS_CHOCOLATE_BAR = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4F).fast().build()));
        return SIXTY_SECONDS_CHOCOLATE_BAR;
    });

    public static Item SIXTY_SECONDS_ENERGY_BAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ENERGY_BAR = ITEMS.register("sixty_seconds_energy_bar", () -> {
        SIXTY_SECONDS_ENERGY_BAR = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.7F).fast().build()));
        return SIXTY_SECONDS_ENERGY_BAR;
    });

    public static Item SIXTY_SECONDS_SEEDS_PACK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SEEDS_PACK = ITEMS.register("sixty_seconds_seeds_pack", () -> {
        SIXTY_SECONDS_SEEDS_PACK = new Item(new Item.Properties().stacksTo(16).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).fast().build()));
        return SIXTY_SECONDS_SEEDS_PACK;
    });

    // ── 海洋食材：生/熟鲨鱼肉排、生/熟触手肉 ─────────────────────────
    public static Item SIXTY_SECONDS_RAW_SHARK_STEAK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RAW_SHARK_STEAK = ITEMS.register("sixty_seconds_raw_shark_steak", () -> {
        SIXTY_SECONDS_RAW_SHARK_STEAK = new Item(new Item.Properties().stacksTo(16).food(
                new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
        return SIXTY_SECONDS_RAW_SHARK_STEAK;
    });

    public static Item SIXTY_SECONDS_COOKED_SHARK_STEAK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COOKED_SHARK_STEAK = ITEMS.register("sixty_seconds_cooked_shark_steak", () -> {
        SIXTY_SECONDS_COOKED_SHARK_STEAK = new Item(new Item.Properties().stacksTo(16).food(
                new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(0.8F).build()));
        return SIXTY_SECONDS_COOKED_SHARK_STEAK;
    });

    public static Item SIXTY_SECONDS_RAW_TENTACLE_MEAT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RAW_TENTACLE_MEAT = ITEMS.register("sixty_seconds_raw_tentacle_meat", () -> {
        SIXTY_SECONDS_RAW_TENTACLE_MEAT = new Item(new Item.Properties().stacksTo(16).food(
                new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
        return SIXTY_SECONDS_RAW_TENTACLE_MEAT;
    });

    public static Item SIXTY_SECONDS_COOKED_TENTACLE_MEAT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COOKED_TENTACLE_MEAT = ITEMS.register("sixty_seconds_cooked_tentacle_meat", () -> {
        SIXTY_SECONDS_COOKED_TENTACLE_MEAT = new Item(new Item.Properties().stacksTo(16).food(
                new net.minecraft.world.food.FoodProperties.Builder().nutrition(12).saturationModifier(0.9F).build()));
        return SIXTY_SECONDS_COOKED_TENTACLE_MEAT;
    });

    public static Item SIXTY_SECONDS_WATER_PACK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WATER_PACK = ITEMS.register("sixty_seconds_water_pack", () -> {
        SIXTY_SECONDS_WATER_PACK = new net.exmo.sixty_seconds.content.item.SixtySecondsWaterItem(
                    new Item.Properties().stacksTo(4), "pack", 45);
        return SIXTY_SECONDS_WATER_PACK;
    });

    public static Item SIXTY_SECONDS_JUICE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_JUICE = ITEMS.register("sixty_seconds_juice", () -> {
        SIXTY_SECONDS_JUICE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 10, 25, 0, 0, false, null,
                    50, UseAnim.DRINK);
        return SIXTY_SECONDS_JUICE;
    });

    public static Item SIXTY_SECONDS_COFFEE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COFFEE = ITEMS.register("sixty_seconds_coffee", () -> {
        SIXTY_SECONDS_COFFEE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 0, 10, 10, 0, false, null,
                    40, UseAnim.DRINK);
        return SIXTY_SECONDS_COFFEE;
    });

    public static Item SIXTY_SECONDS_SEDATIVE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SEDATIVE = ITEMS.register("sixty_seconds_sedative", () -> {
        SIXTY_SECONDS_SEDATIVE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 0, 30, 0, false, null,
                    60, UseAnim.DRINK);
        return SIXTY_SECONDS_SEDATIVE;
    });

    public static Item SIXTY_SECONDS_ANTIBIOTICS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ANTIBIOTICS = ITEMS.register("sixty_seconds_antibiotics", () -> {
        SIXTY_SECONDS_ANTIBIOTICS = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 10, 0, 0, 0, 0, true, null,
                    140, UseAnim.EAT);
        return SIXTY_SECONDS_ANTIBIOTICS;
    });

    public static Item SIXTY_SECONDS_PAINKILLERS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PAINKILLERS = ITEMS.register("sixty_seconds_painkillers", () -> {
        SIXTY_SECONDS_PAINKILLERS = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 15, 0, 0, 0, 0, false, null,
                    100, UseAnim.EAT);
        return SIXTY_SECONDS_PAINKILLERS;
    });

    public static Item SIXTY_SECONDS_PURIFICATION_TABLET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PURIFICATION_TABLET = ITEMS.register("sixty_seconds_purification_tablet", () -> {
        SIXTY_SECONDS_PURIFICATION_TABLET = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 0, 0, 40, false, null,
                    60, UseAnim.EAT);
        return SIXTY_SECONDS_PURIFICATION_TABLET;
    });

    public static Item SIXTY_SECONDS_VITAMIN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_VITAMIN = ITEMS.register("sixty_seconds_vitamin", () -> {
        SIXTY_SECONDS_VITAMIN = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 10, 10, 5, 0, false, null,
                    50, UseAnim.EAT);
        return SIXTY_SECONDS_VITAMIN;
    });

    public static Item SIXTY_SECONDS_CHARCOAL_PILL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHARCOAL_PILL = ITEMS.register("sixty_seconds_charcoal_pill", () -> {
        SIXTY_SECONDS_CHARCOAL_PILL = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(16), 0, 0, 0, 0, 20, false, null,
                    40, UseAnim.EAT);
        return SIXTY_SECONDS_CHARCOAL_PILL;
    });

    public static Item SIXTY_SECONDS_DETOX_TEA;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DETOX_TEA = ITEMS.register("sixty_seconds_detox_tea", () -> {
        SIXTY_SECONDS_DETOX_TEA = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 0, 8, 30, false, null,
                    50, UseAnim.DRINK);
        return SIXTY_SECONDS_DETOX_TEA;
    });

    public static Item SIXTY_SECONDS_PURIFIED_WATER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PURIFIED_WATER = ITEMS.register("sixty_seconds_purified_water", () -> {
        SIXTY_SECONDS_PURIFIED_WATER = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 35, 0, 20, false, null,
                    40, UseAnim.DRINK);
        return SIXTY_SECONDS_PURIFIED_WATER;
    });

    public static Item SIXTY_SECONDS_MEDKIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MEDKIT = ITEMS.register("sixty_seconds_medkit", () -> {
        SIXTY_SECONDS_MEDKIT = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 60, 0, 0, 0, 0, true, null,
                    80, UseAnim.BOW);
        return SIXTY_SECONDS_MEDKIT;
    });

    public static Item SIXTY_SECONDS_ADRENALINE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ADRENALINE = ITEMS.register("sixty_seconds_adrenaline", () -> {
        SIXTY_SECONDS_ADRENALINE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 5, 0, 0, 0, 0, false,
                    () -> new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20 * 30, 1, false, false, true),
                    60, UseAnim.DRINK);
        return SIXTY_SECONDS_ADRENALINE;
    });

    public static Item SIXTY_SECONDS_KNIFE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_KNIFE = ITEMS.register("sixty_seconds_knife", () -> {
        SIXTY_SECONDS_KNIFE = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(30)
                            .attributes(SwordItem.createAttributes(Tiers.STONE, 4, -2.0F)), 18);
        return SIXTY_SECONDS_KNIFE;
    });

    public static Item SIXTY_SECONDS_SLEDGEHAMMER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SLEDGEHAMMER = ITEMS.register("sixty_seconds_sledgehammer", () -> {
        SIXTY_SECONDS_SLEDGEHAMMER = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(66)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 11, -3.0F)), 41);
        return SIXTY_SECONDS_SLEDGEHAMMER;
    });

    public static Item SIXTY_SECONDS_CHAINSAW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHAINSAW = ITEMS.register("sixty_seconds_chainsaw", () -> {
        SIXTY_SECONDS_CHAINSAW = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(72)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 14, -2.8F)), 50);
        return SIXTY_SECONDS_CHAINSAW;
    });

    public static Item SIXTY_SECONDS_FIRE_AXE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FIRE_AXE = ITEMS.register("sixty_seconds_fire_axe", () -> {
        SIXTY_SECONDS_FIRE_AXE = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(54)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 8, -2.8F)), 32);
        return SIXTY_SECONDS_FIRE_AXE;
    });

    public static Item SIXTY_SECONDS_STUN_BATON;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STUN_BATON = ITEMS.register("sixty_seconds_stun_baton", () -> {
        SIXTY_SECONDS_STUN_BATON = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(36)
                            .attributes(SwordItem.createAttributes(Tiers.STONE, 3, -2.0F)), 14, 20 * 3);
        return SIXTY_SECONDS_STUN_BATON;
    });

    public static Item SIXTY_SECONDS_MOLOTOV;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MOLOTOV = ITEMS.register("sixty_seconds_molotov", () -> {
        SIXTY_SECONDS_MOLOTOV = new net.exmo.sixty_seconds.content.item.SixtySecondsGrenadeItem(
                    new Item.Properties().stacksTo(4), 3.0D, 15.0F, 0, true, false);
        return SIXTY_SECONDS_MOLOTOV;
    });

    public static Item SIXTY_SECONDS_PIPE_BOMB;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PIPE_BOMB = ITEMS.register("sixty_seconds_pipe_bomb", () -> {
        SIXTY_SECONDS_PIPE_BOMB = new net.exmo.sixty_seconds.content.item.SixtySecondsGrenadeItem(
                    new Item.Properties().stacksTo(4), 4.0D, 40.0F, 30, false, false);
        return SIXTY_SECONDS_PIPE_BOMB;
    });

    public static Item SIXTY_SECONDS_FLASHBANG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FLASHBANG = ITEMS.register("sixty_seconds_flashbang", () -> {
        SIXTY_SECONDS_FLASHBANG = new net.exmo.sixty_seconds.content.item.SixtySecondsGrenadeItem(
                    new Item.Properties().stacksTo(4), 5.0D, 5.0F, 0, false, true);
        return SIXTY_SECONDS_FLASHBANG;
    });

    public static Item SIXTY_SECONDS_SCRAP_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP_LEGGINGS = ITEMS.register("sixty_seconds_scrap_leggings", () -> {
        SIXTY_SECONDS_SCRAP_LEGGINGS = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_SCRAP_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_SCRAP_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP_BOOTS = ITEMS.register("sixty_seconds_scrap_boots", () -> {
        SIXTY_SECONDS_SCRAP_BOOTS = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_SCRAP_BOOTS;
    });

    public static Item SIXTY_SECONDS_IRON_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_LEGGINGS = ITEMS.register("sixty_seconds_iron_leggings", () -> {
        SIXTY_SECONDS_IRON_LEGGINGS = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_IRON_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_IRON_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_BOOTS = ITEMS.register("sixty_seconds_iron_boots", () -> {
        SIXTY_SECONDS_IRON_BOOTS = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_IRON_BOOTS;
    });

    public static Item SIXTY_SECONDS_GAS_MASK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GAS_MASK = ITEMS.register("sixty_seconds_gas_mask", () -> {
        SIXTY_SECONDS_GAS_MASK = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_GAS_MASK;
    });

    public static Item SIXTY_SECONDS_HAZMAT_SUIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HAZMAT_SUIT = ITEMS.register("sixty_seconds_hazmat_suit", () -> {
        SIXTY_SECONDS_HAZMAT_SUIT = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_HAZMAT_SUIT;
    });

    public static Item SIXTY_SECONDS_NIGHT_GOGGLES;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NIGHT_GOGGLES = ITEMS.register("sixty_seconds_night_goggles", () -> {
        SIXTY_SECONDS_NIGHT_GOGGLES = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_NIGHT_GOGGLES;
    });

    public static Item SIXTY_SECONDS_RIOT_SHIELD;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RIOT_SHIELD = ITEMS.register("sixty_seconds_riot_shield", () -> {
        SIXTY_SECONDS_RIOT_SHIELD = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_RIOT_SHIELD;
    });

    public static Item SIXTY_SECONDS_RADIO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RADIO = ITEMS.register("sixty_seconds_radio", () -> {
        SIXTY_SECONDS_RADIO = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(1),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.RADIO);
        return SIXTY_SECONDS_RADIO;
    });

    public static Item SIXTY_SECONDS_COMPASS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COMPASS = ITEMS.register("sixty_seconds_compass", () -> {
        SIXTY_SECONDS_COMPASS = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(1),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.COMPASS);
        return SIXTY_SECONDS_COMPASS;
    });

    public static Item SIXTY_SECONDS_REPAIR_KIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_REPAIR_KIT = ITEMS.register("sixty_seconds_repair_kit", () -> {
        SIXTY_SECONDS_REPAIR_KIT = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_REPAIR_KIT;
    });

    public static Item SIXTY_SECONDS_SOLAR_PANEL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SOLAR_PANEL = ITEMS.register("sixty_seconds_solar_panel", () -> {
        SIXTY_SECONDS_SOLAR_PANEL = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_SOLAR_PANEL;
    });

    public static Item SIXTY_SECONDS_ALARM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALARM = ITEMS.register("sixty_seconds_alarm", () -> {
        SIXTY_SECONDS_ALARM = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(4),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.ALARM);
        return SIXTY_SECONDS_ALARM;
    });

    public static Item SIXTY_SECONDS_LURE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LURE = ITEMS.register("sixty_seconds_lure", () -> {
        SIXTY_SECONDS_LURE = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(4),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.LURE);
        return SIXTY_SECONDS_LURE;
    });

    public static Item SIXTY_SECONDS_TOOLBOX;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TOOLBOX = ITEMS.register("sixty_seconds_toolbox", () -> {
        SIXTY_SECONDS_TOOLBOX = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(4),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.TOOLBOX);
        return SIXTY_SECONDS_TOOLBOX;
    });

    public static Item SIXTY_SECONDS_BLUEPRINT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BLUEPRINT = ITEMS.register("sixty_seconds_blueprint", () -> {
        SIXTY_SECONDS_BLUEPRINT = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(4),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.BLUEPRINT);
        return SIXTY_SECONDS_BLUEPRINT;
    });

    public static Item SIXTY_SECONDS_CROWBAR_STEEL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CROWBAR_STEEL = ITEMS.register("sixty_seconds_crowbar_steel", () -> {
        SIXTY_SECONDS_CROWBAR_STEEL = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), true, 2);
        return SIXTY_SECONDS_CROWBAR_STEEL;
    });

    public static Item SIXTY_SECONDS_CROWBAR_HYDRAULIC;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CROWBAR_HYDRAULIC = ITEMS.register("sixty_seconds_crowbar_hydraulic", () -> {
        SIXTY_SECONDS_CROWBAR_HYDRAULIC = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), true, 3);
        return SIXTY_SECONDS_CROWBAR_HYDRAULIC;
    });

    public static Item SIXTY_SECONDS_LOCKPICK_PRO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LOCKPICK_PRO = ITEMS.register("sixty_seconds_lockpick_pro", () -> {
        SIXTY_SECONDS_LOCKPICK_PRO = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), false, 3);
        return SIXTY_SECONDS_LOCKPICK_PRO;
    });

    public static Item SIXTY_SECONDS_LOCKPICK_MASTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LOCKPICK_MASTER = ITEMS.register("sixty_seconds_lockpick_master", () -> {
        SIXTY_SECONDS_LOCKPICK_MASTER = new net.exmo.sixty_seconds.content.item.SixtySecondsBreakInItem(
                    new Item.Properties().stacksTo(1), false, 4);
        return SIXTY_SECONDS_LOCKPICK_MASTER;
    });

    public static Item SIXTY_SECONDS_STEEL_INGOT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_INGOT = ITEMS.register("sixty_seconds_steel_ingot", () -> {
        SIXTY_SECONDS_STEEL_INGOT = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_STEEL_INGOT;
    });

    public static Item SIXTY_SECONDS_NAILS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NAILS = ITEMS.register("sixty_seconds_nails", () -> {
        SIXTY_SECONDS_NAILS = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_NAILS;
    });

    public static Item SIXTY_SECONDS_FERTILIZER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FERTILIZER = ITEMS.register("sixty_seconds_fertilizer", () -> {
        SIXTY_SECONDS_FERTILIZER = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_FERTILIZER;
    });

    public static Item SIXTY_SECONDS_CHARCOAL_FILTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHARCOAL_FILTER = ITEMS.register("sixty_seconds_charcoal_filter", () -> {
        SIXTY_SECONDS_CHARCOAL_FILTER = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_CHARCOAL_FILTER;
    });

    public static Item SIXTY_SECONDS_DRIED_FRUIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DRIED_FRUIT = ITEMS.register("sixty_seconds_dried_fruit", () -> {
        SIXTY_SECONDS_DRIED_FRUIT = new Item(new Item.Properties().stacksTo(16).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.4F).fast().build()));
        return SIXTY_SECONDS_DRIED_FRUIT;
    });

    public static Item SIXTY_SECONDS_TRAIL_MIX;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TRAIL_MIX = ITEMS.register("sixty_seconds_trail_mix", () -> {
        SIXTY_SECONDS_TRAIL_MIX = new Item(new Item.Properties().stacksTo(16).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build()));
        return SIXTY_SECONDS_TRAIL_MIX;
    });

    public static Item SIXTY_SECONDS_FRESH_VEGETABLES;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FRESH_VEGETABLES = ITEMS.register("sixty_seconds_fresh_vegetables", () -> {
        SIXTY_SECONDS_FRESH_VEGETABLES = new Item(new Item.Properties().stacksTo(16).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.5F).build()));
        return SIXTY_SECONDS_FRESH_VEGETABLES;
    });

    public static Item SIXTY_SECONDS_BAND_AID;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BAND_AID = ITEMS.register("sixty_seconds_band_aid", () -> {
        SIXTY_SECONDS_BAND_AID = new net.exmo.sixty_seconds.content.item.SixtySecondsBandageItem(
                    new Item.Properties().stacksTo(16), 10, 10);
        return SIXTY_SECONDS_BAND_AID;
    });

    public static Item SIXTY_SECONDS_BAMBOO_RICE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BAMBOO_RICE = ITEMS.register("sixty_seconds_bamboo_rice", () -> {
        SIXTY_SECONDS_BAMBOO_RICE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 40, 70, 0, 0, 0, false, null,
                    100, net.minecraft.world.item.UseAnim.EAT);
        return SIXTY_SECONDS_BAMBOO_RICE;
    });

    public static Item SIXTY_SECONDS_SHIMMER_BERRY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SHIMMER_BERRY = ITEMS.register("sixty_seconds_shimmer_berry", () -> {
        SIXTY_SECONDS_SHIMMER_BERRY = new Item(new Item.Properties().stacksTo(64).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).alwaysEdible().build()));
        return SIXTY_SECONDS_SHIMMER_BERRY;
    });

    public static Item SIXTY_SECONDS_BAGGED_DRIED_FRUIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BAGGED_DRIED_FRUIT = ITEMS.register("sixty_seconds_bagged_dried_fruit", () -> {
        SIXTY_SECONDS_BAGGED_DRIED_FRUIT = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(12).saturationModifier(0.8F).build()));
        return SIXTY_SECONDS_BAGGED_DRIED_FRUIT;
    });

    public static Item SIXTY_SECONDS_BAGGED_BISCUIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BAGGED_BISCUIT = ITEMS.register("sixty_seconds_bagged_biscuit", () -> {
        SIXTY_SECONDS_BAGGED_BISCUIT = new Item(new Item.Properties().stacksTo(8).food(
                    new net.minecraft.world.food.FoodProperties.Builder().nutrition(12).saturationModifier(0.8F).build()));
        return SIXTY_SECONDS_BAGGED_BISCUIT;
    });

    public static Item SIXTY_SECONDS_DRAFT_PAPER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DRAFT_PAPER = ITEMS.register("sixty_seconds_draft_paper", () -> {
        SIXTY_SECONDS_DRAFT_PAPER = new net.exmo.sixty_seconds.content.item.SixtySecondsNoteItem(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_DRAFT_PAPER;
    });

    public static Item SIXTY_SECONDS_PHONE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PHONE = ITEMS.register("sixty_seconds_phone", () -> {
        SIXTY_SECONDS_PHONE = new SixtySecondsPhoneItem(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_PHONE;
    });

    public static Item SIXTY_SECONDS_EXPRESS_PACKAGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EXPRESS_PACKAGE = ITEMS.register("sixty_seconds_express_package", () -> {
        SIXTY_SECONDS_EXPRESS_PACKAGE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_EXPRESS_PACKAGE;
    });

    public static Item SIXTY_SECONDS_CANNED_SOUP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CANNED_SOUP = ITEMS.register("sixty_seconds_canned_soup", () -> {
        SIXTY_SECONDS_CANNED_SOUP = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 20, 20, 0, 0, false, null);
        return SIXTY_SECONDS_CANNED_SOUP;
    });

    public static Item SIXTY_SECONDS_SPORTS_DRINK;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SPORTS_DRINK = ITEMS.register("sixty_seconds_sports_drink", () -> {
        SIXTY_SECONDS_SPORTS_DRINK = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 30, 5, 0, false,
                    () -> new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20 * 20, 0, false, false, true));
        return SIXTY_SECONDS_SPORTS_DRINK;
    });

    public static Item SIXTY_SECONDS_HERBAL_TEA;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HERBAL_TEA = ITEMS.register("sixty_seconds_herbal_tea", () -> {
        SIXTY_SECONDS_HERBAL_TEA = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 15, 20, 0, true, null);
        return SIXTY_SECONDS_HERBAL_TEA;
    });

    public static Item SIXTY_SECONDS_BLOOD_BAG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BLOOD_BAG = ITEMS.register("sixty_seconds_blood_bag", () -> {
        SIXTY_SECONDS_BLOOD_BAG = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 45, 0, 0, 0, 0, false, null);
        return SIXTY_SECONDS_BLOOD_BAG;
    });

    public static Item SIXTY_SECONDS_ANTI_POLLUTION_SERUM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ANTI_POLLUTION_SERUM = ITEMS.register("sixty_seconds_anti_pollution_serum", () -> {
        SIXTY_SECONDS_ANTI_POLLUTION_SERUM = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 0, 0, 60, true, null);
        return SIXTY_SECONDS_ANTI_POLLUTION_SERUM;
    });

    public static Item SIXTY_SECONDS_HATCHET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HATCHET = ITEMS.register("sixty_seconds_hatchet", () -> {
        SIXTY_SECONDS_HATCHET = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().stacksTo(1).durability(36)
                            .attributes(SwordItem.createAttributes(Tiers.STONE, 6, -2.2F)), 23);
        return SIXTY_SECONDS_HATCHET;
    });

    public static Item SIXTY_SECONDS_CLEAVER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CLEAVER = ITEMS.register("sixty_seconds_cleaver", () -> {
        SIXTY_SECONDS_CLEAVER = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().stacksTo(1).durability(45)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 8, -2.4F)), 32);
        return SIXTY_SECONDS_CLEAVER;
    });

    public static Item SIXTY_SECONDS_STEEL_SWORD;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_SWORD = ITEMS.register("sixty_seconds_steel_sword", () -> {
        SIXTY_SECONDS_STEEL_SWORD = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().stacksTo(1).durability(72)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 12, -2.4F)), 45);
        return SIXTY_SECONDS_STEEL_SWORD;
    });

    public static Item SIXTY_SECONDS_STEEL_SPEAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_SPEAR = ITEMS.register("sixty_seconds_steel_spear", () -> {
        SIXTY_SECONDS_STEEL_SPEAR = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().stacksTo(1).durability(60)
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 11, -2.8F)), 41, 30);
        return SIXTY_SECONDS_STEEL_SPEAR;
    });

    /** 重锤属性：按档位设置基础伤害 / 攻速 / 耐久。铁质偏低、钢质等同原版重锤、合金更高更快，
     *  与物品描述一致；此外「重砸额外伤害/击退/破盾概率」也随档位提升。 */
    private static Item.Properties maceProperties(int maxDamage, float attackDamage, double attackSpeed) {
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
        return new Item.Properties().stacksTo(1).durability(maxDamage).attributes(attributes);
    }

    public static Item SIXTY_SECONDS_IRON_MACE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_IRON_MACE = ITEMS.register("iron_mace", () -> {
        SIXTY_SECONDS_IRON_MACE = new net.exmo.sixty_seconds.content.item.SixtySecondsMaceItem(
                50, 0.6F, 0.6F, 0.15F, maceProperties(50, 4.0F, -3.6D));
        return SIXTY_SECONDS_IRON_MACE;
    });

    public static Item SIXTY_SECONDS_STEEL_MACE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_MACE = ITEMS.register("steel_mace", () -> {
        SIXTY_SECONDS_STEEL_MACE = new net.exmo.sixty_seconds.content.item.SixtySecondsMaceItem(
                150, 1.0F, 1.0F, 0.3F, maceProperties(150, 5.0F, -3.4D));
        return SIXTY_SECONDS_STEEL_MACE;
    });

    public static Item SIXTY_SECONDS_ALLOY_MACE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_MACE = ITEMS.register("alloy_mace", () -> {
        SIXTY_SECONDS_ALLOY_MACE = new net.exmo.sixty_seconds.content.item.SixtySecondsMaceItem(
                400, 1.5F, 1.5F, 0.5F, maceProperties(400, 6.5F, -3.2D));
        return SIXTY_SECONDS_ALLOY_MACE;
    });

    public static Item SIXTY_SECONDS_INCENDIARY_GRENADE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_INCENDIARY_GRENADE = ITEMS.register("sixty_seconds_incendiary_grenade", () -> {
        SIXTY_SECONDS_INCENDIARY_GRENADE = new net.exmo.sixty_seconds.content.item.SixtySecondsGrenadeItem(
                    new Item.Properties().stacksTo(4), 4.0D, 25.0F, 20, true, false);
        return SIXTY_SECONDS_INCENDIARY_GRENADE;
    });

    public static Item SIXTY_SECONDS_FRAG_GRENADE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FRAG_GRENADE = ITEMS.register("sixty_seconds_frag_grenade", () -> {
        SIXTY_SECONDS_FRAG_GRENADE = new net.exmo.sixty_seconds.content.item.SixtySecondsGrenadeItem(
                    new Item.Properties().stacksTo(4), 3.5D, 30.0F, 35, false, false);
        return SIXTY_SECONDS_FRAG_GRENADE;
    });

    public static Item SIXTY_SECONDS_THERMOS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_THERMOS = ITEMS.register("sixty_seconds_thermos", () -> {
        SIXTY_SECONDS_THERMOS = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 0, 50, 3, 0, false, null);
        return SIXTY_SECONDS_THERMOS;
    });

    public static Item SIXTY_SECONDS_STEW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEW = ITEMS.register("sixty_seconds_stew", () -> {
        SIXTY_SECONDS_STEW = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 100, 60, 5, 0, false, null);
        return SIXTY_SECONDS_STEW;
    });

    public static Item SIXTY_SECONDS_STEEL_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_HELMET = ITEMS.register("sixty_seconds_steel_helmet", () -> {
        SIXTY_SECONDS_STEEL_HELMET = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_STEEL_HELMET;
    });

    public static Item SIXTY_SECONDS_STEEL_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_CHESTPLATE = ITEMS.register("sixty_seconds_steel_chestplate", () -> {
        SIXTY_SECONDS_STEEL_CHESTPLATE = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_STEEL_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_STEEL_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_LEGGINGS = ITEMS.register("sixty_seconds_steel_leggings", () -> {
        SIXTY_SECONDS_STEEL_LEGGINGS = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_STEEL_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_STEEL_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_STEEL_BOOTS = ITEMS.register("sixty_seconds_steel_boots", () -> {
        SIXTY_SECONDS_STEEL_BOOTS = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_STEEL_BOOTS;
    });

    // 潜水服（穿戴材质继承原版钻石装备，防护能力与钢套相当；全套触发水下呼吸，见 NeoForgeEvents）
    public static Item SIXTY_SECONDS_DIVING_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIVING_HELMET = ITEMS.register("sixty_seconds_diving_helmet", () -> {
        SIXTY_SECONDS_DIVING_HELMET = new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant());
        return SIXTY_SECONDS_DIVING_HELMET;
    });

    public static Item SIXTY_SECONDS_DIVING_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIVING_CHESTPLATE = ITEMS.register("sixty_seconds_diving_chestplate", () -> {
        SIXTY_SECONDS_DIVING_CHESTPLATE = new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1).fireResistant());
        return SIXTY_SECONDS_DIVING_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_DIVING_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIVING_LEGGINGS = ITEMS.register("sixty_seconds_diving_leggings", () -> {
        SIXTY_SECONDS_DIVING_LEGGINGS = new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1).fireResistant());
        return SIXTY_SECONDS_DIVING_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_DIVING_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIVING_BOOTS = ITEMS.register("sixty_seconds_diving_boots", () -> {
        SIXTY_SECONDS_DIVING_BOOTS = new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1).fireResistant());
        return SIXTY_SECONDS_DIVING_BOOTS;
    });

    public static Item SIXTY_SECONDS_BALLISTIC_VEST;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BALLISTIC_VEST = ITEMS.register("sixty_seconds_ballistic_vest", () -> {
        SIXTY_SECONDS_BALLISTIC_VEST = new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_BALLISTIC_VEST;
    });

    public static Item SIXTY_SECONDS_SCRAP_METAL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SCRAP_METAL = ITEMS.register("sixty_seconds_scrap_metal", () -> {
        SIXTY_SECONDS_SCRAP_METAL = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_SCRAP_METAL;
    });

    public static Item SIXTY_SECONDS_PRECIOUS_PARTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PRECIOUS_PARTS = ITEMS.register("sixty_seconds_precious_parts", () -> {
        SIXTY_SECONDS_PRECIOUS_PARTS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_PRECIOUS_PARTS;
    });

    public static Item SIXTY_SECONDS_BREWING_PARTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BREWING_PARTS = ITEMS.register("sixty_seconds_brewing_parts", () -> {
        SIXTY_SECONDS_BREWING_PARTS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_BREWING_PARTS;
    });

    public static Item SIXTY_SECONDS_FLARE_GUN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FLARE_GUN = ITEMS.register("sixty_seconds_flare_gun", () -> {
        SIXTY_SECONDS_FLARE_GUN = new net.exmo.sixty_seconds.content.item.SixtySecondsFlareGunItem(
                    new Item.Properties().stacksTo(1).durability(1));
        return SIXTY_SECONDS_FLARE_GUN;
    });

    public static Item SIXTY_SECONDS_COPPER_SCRAP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COPPER_SCRAP = ITEMS.register("sixty_seconds_copper_scrap", () -> {
        SIXTY_SECONDS_COPPER_SCRAP = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_COPPER_SCRAP;
    });

    public static Item SIXTY_SECONDS_GLASS_PLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_GLASS_PLATE = ITEMS.register("sixty_seconds_glass_plate", () -> {
        SIXTY_SECONDS_GLASS_PLATE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_GLASS_PLATE;
    });

    public static Item SIXTY_SECONDS_PRECIOUS_METAL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PRECIOUS_METAL = ITEMS.register("sixty_seconds_precious_metal", () -> {
        SIXTY_SECONDS_PRECIOUS_METAL = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_PRECIOUS_METAL;
    });

    public static Item SIXTY_SECONDS_ALLOY_PLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_PLATE = ITEMS.register("sixty_seconds_alloy_plate", () -> {
        SIXTY_SECONDS_ALLOY_PLATE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_ALLOY_PLATE;
    });

    public static Item SIXTY_SECONDS_PORTABLE_BATTERY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PORTABLE_BATTERY = ITEMS.register("sixty_seconds_portable_battery", () -> {
        SIXTY_SECONDS_PORTABLE_BATTERY = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_PORTABLE_BATTERY;
    });

    public static Item SIXTY_SECONDS_WILD_RICE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WILD_RICE = ITEMS.register("sixty_seconds_wild_rice", () -> {
        SIXTY_SECONDS_WILD_RICE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_WILD_RICE;
    });

    public static Item SIXTY_SECONDS_WILD_RICE_SEEDS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WILD_RICE_SEEDS = ITEMS.register("sixty_seconds_wild_rice_seeds", () -> {
        SIXTY_SECONDS_WILD_RICE_SEEDS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_WILD_RICE_SEEDS;
    });

    public static Item SIXTY_SECONDS_WILD_TEA_LEAF;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WILD_TEA_LEAF = ITEMS.register("sixty_seconds_wild_tea_leaf", () -> {
        SIXTY_SECONDS_WILD_TEA_LEAF = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_WILD_TEA_LEAF;
    });

    public static Item SIXTY_SECONDS_WILD_TEA_SEED;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WILD_TEA_SEED = ITEMS.register("sixty_seconds_wild_tea_seed", () -> {
        SIXTY_SECONDS_WILD_TEA_SEED = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_WILD_TEA_SEED;
    });

    public static Item SIXTY_SECONDS_HEMP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HEMP = ITEMS.register("sixty_seconds_hemp", () -> {
        SIXTY_SECONDS_HEMP = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_HEMP;
    });

    public static Item SIXTY_SECONDS_HEMP_SEEDS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HEMP_SEEDS = ITEMS.register("sixty_seconds_hemp_seeds", () -> {
        SIXTY_SECONDS_HEMP_SEEDS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_HEMP_SEEDS;
    });

    public static Item SIXTY_SECONDS_TOBACCO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TOBACCO = ITEMS.register("sixty_seconds_tobacco", () -> {
        SIXTY_SECONDS_TOBACCO = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_TOBACCO;
    });

    public static Item SIXTY_SECONDS_CIGARETTE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CIGARETTE = ITEMS.register("sixty_seconds_cigarette", () -> {
        SIXTY_SECONDS_CIGARETTE = new net.exmo.sixty_seconds.content.item.SixtySecondsCigaretteItem(
                new Item.Properties().stacksTo(1).durability(3), 10, 12);
        return SIXTY_SECONDS_CIGARETTE;
    });

    public static Item SIXTY_SECONDS_CIGAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CIGAR = ITEMS.register("sixty_seconds_cigar", () -> {
        SIXTY_SECONDS_CIGAR = new net.exmo.sixty_seconds.content.item.SixtySecondsCigaretteItem(
                new Item.Properties().stacksTo(1).durability(5), 5, 18);
        return SIXTY_SECONDS_CIGAR;
    });

    public static Item SIXTY_SECONDS_TOBACCO_SEEDS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_TOBACCO_SEEDS = ITEMS.register("sixty_seconds_tobacco_seeds", () -> {
        SIXTY_SECONDS_TOBACCO_SEEDS = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_TOBACCO_SEEDS;
    });

    public static Item SIXTY_SECONDS_NUTRIENT_FERTILIZER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NUTRIENT_FERTILIZER = ITEMS.register("sixty_seconds_nutrient_fertilizer", () -> {
        SIXTY_SECONDS_NUTRIENT_FERTILIZER = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_NUTRIENT_FERTILIZER;
    });

    public static Item SIXTY_SECONDS_MEDICINAL_FERN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MEDICINAL_FERN = ITEMS.register("sixty_seconds_medicinal_fern", () -> {
        SIXTY_SECONDS_MEDICINAL_FERN = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(16), 20, 0, 0, 0, 5, false, null,
                    40, net.minecraft.world.item.UseAnim.EAT);
        return SIXTY_SECONDS_MEDICINAL_FERN;
    });

    public static Item SIXTY_SECONDS_NOTE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NOTE = ITEMS.register("sixty_seconds_note", () -> {
        SIXTY_SECONDS_NOTE = new net.exmo.sixty_seconds.content.item.SixtySecondsNoteItem(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_NOTE;
    });

    // ── 挖掘工具（挖掘工具科技线，与工具-I 同级）────────────────────────
    // 材质继承原版铁镐 / 铁锹；采掘等级=铁、耐久=铁(250)。
    // 可采掘方块分两层声明：
    //   1. minecraft:tool 组件 —— 生存/创造模式下的挖掘速度与掉落（correct_for_drops + 铁镐速度 6.0）。
    //   2. minecraft:can_break 组件 —— 冒险模式下允许破坏的方块（原版 CanDestroy 的继任者）。
    //      两者互不替代：冒险模式只认 can_break，所以必须单独声明，否则冒险模式挖不动。
    public static Item SIXTY_SECONDS_MINING_PICKAXE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MINING_PICKAXE = ITEMS.register("sixty_seconds_mining_pickaxe", () -> {
        List<Block> pickaxeBlocks = List.of(
                // 原基础方块
                Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.GRANITE,
                Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.NETHERRACK, Blocks.PRISMARINE,
                Blocks.BLACKSTONE, Blocks.BASALT, Blocks.CALCITE, Blocks.DEEPSLATE,
                Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
                // 玻璃（含所有染色玻璃）
                Blocks.GLASS,
                Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS,
                Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
                Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS,
                Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
                Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
                // 玻璃板（含所有染色玻璃板）
                Blocks.GLASS_PANE,
                Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE,
                Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE,
                Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
                Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
                Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE,
                // 石质建筑方块
                Blocks.STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS,
                Blocks.SMOOTH_STONE_SLAB, Blocks.TERRACOTTA,
                Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
                Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
                Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
                Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
                Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA,
                Blocks.IRON_BARS, Blocks.COBBLESTONE_WALL, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ,
                // 铺路方块 / 铺路灯笼（本模组独立方块）
                ModBlocks.HOLD_SIXTY_SECONDS_PAVING_BLOCK.get(), ModBlocks.HOLD_SIXTY_SECONDS_LANTERN.get(),
                // 门：铁门 + 所有种类的木门
                Blocks.IRON_DOOR,
                Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
                Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR,
                Blocks.MANGROVE_DOOR, Blocks.CHERRY_DOOR, Blocks.BAMBOO_DOOR);
        Tool pickaxeTool = new Tool(List.of(Tool.Rule.minesAndDrops(pickaxeBlocks, 6.0F)), 1.0F, 1);
        SIXTY_SECONDS_MINING_PICKAXE = new MiningToolItem(new Item.Properties().durability(250)
                .component(DataComponents.TOOL, pickaxeTool)
                .component(DataComponents.CAN_BREAK, toAdventurePredicate(pickaxeBlocks)));
        return SIXTY_SECONDS_MINING_PICKAXE;
    });

    public static Item SIXTY_SECONDS_MINING_SHOVEL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MINING_SHOVEL = ITEMS.register("sixty_seconds_mining_shovel", () -> {
        List<Block> shovelBlocks = List.of(
                // 原基础方块
                Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.SNOW, Blocks.SNOW_BLOCK,
                Blocks.POWDER_SNOW, Blocks.GRASS_BLOCK, Blocks.MYCELIUM, Blocks.PODZOL,
                Blocks.COARSE_DIRT, Blocks.MUD, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.DIRT_PATH,
                // 新增：蕨、大型蕨、苔藓块、苔藓地毯
                Blocks.FERN, Blocks.LARGE_FERN, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET);
        Tool shovelTool = new Tool(List.of(Tool.Rule.minesAndDrops(shovelBlocks, 6.0F)), 1.0F, 1);
        SIXTY_SECONDS_MINING_SHOVEL = new MiningToolItem(new Item.Properties().durability(250)
                .component(DataComponents.TOOL, shovelTool)
                .component(DataComponents.CAN_BREAK, toAdventurePredicate(shovelBlocks)));
        return SIXTY_SECONDS_MINING_SHOVEL;
    });

    // 采掘剪刀：通电制作（2 铁）。可挖掘所有树叶、藤蔓、蜘蛛网（冒险模式 can_break）。
    public static Item SIXTY_SECONDS_MINING_SHEARS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MINING_SHEARS = ITEMS.register("sixty_seconds_mining_shears", () -> {
        List<Block> shearsBlocks = List.of(
                Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES,
                Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
                Blocks.VINE, Blocks.WEEPING_VINES, Blocks.TWISTING_VINES, Blocks.CAVE_VINES,
                Blocks.COBWEB);
        SIXTY_SECONDS_MINING_SHEARS = new MiningShearsItem(new Item.Properties().durability(238)
                .component(DataComponents.CAN_BREAK, toAdventurePredicate(shearsBlocks)));
        return SIXTY_SECONDS_MINING_SHEARS;
    });

    // 把方块列表转成冒险模式的 can_break 谓词（宽松匹配：方块任意状态都允许，如雪的任意层数）。
    private static AdventureModePredicate toAdventurePredicate(List<Block> blocks) {
        List<BlockPredicate> predicates = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            predicates.add(BlockPredicate.Builder.block().of(block).build());
        }
        return new AdventureModePredicate(predicates, false);
    }

    public static Item SIXTY_SECONDS_BIG_NOTE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BIG_NOTE = ITEMS.register("sixty_seconds_big_note", () -> {
        SIXTY_SECONDS_BIG_NOTE = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_BIG_NOTE;
    });

    public static Item SIXTY_SECONDS_MAGNET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MAGNET = ITEMS.register("sixty_seconds_magnet", () -> {
        SIXTY_SECONDS_MAGNET = new net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem(
                    new Item.Properties().stacksTo(1).durability(32),
                    net.exmo.sixty_seconds.content.item.SixtySecondsUtilityItem.Type.MAGNET);
        return SIXTY_SECONDS_MAGNET;
    });

    public static Item SIXTY_SECONDS_BOX_PRY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BOX_PRY = ITEMS.register("sixty_seconds_box_pry", () -> {
        SIXTY_SECONDS_BOX_PRY = new SixtySecondsBoxPryItem(new Item.Properties().stacksTo(1).durability(16));
        return SIXTY_SECONDS_BOX_PRY;
    });

    public static Item SIXTY_SECONDS_PLIERS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLIERS = ITEMS.register("sixty_seconds_pliers", () -> {
        SIXTY_SECONDS_PLIERS = new SixtySecondsPliersItem(new Item.Properties().stacksTo(1).durability(16));
        return SIXTY_SECONDS_PLIERS;
    });

    public static Item SIXTY_SECONDS_VAULT_PICK_KIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_VAULT_PICK_KIT = ITEMS.register("sixty_seconds_vault_pick_kit", () -> {
        SIXTY_SECONDS_VAULT_PICK_KIT = new Item(new Item.Properties().stacksTo(1).durability(16));
        return SIXTY_SECONDS_VAULT_PICK_KIT;
    });

    public static Item SIXTY_SECONDS_DETACH_WRENCH;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DETACH_WRENCH = ITEMS.register("sixty_seconds_detach_wrench", () -> {
        SIXTY_SECONDS_DETACH_WRENCH = new net.exmo.sixty_seconds.content.item.SixtySecondsWrenchItem(
                    new Item.Properties().stacksTo(1).durability(20));
        return SIXTY_SECONDS_DETACH_WRENCH;
    });

    public static Item SIXTY_SECONDS_BACKPACK_MILITARY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BACKPACK_MILITARY = ITEMS.register("sixty_seconds_backpack_military", () -> {
        SIXTY_SECONDS_BACKPACK_MILITARY = new net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem(
                    new Item.Properties().stacksTo(1), 4);
        return SIXTY_SECONDS_BACKPACK_MILITARY;
    });

    public static Item SIXTY_SECONDS_BACKPACK_TRAVELER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BACKPACK_TRAVELER = ITEMS.register("sixty_seconds_backpack_traveler", () -> {
        SIXTY_SECONDS_BACKPACK_TRAVELER = new net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem(
                    new Item.Properties().stacksTo(1), 6);
        return SIXTY_SECONDS_BACKPACK_TRAVELER;
    });

    public static Item SIXTY_SECONDS_BATTERY_LARGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BATTERY_LARGE = ITEMS.register("sixty_seconds_battery_large", () -> {
        SIXTY_SECONDS_BATTERY_LARGE = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_BATTERY_LARGE;
    });

    public static Item SIXTY_SECONDS_DOOR_LOCK_REINFORCED;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOR_LOCK_REINFORCED = ITEMS.register("sixty_seconds_door_lock_reinforced", () -> {
        SIXTY_SECONDS_DOOR_LOCK_REINFORCED = new net.exmo.sixty_seconds.content.item.SixtySecondsDoorLockItem(
                    new Item.Properties().stacksTo(4), 2);
        return SIXTY_SECONDS_DOOR_LOCK_REINFORCED;
    });

    public static Item SIXTY_SECONDS_DOOR_LOCK_ULTIMATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOR_LOCK_ULTIMATE = ITEMS.register("sixty_seconds_door_lock_ultimate", () -> {
        SIXTY_SECONDS_DOOR_LOCK_ULTIMATE = new net.exmo.sixty_seconds.content.item.SixtySecondsDoorLockItem(
                    new Item.Properties().stacksTo(4), 3);
        return SIXTY_SECONDS_DOOR_LOCK_ULTIMATE;
    });

    public static Item SIXTY_SECONDS_DOOR_LOCK_ALLOY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOR_LOCK_ALLOY = ITEMS.register("sixty_seconds_door_lock_alloy", () -> {
        SIXTY_SECONDS_DOOR_LOCK_ALLOY = new net.exmo.sixty_seconds.content.item.SixtySecondsDoorLockItem(
                    new Item.Properties().stacksTo(4), 4);
        return SIXTY_SECONDS_DOOR_LOCK_ALLOY;
    });

    public static Item SIXTY_SECONDS_RICE_SOUP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RICE_SOUP = ITEMS.register("sixty_seconds_rice_soup", () -> {
        SIXTY_SECONDS_RICE_SOUP = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 20, 15, 0, 0, false, null);
        return SIXTY_SECONDS_RICE_SOUP;
    });

    public static Item SIXTY_SECONDS_COOKED_NOODLES;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COOKED_NOODLES = ITEMS.register("sixty_seconds_cooked_noodles", () -> {
        SIXTY_SECONDS_COOKED_NOODLES = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 60, 5, 0, 0, false, null);
        return SIXTY_SECONDS_COOKED_NOODLES;
    });

    public static Item SIXTY_SECONDS_DOOMSDAY_CAKE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DOOMSDAY_CAKE = ITEMS.register("sixty_seconds_doomsday_cake", () -> {
        SIXTY_SECONDS_DOOMSDAY_CAKE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 80, 0, 40, 0, false, null);
        return SIXTY_SECONDS_DOOMSDAY_CAKE;
    });

    public static Item SIXTY_SECONDS_LUXURY_STEW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_LUXURY_STEW = ITEMS.register("sixty_seconds_luxury_stew", () -> {
        SIXTY_SECONDS_LUXURY_STEW = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 70, 50, 70, 30, true, null);
        return SIXTY_SECONDS_LUXURY_STEW;
    });

    public static Item SIXTY_SECONDS_ONE_POT_STEW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ONE_POT_STEW = ITEMS.register("sixty_seconds_one_pot_stew", () -> {
        SIXTY_SECONDS_ONE_POT_STEW = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 65, 0, 0, 25, false, null);
        return SIXTY_SECONDS_ONE_POT_STEW;
    });

    public static Item SIXTY_SECONDS_SUSHI;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SUSHI = ITEMS.register("sixty_seconds_sushi", () -> {
        SIXTY_SECONDS_SUSHI = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 65, 0, 0, 0, false, null);
        return SIXTY_SECONDS_SUSHI;
    });

    public static Item SIXTY_SECONDS_BONE_SOUP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BONE_SOUP = ITEMS.register("sixty_seconds_bone_soup", () -> {
        SIXTY_SECONDS_BONE_SOUP = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 60, 5, 0, 0, false, null);
        return SIXTY_SECONDS_BONE_SOUP;
    });

    public static Item SIXTY_SECONDS_HAIMAN_MEATBALL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HAIMAN_MEATBALL = ITEMS.register("sixty_seconds_haiman_meatball", () -> {
        SIXTY_SECONDS_HAIMAN_MEATBALL = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 75, 0, 15, 0, false, null,
                    50, net.minecraft.world.item.UseAnim.EAT);
        return SIXTY_SECONDS_HAIMAN_MEATBALL;
    });

    public static Item SIXTY_SECONDS_CATMOONCAKE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CATMOONCAKE = ITEMS.register("sixty_seconds_catmooncake", () -> {
        SIXTY_SECONDS_CATMOONCAKE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 75, 0, 0, 15, false, null,
                    50, net.minecraft.world.item.UseAnim.EAT);
        return SIXTY_SECONDS_CATMOONCAKE;
    });

    public static Item SIXTY_SECONDS_SOOTHING_TEA;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SOOTHING_TEA = ITEMS.register("sixty_seconds_soothing_tea", () -> {
        SIXTY_SECONDS_SOOTHING_TEA = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 0, 15, 25, 0, false, null,
                    50, UseAnim.DRINK);
        return SIXTY_SECONDS_SOOTHING_TEA;
    });

    public static Item SIXTY_SECONDS_SIMPLE_BANDAGE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SIMPLE_BANDAGE = ITEMS.register("sixty_seconds_simple_bandage", () -> {
        SIXTY_SECONDS_SIMPLE_BANDAGE = new net.exmo.sixty_seconds.content.item.SixtySecondsBandageItem(
                    new Item.Properties().stacksTo(16), 10);
        return SIXTY_SECONDS_SIMPLE_BANDAGE;
    });

    public static Item SIXTY_SECONDS_SPLINT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SPLINT = ITEMS.register("sixty_seconds_splint", () -> {
        SIXTY_SECONDS_SPLINT = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 5, 0, 0, 0, 0, false, null,
                    100, UseAnim.BOW);
        return SIXTY_SECONDS_SPLINT;
    });

    public static Item SIXTY_SECONDS_MEDICAL_BOX;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MEDICAL_BOX = ITEMS.register("sixty_seconds_medical_box", () -> {
        SIXTY_SECONDS_MEDICAL_BOX = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(2), 999, 0, 0, 0, 0, true, null,
                    200, UseAnim.BOW);
        return SIXTY_SECONDS_MEDICAL_BOX;
    });

    public static Item SIXTY_SECONDS_SANITY_PILL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SANITY_PILL = ITEMS.register("sixty_seconds_sanity_pill", () -> {
        SIXTY_SECONDS_SANITY_PILL = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 0, 0, 0, 10, 0, false, null,
                    40, UseAnim.EAT);
        return SIXTY_SECONDS_SANITY_PILL;
    });

    public static Item SIXTY_SECONDS_SANITY_MED;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SANITY_MED = ITEMS.register("sixty_seconds_sanity_med", () -> {
        SIXTY_SECONDS_SANITY_MED = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 0, 0, 60, 0, false, null,
                    60, UseAnim.DRINK);
        return SIXTY_SECONDS_SANITY_MED;
    });

    public static Item SIXTY_SECONDS_MENTAL_FORTIFIER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MENTAL_FORTIFIER = ITEMS.register("sixty_seconds_mental_fortifier", () -> {
        SIXTY_SECONDS_MENTAL_FORTIFIER = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(4), 0, 0, 0, 0, 0, false, null,
                    100, UseAnim.DRINK, 5);
        return SIXTY_SECONDS_MENTAL_FORTIFIER;
    });

    public static Item SIXTY_SECONDS_COGNITIVE_BOOSTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COGNITIVE_BOOSTER = ITEMS.register("sixty_seconds_cognitive_booster", () -> {
        SIXTY_SECONDS_COGNITIVE_BOOSTER = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(2), 0, 0, 0, 0, 0, false, null,
                    160, UseAnim.EAT,
                    10);
        return SIXTY_SECONDS_COGNITIVE_BOOSTER;
    });

    public static Item SIXTY_SECONDS_ANTI_INFECTION;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ANTI_INFECTION = ITEMS.register("sixty_seconds_anti_infection", () -> {
        SIXTY_SECONDS_ANTI_INFECTION = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 25, 0, 0, 0, 30, false, null,
                    100, UseAnim.EAT);
        return SIXTY_SECONDS_ANTI_INFECTION;
    });

    public static Item SIXTY_SECONDS_HEALTH_BOOSTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HEALTH_BOOSTER = ITEMS.register("sixty_seconds_health_booster", () -> {
        SIXTY_SECONDS_HEALTH_BOOSTER = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(2), 0, 0, 0, 0, 0, false, null,
                    100, UseAnim.DRINK, 0, 5, 0, 0, 0);
        return SIXTY_SECONDS_HEALTH_BOOSTER;
    });

    public static Item SIXTY_SECONDS_POLLUTION_RESISTANCE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_POLLUTION_RESISTANCE = ITEMS.register("sixty_seconds_pollution_resistance", () -> {
        SIXTY_SECONDS_POLLUTION_RESISTANCE = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(2), 0, 0, 0, 0, 0, false, null,
                    100, UseAnim.DRINK, 0, 0, 0, 0, 10);
        return SIXTY_SECONDS_POLLUTION_RESISTANCE;
    });

    public static Item SIXTY_SECONDS_NUTRIENT_BOOSTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_NUTRIENT_BOOSTER = ITEMS.register("sixty_seconds_nutrient_booster", () -> {
        SIXTY_SECONDS_NUTRIENT_BOOSTER = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(2), 0, 0, 0, 0, 0, false, null,
                    100, UseAnim.DRINK, 0, 0, 5, 5, 0);
        return SIXTY_SECONDS_NUTRIENT_BOOSTER;
    });

    public static Item SIXTY_SECONDS_OMNI_TONIC;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_OMNI_TONIC = ITEMS.register("sixty_seconds_omni_tonic", () -> {
        SIXTY_SECONDS_OMNI_TONIC = new net.exmo.sixty_seconds.content.item.SixtySecondsStatItem(
                    new Item.Properties().stacksTo(8), 15, 15, 15, 15, 15, false, null,
                    60, UseAnim.DRINK);
        return SIXTY_SECONDS_OMNI_TONIC;
    });

    public static Item SIXTY_SECONDS_POTION_CLEANSER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_POTION_CLEANSER = ITEMS.register("sixty_seconds_potion_cleanser", () -> {
        SIXTY_SECONDS_POTION_CLEANSER = new net.exmo.sixty_seconds.content.item.SixtySecondsPotionCleanserItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_POTION_CLEANSER;
    });

    public static Item SIXTY_SECONDS_RIFLE_AMMO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RIFLE_AMMO = ITEMS.register("sixty_seconds_rifle_ammo", () -> {
        SIXTY_SECONDS_RIFLE_AMMO = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_RIFLE_AMMO;
    });

    public static Item SIXTY_SECONDS_SMG_AMMO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SMG_AMMO = ITEMS.register("sixty_seconds_smg_ammo", () -> {
        SIXTY_SECONDS_SMG_AMMO = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_SMG_AMMO;
    });

    public static Item SIXTY_SECONDS_SHOTGUN_AMMO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SHOTGUN_AMMO = ITEMS.register("sixty_seconds_shotgun_ammo", () -> {
        SIXTY_SECONDS_SHOTGUN_AMMO = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_SHOTGUN_AMMO;
    });

    public static Item SIXTY_SECONDS_MAGNUM_AMMO;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MAGNUM_AMMO = ITEMS.register("sixty_seconds_magnum_ammo", () -> {
        SIXTY_SECONDS_MAGNUM_AMMO = new Item(new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_MAGNUM_AMMO;
    });

    public static Item SIXTY_SECONDS_ROCKET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ROCKET = ITEMS.register("sixty_seconds_rocket", () -> {
        SIXTY_SECONDS_ROCKET = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_ROCKET;
    });

    public static Item SIXTY_SECONDS_SMG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SMG = ITEMS.register("sixty_seconds_smg", () -> {
        SIXTY_SECONDS_SMG = new net.exmo.sixty_seconds.content.item.SixtySecondsGunItem(
                    new Item.Properties().durability(108), 3, 28.0D, 15, 1, false,
                    () -> SIXTY_SECONDS_SMG_AMMO, 12, 100);
        return SIXTY_SECONDS_SMG;
    });

    public static Item SIXTY_SECONDS_COMBAT_SHOTGUN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COMBAT_SHOTGUN = ITEMS.register("sixty_seconds_combat_shotgun", () -> {
        SIXTY_SECONDS_COMBAT_SHOTGUN = new net.exmo.sixty_seconds.content.item.SixtySecondsShotgunItem(
                    new Item.Properties().durability(30), 50, 12.0D, 35,
                    () -> SIXTY_SECONDS_SHOTGUN_AMMO);
        return SIXTY_SECONDS_COMBAT_SHOTGUN;
    });

    public static Item SIXTY_SECONDS_SABER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SABER = ITEMS.register("sixty_seconds_saber", () -> {
        SIXTY_SECONDS_SABER = new net.exmo.sixty_seconds.content.item.SixtySecondsMeleeWeaponItem(
                    new Item.Properties().durability(48).attributes(
                            net.minecraft.world.item.SwordItem.createAttributes(
                                    net.minecraft.world.item.Tiers.IRON, 6, -2.2F)), 27);
        return SIXTY_SECONDS_SABER;
    });

    public static Item SIXTY_SECONDS_DECOY_FLARE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DECOY_FLARE = ITEMS.register("sixty_seconds_decoy_flare", () -> {
        SIXTY_SECONDS_DECOY_FLARE = new net.exmo.sixty_seconds.content.item.SixtySecondsDecoyGrenadeItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_DECOY_FLARE;
    });

    public static Item SIXTY_SECONDS_SMOKE_GRENADE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SMOKE_GRENADE = ITEMS.register("sixty_seconds_smoke_grenade", () -> {
        SIXTY_SECONDS_SMOKE_GRENADE = new net.exmo.sixty_seconds.content.item.SixtySecondsSmokeGrenadeItem(
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_SMOKE_GRENADE;
    });

    public static Item SIXTY_SECONDS_MARKING_GRENADE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MARKING_GRENADE = ITEMS.register("sixty_seconds_marking_grenade", () -> {
        SIXTY_SECONDS_MARKING_GRENADE = new net.exmo.sixty_seconds.content.item.SixtySecondsMarkingGrenadeItem(
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_MARKING_GRENADE;
    });

    public static Item SIXTY_SECONDS_PLASTIC_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLASTIC_HELMET = ITEMS.register("sixty_seconds_plastic_helmet", () -> {
        SIXTY_SECONDS_PLASTIC_HELMET = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_PLASTIC_HELMET;
    });

    public static Item SIXTY_SECONDS_PLASTIC_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLASTIC_CHESTPLATE = ITEMS.register("sixty_seconds_plastic_chestplate", () -> {
        SIXTY_SECONDS_PLASTIC_CHESTPLATE = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_PLASTIC_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_PLASTIC_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLASTIC_LEGGINGS = ITEMS.register("sixty_seconds_plastic_leggings", () -> {
        SIXTY_SECONDS_PLASTIC_LEGGINGS = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_PLASTIC_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_PLASTIC_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PLASTIC_BOOTS = ITEMS.register("sixty_seconds_plastic_boots", () -> {
        SIXTY_SECONDS_PLASTIC_BOOTS = new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_PLASTIC_BOOTS;
    });

    public static Item SIXTY_SECONDS_ALLOY_HELMET;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_HELMET = ITEMS.register("sixty_seconds_alloy_helmet", () -> {
        SIXTY_SECONDS_ALLOY_HELMET = new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_ALLOY_HELMET;
    });

    public static Item SIXTY_SECONDS_ALLOY_CHESTPLATE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_CHESTPLATE = ITEMS.register("sixty_seconds_alloy_chestplate", () -> {
        SIXTY_SECONDS_ALLOY_CHESTPLATE = new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_ALLOY_CHESTPLATE;
    });

    public static Item SIXTY_SECONDS_ALLOY_LEGGINGS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_LEGGINGS = ITEMS.register("sixty_seconds_alloy_leggings", () -> {
        SIXTY_SECONDS_ALLOY_LEGGINGS = new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_ALLOY_LEGGINGS;
    });

    public static Item SIXTY_SECONDS_ALLOY_BOOTS;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_ALLOY_BOOTS = ITEMS.register("sixty_seconds_alloy_boots", () -> {
        SIXTY_SECONDS_ALLOY_BOOTS = new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_ALLOY_BOOTS;
    });

    public static Item SIXTY_SECONDS_EXPANSION_KEY_1;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EXPANSION_KEY_1 = ITEMS.register("sixty_seconds_expansion_key_1", () -> {
        SIXTY_SECONDS_EXPANSION_KEY_1 = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_EXPANSION_KEY_1;
    });

    public static Item SIXTY_SECONDS_EXPANSION_KEY_2;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EXPANSION_KEY_2 = ITEMS.register("sixty_seconds_expansion_key_2", () -> {
        SIXTY_SECONDS_EXPANSION_KEY_2 = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_EXPANSION_KEY_2;
    });

    public static Item SIXTY_SECONDS_EXPANSION_KEY_3;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_EXPANSION_KEY_3 = ITEMS.register("sixty_seconds_expansion_key_3", () -> {
        SIXTY_SECONDS_EXPANSION_KEY_3 = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_EXPANSION_KEY_3;
    });

    public static Item SIXTY_SECONDS_DIESEL_CAN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DIESEL_CAN = ITEMS.register("sixty_seconds_diesel_can", () -> {
        SIXTY_SECONDS_DIESEL_CAN = new Item(new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_DIESEL_CAN;
    });

    public static Item SIXTY_SECONDS_AVIATION_KEROSENE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_AVIATION_KEROSENE = ITEMS.register("sixty_seconds_aviation_kerosene", () -> {
        SIXTY_SECONDS_AVIATION_KEROSENE = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_AVIATION_KEROSENE;
    });

    public static Item SIXTY_SECONDS_MOTORCYCLE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MOTORCYCLE = ITEMS.register("sixty_seconds_motorcycle", () -> {
        SIXTY_SECONDS_MOTORCYCLE = new net.exmo.sixty_seconds.content.item.SixtySecondsVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_MOTORCYCLE);
        return SIXTY_SECONDS_MOTORCYCLE;
    });

    public static Item SIXTY_SECONDS_CAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CAR = ITEMS.register("sixty_seconds_car", () -> {
        SIXTY_SECONDS_CAR = new net.exmo.sixty_seconds.content.item.SixtySecondsVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_CAR);
        return SIXTY_SECONDS_CAR;
    });

    public static Item SIXTY_SECONDS_RAFT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RAFT = ITEMS.register("sixty_seconds_raft", () -> {
        SIXTY_SECONDS_RAFT = new net.exmo.sixty_seconds.content.item.SixtySecondsSeaVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_RAFT);
        return SIXTY_SECONDS_RAFT;
    });

    public static Item SIXTY_SECONDS_MOTORBOAT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_MOTORBOAT = ITEMS.register("sixty_seconds_motorboat", () -> {
        SIXTY_SECONDS_MOTORBOAT = new net.exmo.sixty_seconds.content.item.SixtySecondsSeaVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_MOTORBOAT);
        return SIXTY_SECONDS_MOTORBOAT;
    });

    public static Item SIXTY_SECONDS_FISHING_BOAT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FISHING_BOAT = ITEMS.register("sixty_seconds_fishing_boat", () -> {
        SIXTY_SECONDS_FISHING_BOAT = new net.exmo.sixty_seconds.content.item.SixtySecondsSeaVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_FISHING_BOAT);
        return SIXTY_SECONDS_FISHING_BOAT;
    });

    public static Item SIXTY_SECONDS_SUBMARINE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SUBMARINE = ITEMS.register("sixty_seconds_submarine", () -> {
        SIXTY_SECONDS_SUBMARINE = new net.exmo.sixty_seconds.content.item.SixtySecondsSubmarineItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_SUBMARINE);
        return SIXTY_SECONDS_SUBMARINE;
    });

    public static Item SIXTY_SECONDS_FLYER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FLYER = ITEMS.register("sixty_seconds_flyer", () -> {
        SIXTY_SECONDS_FLYER = new net.exmo.sixty_seconds.content.item.SixtySecondsFlyingVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_FLYER);
        return SIXTY_SECONDS_FLYER;
    });

    public static Item SIXTY_SECONDS_HELICOPTER;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HELICOPTER = ITEMS.register("sixty_seconds_helicopter", () -> {
        SIXTY_SECONDS_HELICOPTER = new net.exmo.sixty_seconds.content.item.SixtySecondsFlyingVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_HELICOPTER);
        return SIXTY_SECONDS_HELICOPTER;
    });

    public static Item SIXTY_SECONDS_AIRPLANE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_AIRPLANE = ITEMS.register("sixty_seconds_airplane", () -> {
        SIXTY_SECONDS_AIRPLANE = new net.exmo.sixty_seconds.content.item.SixtySecondsFlyingVehicleItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.exmo.sixty_seconds.registry.ModEntities.SIXTY_SECONDS_AIRPLANE);
        return SIXTY_SECONDS_AIRPLANE;
    });

    public static Item SIXTY_SECONDS_VEHICLE_REPAIR_TOOL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_VEHICLE_REPAIR_TOOL = ITEMS.register("sixty_seconds_vehicle_repair_tool", () -> {
        SIXTY_SECONDS_VEHICLE_REPAIR_TOOL = new net.exmo.sixty_seconds.content.item.SixtySecondsVehicleRepairItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_VEHICLE_REPAIR_TOOL;
    });

    public static Item SIXTY_SECONDS_VEHICLE_REPAIR_ADVANCED;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_VEHICLE_REPAIR_ADVANCED = ITEMS.register("sixty_seconds_vehicle_repair_advanced", () -> {
        SIXTY_SECONDS_VEHICLE_REPAIR_ADVANCED = new net.exmo.sixty_seconds.content.item.SixtySecondsVehicleRepairItem(
                    new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_VEHICLE_REPAIR_ADVANCED;
    });

    public static Item SIXTY_SECONDS_VEHICLE_REPAIR_UNIVERSAL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_VEHICLE_REPAIR_UNIVERSAL = ITEMS.register("sixty_seconds_vehicle_repair_universal", () -> {
        SIXTY_SECONDS_VEHICLE_REPAIR_UNIVERSAL = new net.exmo.sixty_seconds.content.item.SixtySecondsVehicleRepairItem(
                    new Item.Properties().stacksTo(2));
        return SIXTY_SECONDS_VEHICLE_REPAIR_UNIVERSAL;
    });

    public static Item SIXTY_SECONDS_SMALL_REPAIR_KIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SMALL_REPAIR_KIT = ITEMS.register("sixty_seconds_small_repair_kit", () -> {
        SIXTY_SECONDS_SMALL_REPAIR_KIT = new Item(new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_SMALL_REPAIR_KIT;
    });

    public static Item SIXTY_SECONDS_UNIVERSAL_REPAIR_KIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_UNIVERSAL_REPAIR_KIT = ITEMS.register("sixty_seconds_universal_repair_kit", () -> {
        SIXTY_SECONDS_UNIVERSAL_REPAIR_KIT = new Item(new Item.Properties().stacksTo(4));
        return SIXTY_SECONDS_UNIVERSAL_REPAIR_KIT;
    });

    public static Item SIXTY_SECONDS_RV_SPAWN_TOOL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RV_SPAWN_TOOL = ITEMS.register("sixty_seconds_rv_spawn_tool", () -> {
        SIXTY_SECONDS_RV_SPAWN_TOOL = new net.exmo.sixty_seconds.content.item.SixtySecondsRvSpawnToolItem(
                    new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_RV_SPAWN_TOOL;
    });

    public static final Map<SixtySecondsRvPart, Item> RV_PART_ITEMS = new EnumMap<>(SixtySecondsRvPart.class);
    static {
        for (SixtySecondsRvPart part : SixtySecondsRvPart.values()) {
            ITEMS.register("sixty_seconds_rv_" + part.id(), () -> {
                Item item = new SixtySecondsRvPartItem(part, new Item.Properties().stacksTo(16));
                RV_PART_ITEMS.put(part, item);
                return item;
            });
        }
    }

    public static Item SIXTY_SECONDS_FILTHY_JAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FILTHY_JAR = ITEMS.register("sixty_seconds_filthy_jar", () -> {
        SIXTY_SECONDS_FILTHY_JAR = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_FILTHY_JAR;
    });

    public static Item SIXTY_SECONDS_BLOOD_JAR;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_BLOOD_JAR = ITEMS.register("sixty_seconds_blood_jar", () -> {
        SIXTY_SECONDS_BLOOD_JAR = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_BLOOD_JAR;
    });

    public static Item SIXTY_SECONDS_REVIVAL_TOTEM;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_REVIVAL_TOTEM = ITEMS.register("sixty_seconds_revival_totem", () -> {
        SIXTY_SECONDS_REVIVAL_TOTEM = new Item(new Item.Properties().stacksTo(1));
        return SIXTY_SECONDS_REVIVAL_TOTEM;
    });

    public static Item SIXTY_SECONDS_SIMPLE_BAIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SIMPLE_BAIT = ITEMS.register("sixty_seconds_simple_bait", () -> {
        SIXTY_SECONDS_SIMPLE_BAIT = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_SIMPLE_BAIT;
    });

    public static Item SIXTY_SECONDS_FRAGRANT_BAIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_FRAGRANT_BAIT = ITEMS.register("sixty_seconds_fragrant_bait", () -> {
        SIXTY_SECONDS_FRAGRANT_BAIT = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_FRAGRANT_BAIT;
    });

    public static Item SIXTY_SECONDS_REFINED_BAIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_REFINED_BAIT = ITEMS.register("sixty_seconds_refined_bait", () -> {
        SIXTY_SECONDS_REFINED_BAIT = new Item(new Item.Properties().stacksTo(64));
        return SIXTY_SECONDS_REFINED_BAIT;
    });

    public static Item SIXTY_SECONDS_CHICKEN;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CHICKEN = ITEMS.register("sixty_seconds_chicken", () -> {
        SIXTY_SECONDS_CHICKEN = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.CHICKEN,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_CHICKEN;
    });

    public static Item SIXTY_SECONDS_PIG;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_PIG = ITEMS.register("sixty_seconds_pig", () -> {
        SIXTY_SECONDS_PIG = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.PIG,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_PIG;
    });

    public static Item SIXTY_SECONDS_SHEEP;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_SHEEP = ITEMS.register("sixty_seconds_sheep", () -> {
        SIXTY_SECONDS_SHEEP = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.SHEEP,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_SHEEP;
    });

    public static Item SIXTY_SECONDS_RABBIT;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_RABBIT = ITEMS.register("sixty_seconds_rabbit", () -> {
        SIXTY_SECONDS_RABBIT = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.RABBIT,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_RABBIT;
    });

    public static Item SIXTY_SECONDS_COW;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_COW = ITEMS.register("sixty_seconds_cow", () -> {
        SIXTY_SECONDS_COW = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.COW,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_COW;
    });

    public static Item SIXTY_SECONDS_WOLF;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_WOLF = ITEMS.register("sixty_seconds_wolf", () -> {
        SIXTY_SECONDS_WOLF = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.WOLF,
                    new Item.Properties().stacksTo(16));
        return SIXTY_SECONDS_WOLF;
    });

    public static Item SIXTY_SECONDS_HORSE;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_HORSE = ITEMS.register("sixty_seconds_horse", () -> {
        SIXTY_SECONDS_HORSE = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.HORSE,
                    new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_HORSE;
    });

    // ── 马蹄铁 / 前人留下的马铠 ──
    public static Item HORSESHOE_SUPERPIG;
    public static final DeferredItem<Item> HOLD_HORSESHOE_SUPERPIG = ITEMS.register("horseshoe_superpig", () -> {
        HORSESHOE_SUPERPIG = new SuperPigHorseshoeItem(new Item.Properties().stacksTo(1));
        return HORSESHOE_SUPERPIG;
    });

    public static Item HORSESHOE_RAINBOW;
    public static final DeferredItem<Item> HOLD_HORSESHOE_RAINBOW = ITEMS.register("horseshoe_rainbow", () -> {
        HORSESHOE_RAINBOW = new RainbowHorseshoeItem(new Item.Properties().stacksTo(1));
        return HORSESHOE_RAINBOW;
    });

    public static Item HORSESHOE_CANYUESA;
    public static final DeferredItem<Item> HOLD_HORSESHOE_CANYUESA = ITEMS.register("horseshoe_canyuesa", () -> {
        HORSESHOE_CANYUESA = new CanyuesaHorseshoeItem(new Item.Properties().stacksTo(1));
        return HORSESHOE_CANYUESA;
    });

    public static PredecessorHorseArmorItem PREDECESSOR_HORSE_ARMOR;
    public static final DeferredItem<PredecessorHorseArmorItem> HOLD_PREDECESSOR_HORSE_ARMOR = ITEMS.register("predecessor_horse_armor", () -> {
        PREDECESSOR_HORSE_ARMOR = new PredecessorHorseArmorItem(new Item.Properties());
        return PREDECESSOR_HORSE_ARMOR;
    });

    public static Item SIXTY_SECONDS_DONKEY;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_DONKEY = ITEMS.register("sixty_seconds_donkey", () -> {
        SIXTY_SECONDS_DONKEY = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.DONKEY,
                    new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_DONKEY;
    });

    public static Item SIXTY_SECONDS_CAMEL;
    public static final DeferredItem<Item> HOLD_SIXTY_SECONDS_CAMEL = ITEMS.register("sixty_seconds_camel", () -> {
        SIXTY_SECONDS_CAMEL = new net.exmo.sixty_seconds.content.item.TrapCageAnimalItem(EntityType.CAMEL,
                    new Item.Properties().stacksTo(8));
        return SIXTY_SECONDS_CAMEL;
    });


    public static Item NEWSPAPER;
    public static final DeferredItem<Item> HOLD_NEWSPAPER = ITEMS.register("newspaper", () -> {
        NEWSPAPER = new net.exmo.sixty_seconds.content.item.NewspaperItem(new Item.Properties().stacksTo(8));
        return NEWSPAPER;
    });
    public static Item WHEELCHAIR;
    public static final DeferredItem<Item> HOLD_WHEELCHAIR = ITEMS.register("wheelchair", () -> {
        WHEELCHAIR = new net.exmo.sixty_seconds.content.item.WheelchairItem();
        return WHEELCHAIR;
    });
    public static Item RADIO;
    public static final DeferredItem<Item> HOLD_RADIO = ITEMS.register("radio", () -> {
        RADIO = new net.exmo.sixty_seconds.content.item.RadioItem(new Item.Properties().stacksTo(1));
        return RADIO;
    });

    private ModItems() {}

    public static void register(IEventBus bus) {
        net.exmo.sixty_seconds.index.SixtySecDataComponentTypes.register(bus); // 报纸数据组件（RegisterEvent 时注册）
        ITEMS.register(bus);
    }
}
