package net.exmo.sixty_seconds.content.block_entity;

import net.exmo.sixty_seconds.loot.SixtySecondsLootStore;
import net.exmo.sixty_seconds.loot.SixtySecondsLootTable;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.content.item.SixtySecondsLootMagnifierItem;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem;
import net.exmo.sixty_seconds.content.block.SupplyBoxBlock;
import net.minecraft.core.NonNullList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 物资箱方块实体：按自身 {@link #category} 从共享 loot 表加权抽取，每日刷新（惰性：交互时按当前游戏日刷新）、
 * <b>每日全局一次</b>——任何玩家领取后当天全员不可再搜。物资在领取时即时掷骰（而非刷新时缓存一份），
 * 避免多人拿到同一份预掷结果。参照 {@code net.exmo.sixty_seconds.content.block_entity.SupplyCrateBlockEntity}。
 */
public class SupplyBoxBlockEntity extends BlockEntity {
    public String category = "tool";
    private int lastRefreshDay = -1;
    /** 当天是否已被任何玩家领取（所有玩家共享一次机会）。 */
    private boolean claimedToday = false;
    /** 每次领取掷出的物资件数（空投奖励箱=9，普通箱=1）。 */
    private int bonusRolls = 1;
    /** 一次性箱（空投奖励箱）：被搜刮一次后整个方块移除，不随日刷新。 */
    private boolean oneShot = false;
    /** 上锁箱是否已被撬开（撬开后持续开放）。 */
    private boolean unlocked = false;

    /** 容器搜刮形式：箱内物品（放大镜占位或已揭示战利品），按日生成一次。 */
    private NonNullList<ItemStack> searchItems =
            NonNullList.withSize(SixtySecondsBalance.SUPPLY_SEARCH_CONTAINER_SIZE, ItemStack.EMPTY);
    /** 已生成的游戏日，避免同一天重复生成放大镜。 */
    private int generatedDay = -1;

    public SupplyBoxBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ENTITY, pos, state);
    }

    /** 供子类（如随机物资箱）复用同一套逻辑，仅换 {@link BlockEntityType}。 */
    protected SupplyBoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 本箱现在能否被搜刮（当天未被任何人领过且 loot 表有可抽条目）。用于开始搜刮前预检，避免白搜。
     * 惰性按当前游戏日刷新——<b>不做全图扫描</b>，仅交互到的箱子在此刻按需重置。
     */
    public boolean hasLootFor(ServerLevel level, ServerPlayer player) {
        ensureDaily(level);
        return !claimedToday && hasAnyLoot(level);
    }

    /**
     * 领取：每日全局一次，领取时即时加权掷骰（{@link #bonusRolls} 件 + 星级连锁概率额外件数）。
     * 当天已被领过返回空列表。<b>区域等级</b>（{@code SixtySecondsAreaLevels.levelAt} 按箱子坐标反查）
     * 越高：稀有（低权重）条目相对越容易被抽中（权重压平指数），且按连锁概率额外多开出物品
     *（{@link SixtySecondsAreaLevels#chainBonusRolls}：1星10%+1 … 5星40%/30%/5% 最多+3）。
     */
    public List<ItemStack> claim(ServerLevel level, ServerPlayer player) {
        ensureDaily(level);
        if (claimedToday) {
            return List.of();
        }
        claimedToday = true;
        setChanged();
        SixtySecondsLootTable table = SixtySecondsLootStore.get(level);
        int areaLevel = net.exmo.sixty_seconds.logic.SixtySecondsAreaLevels.levelAt(level, worldPosition);
        // 高级物资箱：使用独立的 advanced_* 战利品类别，与普通物资箱完全分开
        boolean advanced = getBlockState().getBlock()
                instanceof net.exmo.sixty_seconds.content.block.SupplyBoxBlock box && box.advanced();
        if (advanced) {
            areaLevel = Math.min(net.exmo.sixty_seconds.SixtySecondsBalance.AREA_LEVEL_MAX, areaLevel + 2);
        }
        double exponent = net.exmo.sixty_seconds.logic.SixtySecondsAreaLevels.lootExponent(areaLevel);
        int rolls = Math.max(1, bonusRolls)
                + net.exmo.sixty_seconds.logic.SixtySecondsAreaLevels.chainBonusRolls(level.random, areaLevel)
                + (advanced ? 2 : 0);
        List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            // 每件独立选类别（随机箱=每件随机一类）+ 独立掷骰
            // 高级物资箱：从 advanced_* 类别抽取
            ItemStack stack = table.roll(chooseCategory(level, table), level.random, exponent);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        // 高级物资箱：额外掷一次「稀有」类别
        if (advanced && level.random.nextFloat() < 0.4F) {
            ItemStack rare = table.roll("advanced_rare", level.random, exponent);
            if (!rare.isEmpty()) {
                out.add(rare);
            }
        }
        // 野外箱（不在任何队伍的住宅/避难所盒内）：额外掷一次「野外专属」类别
        // （废弃金属/贵金属器件/酿造器件/信号枪只能在野外搜到）
        if (!advanced && isInField(level) && level.random.nextFloat() < 0.5F) {
            ItemStack field = table.roll("field", level.random, exponent);
            if (!field.isEmpty()) {
                out.add(field);
            }
        }
        if (net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.lootExtraChance(player) > 0
                && level.random.nextDouble() < 0.10) {
            ItemStack extra = table.roll(chooseCategory(level, table), level.random, exponent);
            if (!extra.isEmpty()) {
                out.add(extra);
            }
        }
        return out;
    }

    /** 本箱是否在野外（不落在任何队伍的住宅盒/避难所盒内）。 */
    private boolean isInField(ServerLevel level) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        for (SixtySecondsState.TeamData team : SixtySecondsState.get(level).teams.values()) {
            if ((team.shelterBox != null && team.shelterBox.contains(x, y, z))
                    || (team.residentialBox != null && team.residentialBox.contains(x, y, z))) {
                return false;
            }
        }
        return true;
    }

    /** 配置为空投奖励箱：一次领取掷 {@code rolls} 件，搜刮一次后整箱移除。 */
    public void setAirdropReward(int rolls) {
        this.bonusRolls = Math.max(1, rolls);
        this.oneShot = true;
        setChanged();
    }

    public boolean isOneShot() {
        return oneShot;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked() {
        this.unlocked = true;
        setChanged();
    }

    /** 确保按日重置（模板克隆时 NBT 可能残留 unlocked=true，交互前调用此方法重置）。 */
    public void ensureDaily(ServerLevel level) {
        int day = SixtySecondsState.get(level).dayNumber;
        if (day != lastRefreshDay) {
            lastRefreshDay = day;
            claimedToday = false;
            unlocked = false;
            generatedDay = -1;
            setChanged();
        }
    }

    /** 本箱 loot 来源当前是否有可抽条目。随机物资箱覆写为任一类别可抽即可。 */
    protected boolean hasAnyLoot(ServerLevel level) {
        return SixtySecondsLootStore.get(level).canRoll(category);
    }

    /**
     * 本次刷新使用哪个 loot 类别。基础物资箱固定用自身 {@link #category}；
     * 高级物资箱使用 {@code advanced_} 前缀的独立类别；
     * 随机物资箱（{@link RandomSupplyBoxBlockEntity}）覆写为每次刷新随机取一类。
     */
    protected String chooseCategory(ServerLevel level, SixtySecondsLootTable table) {
        boolean advanced = getBlockState().getBlock()
                instanceof net.exmo.sixty_seconds.content.block.SupplyBoxBlock box && box.advanced();
        if (advanced) {
            String advCategory = "advanced_" + category;
            if (table.canRoll(advCategory)) {
                return advCategory;
            }
        }
        return category;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Category", category);
        tag.putInt("BonusRolls", bonusRolls);
        tag.putBoolean("OneShot", oneShot);
        tag.putBoolean("Unlocked", unlocked);
        // 仅保存非空物品；空 ItemStack 无法编码，会导致写存档抛 IllegalStateException
        net.minecraft.nbt.CompoundTag searchMap = new net.minecraft.nbt.CompoundTag();
        for (int i = 0; i < searchItems.size(); i++) {
            ItemStack s = searchItems.get(i);
            if (!s.isEmpty()) {
                searchMap.put(Integer.toString(i), s.save(registries));
            }
        }
        tag.put("SearchItems", searchMap);
        tag.putInt("GeneratedDay", generatedDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String stored = tag.getString("Category");
        category = stored.isEmpty() ? "tool" : stored;
        bonusRolls = tag.contains("BonusRolls") ? Math.max(1, tag.getInt("BonusRolls")) : 1;
        oneShot = tag.getBoolean("OneShot");
        unlocked = tag.getBoolean("Unlocked");
        if (tag.contains("SearchItems", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag searchSaved = tag.getCompound("SearchItems");
            searchItems = NonNullList.withSize(SixtySecondsBalance.SUPPLY_SEARCH_CONTAINER_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < searchItems.size(); i++) {
                String key = Integer.toString(i);
                if (searchSaved.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    searchItems.set(i, ItemStack.parse(registries, searchSaved.getCompound(key)).orElse(ItemStack.EMPTY));
                }
            }
        } else if (tag.contains("SearchItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            // 兼容旧版本：SearchItems 曾为 ListTag
            net.minecraft.nbt.ListTag searchSaved = tag.getList("SearchItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
            searchItems = NonNullList.withSize(SixtySecondsBalance.SUPPLY_SEARCH_CONTAINER_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < searchSaved.size() && i < searchItems.size(); i++) {
                searchItems.set(i, ItemStack.parse(registries, searchSaved.getCompound(i)).orElse(ItemStack.EMPTY));
            }
        }
        if (tag.contains("GeneratedDay")) {
            generatedDay = tag.getInt("GeneratedDay");
        }
    }

    // ── 容器搜刮形式支持 ───────────────────────────────────────────────

    public NonNullList<ItemStack> getSearchItems() {
        return searchItems;
    }

    public boolean isSearchItemsEmpty() {
        for (ItemStack s : searchItems) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 容器形式下打开箱子时调用：按需生成放大镜占位物（每日一次，与每日领取共享 claimedToday）。 */
    public void generateSearchLoot(ServerLevel level, ServerPlayer player) {
        ensureDaily(level);
        int day = SixtySecondsState.get(level).dayNumber;
        if (generatedDay == day) {
            return;
        }
        generatedDay = day;
        for (int i = 0; i < searchItems.size(); i++) {
            searchItems.set(i, ItemStack.EMPTY);
        }
        List<ItemStack> loot = claim(level, player);
        if (!loot.isEmpty()) {
            boolean advanced = isAdvancedBox();
            // 一次性箱（空投奖励箱）有固定的全部战利品，不按普通/高级上限截断，避免丢物资
            int max = oneShot ? searchItems.size()
                    : (advanced ? SixtySecondsBalance.SUPPLY_SEARCH_ADVANCED_MAX
                    : SixtySecondsBalance.SUPPLY_SEARCH_NORMAL_MAX);
            int n = Math.min(loot.size(), max);
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < searchItems.size(); i++) {
                slots.add(i);
            }
            for (int i = slots.size() - 1; i > 0; i--) {
                int j = level.random.nextInt(i + 1);
                Collections.swap(slots, i, j);
            }
            SixtySecondsWeightConfig wcfg = SixtySecondsWeightConfigStore.get(level.getServer());
            double m = SixtySecondsTraitSystem.lootSearchMultiplier(player);
            for (int k = 0; k < n; k++) {
                ItemStack lootStack = loot.get(k);
                int ticks = computeSearchTicks(lootStack, wcfg, m);
                searchItems.set(slots.get(k), SixtySecondsLootMagnifierItem.create(level, lootStack, ticks));
            }
        }
        setChanged();
    }

    /** 左键搜刮读条完成后由服务端揭示：把放大镜替换为真实战利品（仅当仍是放大镜且玩家在范围内）。 */
    public void revealSlot(int slot, ServerPlayer player) {
        ServerLevel level = (ServerLevel) getLevel();
        if (level == null || slot < 0 || slot >= searchItems.size()) {
            return;
        }
        ItemStack mag = searchItems.get(slot);
        if (!SixtySecondsLootMagnifierItem.isMagnifier(mag)) {
            return;
        }
        if (player.distanceToSqr(getBlockPos().getCenter()) > SixtySecondsBalance.SUPPLY_SEARCH_CONTAINER_MAX_DIST_SQR) {
            return;
        }
        ItemStack loot = SixtySecondsLootMagnifierItem.getLoot(level, mag);
        if (loot.isEmpty()) {
            return;
        }
        searchItems.set(slot, loot.copy());
        setChanged();
    }

    /** 一次性箱（空投奖励箱）箱内物资被取空后移除方块。 */
    public void removeOneShotBox() {
        if (getLevel() != null && getLevel().getBlockState(getBlockPos()).getBlock() == this.getBlockState().getBlock()) {
            getLevel().removeBlock(getBlockPos(), false);
        }
    }

    private boolean isAdvancedBox() {
        return getBlockState().getBlock() instanceof SupplyBoxBlock box && box.advanced();
    }

    private int computeSearchTicks(ItemStack loot, SixtySecondsWeightConfig cfg, double m) {
        double weight = SixtySecondsWeightCalc.unitWeight(loot, cfg);
        double ticks = (SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS + weight * SixtySecondsBalance.SUPPLY_SEARCH_WEIGHT_TICKS) * m;
        return (int) Math.max(SixtySecondsBalance.SUPPLY_SEARCH_MIN_TICKS,
                Math.min(SixtySecondsBalance.SUPPLY_SEARCH_MAX_TICKS, Math.round(ticks)));
    }
}
