package net.exmo.sixty_seconds.weights;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 负重系统配置（以世界目录下的 JSON 文件保存）。
 *
 * <ul>
 *   <li>{@code enabled}：系统总开关，默认 {@code true}。</li>
 *   <li>{@code backpackMultiplier}：背包内物品的轻盈系数（默认 {@code 1.5}）。背包里的物品会被
 *       结算并统一计重，实际重量 = 该物品基础重量 ÷ 此系数，即装进背包的物品更轻。</li>
 *   <li>{@code handMultiplier}：手持/穿戴/物品栏内物品的倍率（默认 {@code 1.0}）。</li>
 *   <li>{@code maxLoad}：免惩罚的负重上限（默认 {@code 50}）。</li>
 *   <li>{@code speedPenaltyEnabled}：是否对超重玩家施加移动减速（默认 {@code true}）。</li>
 *   <li>{@code speedPenaltyPerLoad}：每超出多少单位负重降低 1 级减速（默认 {@code 10}）。</li>
 *   <li>{@code tagWeights}：按物品标签统一定义重量，键形如 {@code "#minecraft:planks"}。</li>
 *   <li>{@code itemWeights}：按物品 id 定义重量。模组的物品用注册名（如
 *       {@code "sixty_seconds:iron_pipe"}）；TACZ 枪械/弹药/配件用其具体型号 id
 *       （如 {@code "tacz:glock_17"}、{@code "tacz:9mm"}、{@code "tacz:sight_sro_dot"}）。</li>
 * </ul>
 */
public class SixtySecondsWeightConfig {

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("backpack_multiplier")
    public double backpackMultiplier = 1.5;

    @SerializedName("hand_multiplier")
    public double handMultiplier = 1.0;

    @SerializedName("max_load")
    public double maxLoad = 50.0;

    @SerializedName("speed_penalty_enabled")
    public boolean speedPenaltyEnabled = true;

    @SerializedName("speed_penalty_per_load")
    public double speedPenaltyPerLoad = 10.0;

    /** 未配置物品时的兜底单件重量。 */
    @SerializedName("default_weight")
    public double defaultWeight = 1.0;

    /** 物品标签权重，键形如 {@code #namespace:tag}。 */
    @SerializedName("tag_weights")
    public Map<String, Double> tagWeights = new LinkedHashMap<>();

    /** 物品权重，键为物品注册名或 TACZ 具体型号 id。 */
    @SerializedName("item_weights")
    public Map<String, Double> itemWeights = new LinkedHashMap<>();
}
