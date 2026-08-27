package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.List;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.level.block.LanternBlock;
import net.exmo.sixty_seconds.content.item.SixtySecondsShelterPlaceableBlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SixtySeconds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SixtySeconds.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SixtySeconds.MOD_ID);

    public static Block SIXTY_SECONDS_SHELTER_DOOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SHELTER_DOOR = BLOCKS.register("sixty_seconds_shelter_door", () -> {
        SIXTY_SECONDS_SHELTER_DOOR = new net.exmo.sixty_seconds.content.block.ShelterDoorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(3.0F));
        return SIXTY_SECONDS_SHELTER_DOOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SHELTER_DOOR = ITEMS.register("sixty_seconds_shelter_door", () -> new BlockItem(HOLD_SIXTY_SECONDS_SHELTER_DOOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SHELTER_TRAPDOOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SHELTER_TRAPDOOR = BLOCKS.register("sixty_seconds_shelter_trapdoor", () -> {
        SIXTY_SECONDS_SHELTER_TRAPDOOR = new net.exmo.sixty_seconds.content.block.ShelterTrapdoorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(3.0F));
        return SIXTY_SECONDS_SHELTER_TRAPDOOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SHELTER_TRAPDOOR = ITEMS.register("sixty_seconds_shelter_trapdoor", () -> new BlockItem(HOLD_SIXTY_SECONDS_SHELTER_TRAPDOOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SHELTER_TRAPDOOR_PART;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SHELTER_TRAPDOOR_PART = BLOCKS.register("sixty_seconds_shelter_trapdoor_part", () -> {
        SIXTY_SECONDS_SHELTER_TRAPDOOR_PART = new net.exmo.sixty_seconds.content.block.ShelterTrapdoorPartBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(3.0F));
        return SIXTY_SECONDS_SHELTER_TRAPDOOR_PART;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SHELTER_TRAPDOOR_PART = ITEMS.register("sixty_seconds_shelter_trapdoor_part", () -> new BlockItem(HOLD_SIXTY_SECONDS_SHELTER_TRAPDOOR_PART.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SUPPLY_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SUPPLY_BOX = BLOCKS.register("sixty_seconds_supply_box", () -> {
        SIXTY_SECONDS_SUPPLY_BOX = new net.exmo.sixty_seconds.content.block.SupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F));
        return SIXTY_SECONDS_SUPPLY_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SUPPLY_BOX = ITEMS.register("sixty_seconds_supply_box", () -> new BlockItem(HOLD_SIXTY_SECONDS_SUPPLY_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SUPPLY_BOX_LOCKED;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SUPPLY_BOX_LOCKED = BLOCKS.register("sixty_seconds_supply_box_locked", () -> {
        SIXTY_SECONDS_SUPPLY_BOX_LOCKED = new net.exmo.sixty_seconds.content.block.SupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), true, false);
        return SIXTY_SECONDS_SUPPLY_BOX_LOCKED;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SUPPLY_BOX_LOCKED = ITEMS.register("sixty_seconds_supply_box_locked", () -> new BlockItem(HOLD_SIXTY_SECONDS_SUPPLY_BOX_LOCKED.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SUPPLY_BOX_ADVANCED;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED = BLOCKS.register("sixty_seconds_supply_box_advanced", () -> {
        SIXTY_SECONDS_SUPPLY_BOX_ADVANCED = new net.exmo.sixty_seconds.content.block.SupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), false, true);
        return SIXTY_SECONDS_SUPPLY_BOX_ADVANCED;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED = ITEMS.register("sixty_seconds_supply_box_advanced", () -> new BlockItem(HOLD_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED = BLOCKS.register("sixty_seconds_supply_box_advanced_locked", () -> {
        SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED = new net.exmo.sixty_seconds.content.block.SupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), true, true);
        return SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED = ITEMS.register("sixty_seconds_supply_box_advanced_locked", () -> new BlockItem(HOLD_SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX = BLOCKS.register("sixty_seconds_low_tier_random_supply_box", () -> {
        SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX = new net.exmo.sixty_seconds.content.block.RandomSupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), "low");
        return SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX = ITEMS.register("sixty_seconds_low_tier_random_supply_box", () -> new BlockItem(HOLD_SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX = BLOCKS.register("sixty_seconds_high_tier_random_supply_box", () -> {
        SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX = new net.exmo.sixty_seconds.content.block.RandomSupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), "high");
        return SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX = ITEMS.register("sixty_seconds_high_tier_random_supply_box", () -> new BlockItem(HOLD_SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_RANDOM_SUPPLY_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_RANDOM_SUPPLY_BOX = BLOCKS.register("sixty_seconds_random_supply_box", () -> {
        SIXTY_SECONDS_RANDOM_SUPPLY_BOX = new net.exmo.sixty_seconds.content.block.RandomSupplyBoxBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F), "low");
        return SIXTY_SECONDS_RANDOM_SUPPLY_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_RANDOM_SUPPLY_BOX = ITEMS.register("sixty_seconds_random_supply_box", () -> new BlockItem(HOLD_SIXTY_SECONDS_RANDOM_SUPPLY_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SURVIVOR_CAMP;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SURVIVOR_CAMP = BLOCKS.register("sixty_seconds_survivor_camp", () -> {
        SIXTY_SECONDS_SURVIVOR_CAMP = new net.exmo.sixty_seconds.content.block.SixtySecondsSurvivorCampBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(s -> 12).noOcclusion());
        return SIXTY_SECONDS_SURVIVOR_CAMP;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SURVIVOR_CAMP = ITEMS.register("sixty_seconds_survivor_camp", () -> new BlockItem(HOLD_SIXTY_SECONDS_SURVIVOR_CAMP.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SHELTER_PANEL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SHELTER_PANEL = BLOCKS.register("sixty_seconds_shelter_panel", () -> {
        SIXTY_SECONDS_SHELTER_PANEL = new net.exmo.sixty_seconds.content.block.SixtySecondsShelterPanelBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F));
        return SIXTY_SECONDS_SHELTER_PANEL;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SHELTER_PANEL = ITEMS.register("sixty_seconds_shelter_panel", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SHELTER_PANEL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_RESEARCH_TABLE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_RESEARCH_TABLE = BLOCKS.register("sixty_seconds_research_table", () -> {
        SIXTY_SECONDS_RESEARCH_TABLE = new net.exmo.sixty_seconds.content.block.SixtySecondsUsableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F));
        return SIXTY_SECONDS_RESEARCH_TABLE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_RESEARCH_TABLE = ITEMS.register("sixty_seconds_research_table", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_RESEARCH_TABLE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_DISMANTLER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_DISMANTLER = BLOCKS.register("sixty_seconds_dismantler", () -> {
        SIXTY_SECONDS_DISMANTLER = new net.exmo.sixty_seconds.content.block.SixtySecondsUsableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F));
        return SIXTY_SECONDS_DISMANTLER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_DISMANTLER = ITEMS.register("sixty_seconds_dismantler", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_DISMANTLER.get(), new Item.Properties()));

    // 铺路方块：独立方块，贴图继承灰色混凝土；只能在白色混凝土标记附近放置，且庇护所内禁止放置。
    public static Block SIXTY_SECONDS_PAVING_BLOCK;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_PAVING_BLOCK = BLOCKS.register("sixty_seconds_paving_block", () -> {
        SIXTY_SECONDS_PAVING_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAY_CONCRETE).strength(2.0F));
        return SIXTY_SECONDS_PAVING_BLOCK;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_PAVING_BLOCK = ITEMS.register("sixty_seconds_paving_block", () -> new SixtySecondsShelterPlaceableBlockItem(
            HOLD_SIXTY_SECONDS_PAVING_BLOCK.get(),
            new Item.Properties().component(DataComponents.CAN_PLACE_ON,
                    new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build()), false))));

    // 铺路灯笼：独立方块，材质/行为继承原版灯笼（发光、可悬挂）；只能在白色混凝土标记附近放置，且庇护所内禁止放置。
    public static Block SIXTY_SECONDS_LANTERN;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_LANTERN = BLOCKS.register("sixty_seconds_lantern", () -> {
        SIXTY_SECONDS_LANTERN = new LanternBlock(BlockBehaviour.Properties.of()
                .lightLevel(s -> 15).strength(0.3F)
                .sound(net.minecraft.world.level.block.SoundType.LANTERN).noCollission());
        return SIXTY_SECONDS_LANTERN;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_LANTERN = ITEMS.register("sixty_seconds_lantern", () -> new SixtySecondsShelterPlaceableBlockItem(
            HOLD_SIXTY_SECONDS_LANTERN.get(),
            new Item.Properties().component(DataComponents.CAN_PLACE_ON,
                    new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build()), false))));

    public static Block SIXTY_SECONDS_WORKBENCH;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_WORKBENCH = BLOCKS.register("sixty_seconds_workbench", () -> {
        SIXTY_SECONDS_WORKBENCH = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.WORKBENCH);
        return SIXTY_SECONDS_WORKBENCH;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_WORKBENCH = ITEMS.register("sixty_seconds_workbench", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_WORKBENCH.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_STOVE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_STOVE = BLOCKS.register("sixty_seconds_stove", () -> {
        SIXTY_SECONDS_STOVE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SMOKER).strength(2.5F).lightLevel(s -> 6),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.STOVE);
        return SIXTY_SECONDS_STOVE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_STOVE = ITEMS.register("sixty_seconds_stove", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_STOVE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_PURIFIER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_PURIFIER = BLOCKS.register("sixty_seconds_purifier", () -> {
        SIXTY_SECONDS_PURIFIER = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.BATHTUB);
        return SIXTY_SECONDS_PURIFIER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_PURIFIER = ITEMS.register("sixty_seconds_purifier", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_PURIFIER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_TAILOR_TABLE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_TAILOR_TABLE = BLOCKS.register("sixty_seconds_tailor_table", () -> {
        SIXTY_SECONDS_TAILOR_TABLE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LOOM).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.TAILOR);
        return SIXTY_SECONDS_TAILOR_TABLE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_TAILOR_TABLE = ITEMS.register("sixty_seconds_tailor_table", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_TAILOR_TABLE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ARSENAL_TABLE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ARSENAL_TABLE = BLOCKS.register("sixty_seconds_arsenal_table", () -> {
        SIXTY_SECONDS_ARSENAL_TABLE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.ARSENAL);
        return SIXTY_SECONDS_ARSENAL_TABLE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ARSENAL_TABLE = ITEMS.register("sixty_seconds_arsenal_table", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ARSENAL_TABLE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_GENERATOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_GENERATOR = BLOCKS.register("sixty_seconds_generator", () -> {
        SIXTY_SECONDS_GENERATOR = new net.exmo.sixty_seconds.content.block.SixtySecondsGeneratorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).lightLevel(
                            s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                                    ? 7 : 0));
        return SIXTY_SECONDS_GENERATOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_GENERATOR = ITEMS.register("sixty_seconds_generator", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_GENERATOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_LAMP;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_LAMP = BLOCKS.register("sixty_seconds_lamp", () -> {
        SIXTY_SECONDS_LAMP = new net.exmo.sixty_seconds.content.block.SixtySecondsLampBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(0.5F).lightLevel(
                            s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                                    ? 14 : 0));
        return SIXTY_SECONDS_LAMP;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_LAMP = ITEMS.register("sixty_seconds_lamp", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_LAMP.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BARRICADE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BARRICADE = BLOCKS.register("sixty_seconds_barricade", () -> {
        SIXTY_SECONDS_BARRICADE = new net.exmo.sixty_seconds.content.block.SixtySecondsBarricadeBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(4.0F),
                    net.exmo.sixty_seconds.SixtySecondsBalance.BARRICADE_HP);
        return SIXTY_SECONDS_BARRICADE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BARRICADE = ITEMS.register("sixty_seconds_barricade", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BARRICADE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_HEAVY_BARRICADE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_HEAVY_BARRICADE = BLOCKS.register("sixty_seconds_heavy_barricade", () -> {
        SIXTY_SECONDS_HEAVY_BARRICADE = new net.exmo.sixty_seconds.content.block.SixtySecondsBarricadeBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).strength(6.0F),
                    net.exmo.sixty_seconds.SixtySecondsBalance.BARRICADE_HEAVY_HP);
        return SIXTY_SECONDS_HEAVY_BARRICADE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_HEAVY_BARRICADE = ITEMS.register("sixty_seconds_heavy_barricade", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_HEAVY_BARRICADE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SPIKE_TRAP;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SPIKE_TRAP = BLOCKS.register("sixty_seconds_spike_trap", () -> {
        SIXTY_SECONDS_SPIKE_TRAP = new net.exmo.sixty_seconds.content.block.SixtySecondsSpikeTrapBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion());
        return SIXTY_SECONDS_SPIKE_TRAP;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SPIKE_TRAP = ITEMS.register("sixty_seconds_spike_trap", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SPIKE_TRAP.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BARBED_WIRE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BARBED_WIRE = BLOCKS.register("sixty_seconds_barbed_wire", () -> {
        SIXTY_SECONDS_BARBED_WIRE = new net.exmo.sixty_seconds.content.block.SixtySecondsSpikeTrapBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion(),
                    net.exmo.sixty_seconds.SixtySecondsBalance.BARBED_WIRE_DAMAGE);
        return SIXTY_SECONDS_BARBED_WIRE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BARBED_WIRE = ITEMS.register("sixty_seconds_barbed_wire", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BARBED_WIRE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_TURRET;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_TURRET = BLOCKS.register("sixty_seconds_turret", () -> {
        SIXTY_SECONDS_TURRET = new net.exmo.sixty_seconds.content.block.SixtySecondsTurretBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion());
        return SIXTY_SECONDS_TURRET;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_TURRET = ITEMS.register("sixty_seconds_turret", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_TURRET.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_REINFORCED_BARRICADE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_REINFORCED_BARRICADE = BLOCKS.register("sixty_seconds_reinforced_barricade", () -> {
        SIXTY_SECONDS_REINFORCED_BARRICADE = new net.exmo.sixty_seconds.content.block.SixtySecondsBarricadeBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(8.0F),
                    net.exmo.sixty_seconds.SixtySecondsBalance.BARRICADE_REINFORCED_HP);
        return SIXTY_SECONDS_REINFORCED_BARRICADE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_REINFORCED_BARRICADE = ITEMS.register("sixty_seconds_reinforced_barricade", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_REINFORCED_BARRICADE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_FLOODLIGHT;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_FLOODLIGHT = BLOCKS.register("sixty_seconds_floodlight", () -> {
        SIXTY_SECONDS_FLOODLIGHT = new net.exmo.sixty_seconds.content.block.SixtySecondsLampBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(0.5F).lightLevel(
                            s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                                    ? 15 : 0));
        return SIXTY_SECONDS_FLOODLIGHT;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_FLOODLIGHT = ITEMS.register("sixty_seconds_floodlight", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_FLOODLIGHT.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SHOWER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SHOWER = BLOCKS.register("sixty_seconds_shower", () -> {
        SIXTY_SECONDS_SHOWER = new net.exmo.sixty_seconds.content.block.SixtySecondsShowerBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F));
        return SIXTY_SECONDS_SHOWER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SHOWER = ITEMS.register("sixty_seconds_shower", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SHOWER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_PLANTER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_PLANTER = BLOCKS.register("sixty_seconds_planter", () -> {
        SIXTY_SECONDS_PLANTER = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks());
        return SIXTY_SECONDS_PLANTER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_PLANTER = ITEMS.register("sixty_seconds_planter", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_PLANTER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_RAIN_BARREL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_RAIN_BARREL = BLOCKS.register("sixty_seconds_rain_barrel", () -> {
        SIXTY_SECONDS_RAIN_BARREL = new net.exmo.sixty_seconds.content.block.SixtySecondsWaterCollectorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_BASIC_CAPACITY,
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_BASIC_INTERVAL);
        return SIXTY_SECONDS_RAIN_BARREL;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_RAIN_BARREL = ITEMS.register("sixty_seconds_rain_barrel", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_RAIN_BARREL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_RAIN_COLLECTOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_RAIN_COLLECTOR = BLOCKS.register("sixty_seconds_rain_collector", () -> {
        SIXTY_SECONDS_RAIN_COLLECTOR = new net.exmo.sixty_seconds.content.block.SixtySecondsWaterCollectorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_ROOF_CAPACITY,
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_ROOF_INTERVAL);
        return SIXTY_SECONDS_RAIN_COLLECTOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_RAIN_COLLECTOR = ITEMS.register("sixty_seconds_rain_collector", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_RAIN_COLLECTOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_CONDENSER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_CONDENSER = BLOCKS.register("sixty_seconds_condenser", () -> {
        SIXTY_SECONDS_CONDENSER = new net.exmo.sixty_seconds.content.block.SixtySecondsWaterCollectorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F).randomTicks(),
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_CONDENSER_CAPACITY,
                    net.exmo.sixty_seconds.SixtySecondsBalance.COLLECTOR_CONDENSER_INTERVAL);
        return SIXTY_SECONDS_CONDENSER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_CONDENSER = ITEMS.register("sixty_seconds_condenser", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_CONDENSER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_STERILE_TABLE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_STERILE_TABLE = BLOCKS.register("sixty_seconds_sterile_table", () -> {
        SIXTY_SECONDS_STERILE_TABLE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.STERILE);
        return SIXTY_SECONDS_STERILE_TABLE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_STERILE_TABLE = ITEMS.register("sixty_seconds_sterile_table", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_STERILE_TABLE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ADV_WORKBENCH;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ADV_WORKBENCH = BLOCKS.register("sixty_seconds_adv_workbench", () -> {
        SIXTY_SECONDS_ADV_WORKBENCH = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.ADV_WORKBENCH);
        return SIXTY_SECONDS_ADV_WORKBENCH;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ADV_WORKBENCH = ITEMS.register("sixty_seconds_adv_workbench", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ADV_WORKBENCH.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SMELTER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SMELTER = BLOCKS.register("sixty_seconds_smelter", () -> {
        SIXTY_SECONDS_SMELTER = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BLAST_FURNACE).strength(3.0F).lightLevel(s -> 8),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.SMELTER);
        return SIXTY_SECONDS_SMELTER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SMELTER = ITEMS.register("sixty_seconds_smelter", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SMELTER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BREWERY;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BREWERY = BLOCKS.register("sixty_seconds_brewery", () -> {
        SIXTY_SECONDS_BREWERY = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.BREWING);
        return SIXTY_SECONDS_BREWERY;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BREWERY = ITEMS.register("sixty_seconds_brewery", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BREWERY.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ARMOR_FORGE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ARMOR_FORGE = BLOCKS.register("sixty_seconds_armor_forge", () -> {
        SIXTY_SECONDS_ARMOR_FORGE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.ARMOR_FORGE);
        return SIXTY_SECONDS_ARMOR_FORGE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ARMOR_FORGE = ITEMS.register("sixty_seconds_armor_forge", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ARMOR_FORGE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_WEAPON_FORGE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_WEAPON_FORGE = BLOCKS.register("sixty_seconds_weapon_forge", () -> {
        SIXTY_SECONDS_WEAPON_FORGE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.WEAPON_FORGE);
        return SIXTY_SECONDS_WEAPON_FORGE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_WEAPON_FORGE = ITEMS.register("sixty_seconds_weapon_forge", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_WEAPON_FORGE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_LATHE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_LATHE = BLOCKS.register("sixty_seconds_lathe", () -> {
        SIXTY_SECONDS_LATHE = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.LATHE);
        return SIXTY_SECONDS_LATHE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_LATHE = ITEMS.register("sixty_seconds_lathe", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_LATHE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_MELTING_POT;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_MELTING_POT = BLOCKS.register("sixty_seconds_melting_pot", () -> {
        SIXTY_SECONDS_MELTING_POT = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).strength(2.5F).lightLevel(s -> 6),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.MELTING_POT);
        return SIXTY_SECONDS_MELTING_POT;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_MELTING_POT = ITEMS.register("sixty_seconds_melting_pot", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_MELTING_POT.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ALTAR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ALTAR = BLOCKS.register("sixty_seconds_altar", () -> {
        SIXTY_SECONDS_ALTAR = new net.exmo.sixty_seconds.content.block.SixtySecondsStationBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).lightLevel(s -> 5),
                    net.exmo.sixty_seconds.logic.SixtySecondsRecipes.Station.ALTAR);
        return SIXTY_SECONDS_ALTAR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ALTAR = ITEMS.register("sixty_seconds_altar", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ALTAR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ADVANCED_PLANTER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ADVANCED_PLANTER = BLOCKS.register("sixty_seconds_advanced_planter", () -> {
        SIXTY_SECONDS_ADVANCED_PLANTER = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.ADVANCED);
        return SIXTY_SECONDS_ADVANCED_PLANTER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ADVANCED_PLANTER = ITEMS.register("sixty_seconds_advanced_planter", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ADVANCED_PLANTER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_MUSHROOM_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_MUSHROOM_BOX = BLOCKS.register("sixty_seconds_mushroom_box", () -> {
        SIXTY_SECONDS_MUSHROOM_BOX = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.MUSHROOM);
        return SIXTY_SECONDS_MUSHROOM_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_MUSHROOM_BOX = ITEMS.register("sixty_seconds_mushroom_box", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_MUSHROOM_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_GARDENER_PLANTER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_GARDENER_PLANTER = BLOCKS.register("sixty_seconds_gardener_planter", () -> {
        SIXTY_SECONDS_GARDENER_PLANTER = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.GARDENER);
        return SIXTY_SECONDS_GARDENER_PLANTER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_GARDENER_PLANTER = ITEMS.register("sixty_seconds_gardener_planter", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_GARDENER_PLANTER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_ARID_CULTIVATOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_ARID_CULTIVATOR = BLOCKS.register("sixty_seconds_arid_cultivator", () -> {
        SIXTY_SECONDS_ARID_CULTIVATOR = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.ARID);
        return SIXTY_SECONDS_ARID_CULTIVATOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_ARID_CULTIVATOR = ITEMS.register("sixty_seconds_arid_cultivator", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_ARID_CULTIVATOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_HYDROPONIC_BOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_HYDROPONIC_BOX = BLOCKS.register("sixty_seconds_hydroponic_box", () -> {
        SIXTY_SECONDS_HYDROPONIC_BOX = new net.exmo.sixty_seconds.content.block.SixtySecondsPlanterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.HYDROPONIC);
        return SIXTY_SECONDS_HYDROPONIC_BOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_HYDROPONIC_BOX = ITEMS.register("sixty_seconds_hydroponic_box", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_HYDROPONIC_BOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SAPLING_CULTIVATOR;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SAPLING_CULTIVATOR = BLOCKS.register("sixty_seconds_sapling_cultivator", () -> {
        SIXTY_SECONDS_SAPLING_CULTIVATOR = new net.exmo.sixty_seconds.content.block.SixtySecondsSaplingCultivatorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).randomTicks(),
                    net.exmo.sixty_seconds.logic.SixtySecondsCrops.Tier.SAPLING);
        return SIXTY_SECONDS_SAPLING_CULTIVATOR;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SAPLING_CULTIVATOR = ITEMS.register("sixty_seconds_sapling_cultivator", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SAPLING_CULTIVATOR.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_VAULT_SMALL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_VAULT_SMALL = BLOCKS.register("sixty_seconds_vault_small", () -> {
        SIXTY_SECONDS_VAULT_SMALL = new net.exmo.sixty_seconds.content.block.SixtySecondsVaultBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F), 2, true);
        return SIXTY_SECONDS_VAULT_SMALL;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_VAULT_SMALL = ITEMS.register("sixty_seconds_vault_small", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_VAULT_SMALL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_VAULT_MEDIUM;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_VAULT_MEDIUM = BLOCKS.register("sixty_seconds_vault_medium", () -> {
        SIXTY_SECONDS_VAULT_MEDIUM = new net.exmo.sixty_seconds.content.block.SixtySecondsVaultBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.5F), 3, true);
        return SIXTY_SECONDS_VAULT_MEDIUM;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_VAULT_MEDIUM = ITEMS.register("sixty_seconds_vault_medium", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_VAULT_MEDIUM.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_VAULT_LARGE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_VAULT_LARGE = BLOCKS.register("sixty_seconds_vault_large", () -> {
        SIXTY_SECONDS_VAULT_LARGE = new net.exmo.sixty_seconds.content.block.SixtySecondsVaultBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0F), 6, true);
        return SIXTY_SECONDS_VAULT_LARGE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_VAULT_LARGE = ITEMS.register("sixty_seconds_vault_large", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_VAULT_LARGE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_CHEST_SMALL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_CHEST_SMALL = BLOCKS.register("sixty_seconds_base_chest_small", () -> {
        SIXTY_SECONDS_BASE_CHEST_SMALL = new net.exmo.sixty_seconds.content.block.SixtySecondsVaultBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F), 3, false);
        return SIXTY_SECONDS_BASE_CHEST_SMALL;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_CHEST_SMALL = ITEMS.register("sixty_seconds_base_chest_small", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BASE_CHEST_SMALL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_CHEST_LARGE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_CHEST_LARGE = BLOCKS.register("sixty_seconds_base_chest_large", () -> {
        SIXTY_SECONDS_BASE_CHEST_LARGE = new net.exmo.sixty_seconds.content.block.SixtySecondsVaultBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F), 6, false);
        return SIXTY_SECONDS_BASE_CHEST_LARGE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_CHEST_LARGE = ITEMS.register("sixty_seconds_base_chest_large", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BASE_CHEST_LARGE.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_MAILBOX;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_MAILBOX = BLOCKS.register("sixty_seconds_mailbox", () -> {
        SIXTY_SECONDS_MAILBOX = new net.exmo.sixty_seconds.content.block.SixtySecondsMailboxBlock();
        return SIXTY_SECONDS_MAILBOX;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_MAILBOX = ITEMS.register("sixty_seconds_mailbox", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_MAILBOX.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_ALARM;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_ALARM = BLOCKS.register("sixty_seconds_base_alarm", () -> {
        SIXTY_SECONDS_BASE_ALARM = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F),
                    net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock.Kind.ALARM);
        return SIXTY_SECONDS_BASE_ALARM;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_ALARM = ITEMS.register("sixty_seconds_base_alarm", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_BASE_ALARM.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_DOLL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_DOLL = BLOCKS.register("sixty_seconds_doll", () -> {
        SIXTY_SECONDS_DOLL = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).strength(0.8F),
                    net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock.Kind.DOLL);
        return SIXTY_SECONDS_DOLL;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_DOLL = ITEMS.register("sixty_seconds_doll", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_DOLL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_SUBWOOFER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SUBWOOFER = BLOCKS.register("sixty_seconds_subwoofer", () -> {
        SIXTY_SECONDS_SUBWOOFER = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F),
                    net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock.Kind.SUBWOOFER);
        return SIXTY_SECONDS_SUBWOOFER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SUBWOOFER = ITEMS.register("sixty_seconds_subwoofer", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_SUBWOOFER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_DOOR_1;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_DOOR_1 = BLOCKS.register("sixty_seconds_base_door_1", () -> {
        SIXTY_SECONDS_BASE_DOOR_1 = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseDoorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(50.0F).noLootTable(), 1);
        return SIXTY_SECONDS_BASE_DOOR_1;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_DOOR_1 = ITEMS.register("sixty_seconds_base_door_1", () -> new BlockItem(HOLD_SIXTY_SECONDS_BASE_DOOR_1.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_DOOR_2;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_DOOR_2 = BLOCKS.register("sixty_seconds_base_door_2", () -> {
        SIXTY_SECONDS_BASE_DOOR_2 = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseDoorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(50.0F).noLootTable(), 2);
        return SIXTY_SECONDS_BASE_DOOR_2;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_DOOR_2 = ITEMS.register("sixty_seconds_base_door_2", () -> new BlockItem(HOLD_SIXTY_SECONDS_BASE_DOOR_2.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_BASE_DOOR_3;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_BASE_DOOR_3 = BLOCKS.register("sixty_seconds_base_door_3", () -> {
        SIXTY_SECONDS_BASE_DOOR_3 = new net.exmo.sixty_seconds.content.block.SixtySecondsBaseDoorBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(50.0F).noLootTable(), 3);
        return SIXTY_SECONDS_BASE_DOOR_3;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_BASE_DOOR_3 = ITEMS.register("sixty_seconds_base_door_3", () -> new BlockItem(HOLD_SIXTY_SECONDS_BASE_DOOR_3.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_POWER_BATTERY;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_POWER_BATTERY = BLOCKS.register("sixty_seconds_power_battery", () -> {
        SIXTY_SECONDS_POWER_BATTERY = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F));
        return SIXTY_SECONDS_POWER_BATTERY;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_POWER_BATTERY = ITEMS.register("sixty_seconds_power_battery", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_POWER_BATTERY.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_POWER_AMPLIFIER;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_POWER_AMPLIFIER = BLOCKS.register("sixty_seconds_power_amplifier", () -> {
        SIXTY_SECONDS_POWER_AMPLIFIER = new net.exmo.sixty_seconds.content.block.SixtySecondsPowerAmplifierBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion());
        return SIXTY_SECONDS_POWER_AMPLIFIER;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_POWER_AMPLIFIER = ITEMS.register("sixty_seconds_power_amplifier", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_POWER_AMPLIFIER.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_TRAP_CAGE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_TRAP_CAGE = BLOCKS.register("sixty_seconds_trap_cage", () -> {
        SIXTY_SECONDS_TRAP_CAGE = new net.exmo.sixty_seconds.content.block.TrapCageBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F));
        return SIXTY_SECONDS_TRAP_CAGE;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_TRAP_CAGE = ITEMS.register("sixty_seconds_trap_cage", () -> new net.exmo.sixty_seconds.content.item.SixtySecondsPlaceableBlockItem(HOLD_SIXTY_SECONDS_TRAP_CAGE.get(), new Item.Properties()));

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SupplyBoxBlockEntity> SIXTY_SECONDS_SUPPLY_BOX_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SupplyBoxBlockEntity>> HOLD_SIXTY_SECONDS_SUPPLY_BOX_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_supply_box", () -> {
                SIXTY_SECONDS_SUPPLY_BOX_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.SupplyBoxBlockEntity::new, SIXTY_SECONDS_SUPPLY_BOX, SIXTY_SECONDS_SUPPLY_BOX_LOCKED, SIXTY_SECONDS_SUPPLY_BOX_ADVANCED, SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED).build(null);
                return SIXTY_SECONDS_SUPPLY_BOX_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.RandomSupplyBoxBlockEntity> SIXTY_SECONDS_RANDOM_SUPPLY_BOX_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.RandomSupplyBoxBlockEntity>> HOLD_SIXTY_SECONDS_RANDOM_SUPPLY_BOX_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_random_supply_box", () -> {
                SIXTY_SECONDS_RANDOM_SUPPLY_BOX_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.RandomSupplyBoxBlockEntity::new, SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX, SIXTY_SECONDS_HIGH_TIER_RANDOM_SUPPLY_BOX, SIXTY_SECONDS_RANDOM_SUPPLY_BOX).build(null);
                return SIXTY_SECONDS_RANDOM_SUPPLY_BOX_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsPlanterBlockEntity> SIXTY_SECONDS_PLANTER_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsPlanterBlockEntity>> HOLD_SIXTY_SECONDS_PLANTER_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_planter", () -> {
                SIXTY_SECONDS_PLANTER_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.SixtySecondsPlanterBlockEntity::new, SIXTY_SECONDS_PLANTER, SIXTY_SECONDS_ADVANCED_PLANTER, SIXTY_SECONDS_MUSHROOM_BOX, SIXTY_SECONDS_GARDENER_PLANTER, SIXTY_SECONDS_ARID_CULTIVATOR, SIXTY_SECONDS_HYDROPONIC_BOX, SIXTY_SECONDS_SAPLING_CULTIVATOR).build(null);
                return SIXTY_SECONDS_PLANTER_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsVaultBlockEntity> SIXTY_SECONDS_VAULT_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsVaultBlockEntity>> HOLD_SIXTY_SECONDS_VAULT_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_vault", () -> {
                SIXTY_SECONDS_VAULT_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.SixtySecondsVaultBlockEntity::new, SIXTY_SECONDS_VAULT_SMALL, SIXTY_SECONDS_VAULT_MEDIUM, SIXTY_SECONDS_VAULT_LARGE, SIXTY_SECONDS_BASE_CHEST_SMALL, SIXTY_SECONDS_BASE_CHEST_LARGE).build(null);
                return SIXTY_SECONDS_VAULT_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsTurretBlockEntity> SIXTY_SECONDS_TURRET_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsTurretBlockEntity>> HOLD_SIXTY_SECONDS_TURRET_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_turret", () -> {
                SIXTY_SECONDS_TURRET_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.SixtySecondsTurretBlockEntity::new, SIXTY_SECONDS_TURRET).build(null);
                return SIXTY_SECONDS_TURRET_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsMailboxBlockEntity> SIXTY_SECONDS_MAILBOX_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsMailboxBlockEntity>> HOLD_SIXTY_SECONDS_MAILBOX_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_mailbox", () -> {
                SIXTY_SECONDS_MAILBOX_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.SixtySecondsMailboxBlockEntity::new, SIXTY_SECONDS_MAILBOX).build(null);
                return SIXTY_SECONDS_MAILBOX_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.TrapCageBlockEntity> TRAP_CAGE_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.TrapCageBlockEntity>> HOLD_TRAP_CAGE_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_trap_cage", () -> {
                TRAP_CAGE_ENTITY = BlockEntityType.Builder.of(net.exmo.sixty_seconds.content.block_entity.TrapCageBlockEntity::new, SIXTY_SECONDS_TRAP_CAGE).build(null);
                return TRAP_CAGE_ENTITY;
            });


    // 电脑（透明贴面、可贴墙）
    public static Block SIXTY_SECONDS_MINIGAME_QUEST_PANEL;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_MINIGAME_QUEST_PANEL = BLOCKS.register(
            "sixty_seconds_minigame_quest_panel", () -> {
                SIXTY_SECONDS_MINIGAME_QUEST_PANEL = new net.exmo.sixty_seconds.bridge.minigame.MinigameQuestPanelBlock(
                        BlockBehaviour.Properties.of().strength(2.5F).noOcclusion());
                return SIXTY_SECONDS_MINIGAME_QUEST_PANEL;
            });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_MINIGAME_QUEST_PANEL = ITEMS.register(
            "sixty_seconds_minigame_quest_panel",
            () -> new BlockItem(HOLD_SIXTY_SECONDS_MINIGAME_QUEST_PANEL.get(), new Item.Properties()));

    public static Block SIXTY_SECONDS_MINIGAME_QUEST;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_MINIGAME_QUEST = BLOCKS.register("sixty_seconds_minigame_quest", () -> {
        SIXTY_SECONDS_MINIGAME_QUEST = new net.exmo.sixty_seconds.bridge.minigame.MinigameQuestBlock(
                BlockBehaviour.Properties.of().strength(2.5F).noOcclusion());
        return SIXTY_SECONDS_MINIGAME_QUEST;
    });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_MINIGAME_QUEST = ITEMS.register("sixty_seconds_minigame_quest",
            () -> new BlockItem(HOLD_SIXTY_SECONDS_MINIGAME_QUEST.get(), new Item.Properties()));
    public static BlockEntityType<net.exmo.sixty_seconds.bridge.minigame.MinigameQuestBlockEntity> SIXTY_SECONDS_MINIGAME_QUEST_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.bridge.minigame.MinigameQuestBlockEntity>> HOLD_SIXTY_SECONDS_MINIGAME_QUEST_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_minigame_quest", () -> {
                SIXTY_SECONDS_MINIGAME_QUEST_ENTITY = BlockEntityType.Builder.of(
                        net.exmo.sixty_seconds.bridge.minigame.MinigameQuestBlockEntity::new,
                        SIXTY_SECONDS_MINIGAME_QUEST, SIXTY_SECONDS_MINIGAME_QUEST_PANEL).build(null);
                return SIXTY_SECONDS_MINIGAME_QUEST_ENTITY;
            });

    public static BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsRatHoleBlockEntity> SIXTY_SECONDS_RAT_HOLE_ENTITY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.exmo.sixty_seconds.content.block_entity.SixtySecondsRatHoleBlockEntity>> HOLD_SIXTY_SECONDS_RAT_HOLE_ENTITY =
            BLOCK_ENTITIES.register("sixty_seconds_rat_hole", () -> {
                SIXTY_SECONDS_RAT_HOLE_ENTITY = BlockEntityType.Builder.of(
                        net.exmo.sixty_seconds.content.block_entity.SixtySecondsRatHoleBlockEntity::new,
                        SIXTY_SECONDS_RAT_HOLE).build(null);
                return SIXTY_SECONDS_RAT_HOLE_ENTITY;
            });

    // 出生点方块（透明、无碰撞，放在已登记的住宅/庇护所模板内即登记该建筑出生点）
    public static Block SIXTY_SECONDS_SPAWN_POINT;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_SPAWN_POINT = BLOCKS.register(
            "sixty_seconds_spawn_point", () -> {
                SIXTY_SECONDS_SPAWN_POINT = new net.exmo.sixty_seconds.content.block.SixtySecondsSpawnPointBlock(
                        BlockBehaviour.Properties.of().strength(2.5F).noOcclusion().noCollission());
                return SIXTY_SECONDS_SPAWN_POINT;
            });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_SPAWN_POINT = ITEMS.register(
            "sixty_seconds_spawn_point",
            () -> new BlockItem(HOLD_SIXTY_SECONDS_SPAWN_POINT.get(), new Item.Properties()));

    // 老鼠洞方块（每天右键可掏出基础资源，也有可能掏不到）
    public static Block SIXTY_SECONDS_RAT_HOLE;
    public static final DeferredBlock<Block> HOLD_SIXTY_SECONDS_RAT_HOLE = BLOCKS.register(
            "sixty_seconds_rat_hole", () -> {
                SIXTY_SECONDS_RAT_HOLE = new net.exmo.sixty_seconds.content.block.SixtySecondsRatHoleBlock(
                        BlockBehaviour.Properties.of().strength(1.5F));
                return SIXTY_SECONDS_RAT_HOLE;
            });
    public static final DeferredItem<Item> ITEM_SIXTY_SECONDS_RAT_HOLE = ITEMS.register(
            "sixty_seconds_rat_hole",
            () -> new BlockItem(HOLD_SIXTY_SECONDS_RAT_HOLE.get(), new Item.Properties()));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
