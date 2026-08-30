package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.bridge.event.AllowPlayerDeathWithKiller;
import net.exmo.sixty_seconds.bridge.fabric.AttackEntityCallback;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.bridge.fabric.ServerEntityEvents;
import net.exmo.sixty_seconds.bridge.fabric.ServerLivingEntityEvents;
import net.exmo.sixty_seconds.bridge.fabric.ServerMessageEvents;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayConnectionEvents;
import net.exmo.sixty_seconds.bridge.fabric.ServerTickEvents;
import net.exmo.sixty_seconds.bridge.fabric.UseBlockCallback;
import net.exmo.sixty_seconds.bridge.fabric.UseEntityCallback;
import net.exmo.sixty_seconds.bridge.fabric.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.logic.SixtySecondsDailyEvents;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class NeoForgeEvents {
    private NeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        for (CommandRegistrationCallback callback : CommandRegistrationCallback.EVENT.invokers()) {
            callback.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
        }
    }

    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        for (ServerTickEvents.StartServerTick listener : ServerTickEvents.START_SERVER_TICK.invokers()) {
            listener.onStartTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        GameUtils.tickTaskQueue(event.getServer());
        net.exmo.sixty_seconds.weather.WeatherSync.serverTick(event.getServer());
        for (ServerTickEvents.EndServerTick listener : ServerTickEvents.END_SERVER_TICK.invokers()) {
            listener.onEndTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            net.exmo.sixty_seconds.lostcities.SixtySecondsBuildingTitles.tick(level);
            SixtySecGameWorldComponent.KEY.get(level).serverTick();
            // 海洋（海岛）维度：独立于主世界对局，自行驱动海洋生物刷新、海盗 NPC 与海岛登岛检测
            if (level.dimension() == SixtySeconds.OCEAN_DIMENSION) {
                net.exmo.sixty_seconds.island.SixtySecondsIslands.ensureOceanStarted(level);
                net.exmo.sixty_seconds.island.SixtySecondsIslands.tick(level);
                // 对局未开始时不刷新海洋生物与海盗 NPC，避免无意义生成
                if (SixtySecondsMod.RUNNING) {
                    net.exmo.sixty_seconds.logic.OceanCreatureSpawner.tick(level);
                    // 海盗等海面 NPC：海洋维度内（不依赖搜索区/对局）按固定间隔刷新
                    net.exmo.sixty_seconds.state.SixtySecondsState.Data od =
                            net.exmo.sixty_seconds.state.SixtySecondsState.get(level);
                    net.exmo.sixty_seconds.logic.SixtySecondsNpcSpawner.spawnPirates(level, od, level.isNight());
                }
            }
            for (ServerTickEvents.EndWorldTick listener : ServerTickEvents.END_WORLD_TICK.invokers()) {
                listener.onEndTick(level);
            }
            // 潜水服套装效果：穿戴全套时获得水下呼吸
            for (net.minecraft.world.entity.player.Player player : level.players()) {
                if (isFullDivingSuit(player)) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WATER_BREATHING, 240, 0, false, false));
                }
            }
        }
    }

    /** 玩家是否穿戴整套潜水服（头盔/胸甲/护腿/靴）。 */
    private static boolean isFullDivingSuit(net.minecraft.world.entity.player.Player player) {
        var armor = player.getInventory().armor;
        return !armor.get(3).isEmpty() && armor.get(3).getItem() == net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_DIVING_HELMET
                && !armor.get(2).isEmpty() && armor.get(2).getItem() == net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_DIVING_CHESTPLATE
                && !armor.get(1).isEmpty() && armor.get(1).getItem() == net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_DIVING_LEGGINGS
                && !armor.get(0).isEmpty() && armor.get(0).getItem() == net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_DIVING_BOOTS;
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        for (UseBlockCallback callback : UseBlockCallback.EVENT.invokers()) {
            InteractionResult result = callback.interact(event.getEntity(), event.getLevel(), event.getHand(),
                    event.getHitVec());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        for (UseItemCallback callback : UseItemCallback.EVENT.invokers()) {
            var holder = callback.interact(event.getEntity(), event.getLevel(), event.getHand());
            if (holder.getResult() != InteractionResult.PASS) {
                event.setCancellationResult(holder.getResult());
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        for (UseEntityCallback callback : UseEntityCallback.EVENT.invokers()) {
            InteractionResult result = callback.interact(event.getEntity(), event.getLevel(), event.getHand(),
                    event.getTarget(), null);
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (AttackEntityCallback callback : AttackEntityCallback.EVENT.invokers()) {
            InteractionResult result = callback.interact(player, player.level(), player.getUsedItemHand(),
                    event.getTarget(), null);
            if (result == InteractionResult.FAIL) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // 难度：怪物加入世界时按当前难度缩放攻击/血量（幂等）
            if (event.getEntity() instanceof LivingEntity mob) {
                net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.applyToMob(mob);
            }
            if (event.getEntity() instanceof OceanSharkEntity) {
                net.exmo.sixty_seconds.logic.OceanCreatureSpawner.onSharkJoined(level);
            }
            for (ServerEntityEvents.Load listener : ServerEntityEvents.ENTITY_LOAD.invokers()) {
                listener.onLoad(event.getEntity(), level);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getEntity() instanceof OceanSharkEntity) {
            net.exmo.sixty_seconds.logic.OceanCreatureSpawner.onSharkLeft(level);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        for (ServerLivingEntityEvents.AllowDamage listener : ServerLivingEntityEvents.ALLOW_DAMAGE.invokers()) {
            if (!listener.allowDamage(entity, event.getSource(), event.getAmount())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        for (ServerLivingEntityEvents.AllowDeath listener : ServerLivingEntityEvents.ALLOW_DEATH.invokers()) {
            if (!listener.allowDeath(entity, event.getSource(), entity.getHealth())) {
                event.setCanceled(true);
                return;
            }
        }
        if (entity instanceof ServerPlayer victim) {
            ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
            for (AllowPlayerDeathWithKiller listener : AllowPlayerDeathWithKiller.EVENT.invokers()) {
                if (!listener.allowDeath(victim, killer, SixtySeconds.id("vanilla_death"))) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        for (ServerLivingEntityEvents.AfterDeath listener : ServerLivingEntityEvents.AFTER_DEATH.invokers()) {
            listener.afterDeath(entity, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        for (ServerMessageEvents.AllowChatMessage listener : ServerMessageEvents.ALLOW_CHAT_MESSAGE.invokers()) {
            if (!listener.allowChatMessage(event.getMessage(), event.getPlayer(), event.getRawText())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayConnectionEvents.Handler handler = new ServerPlayConnectionEvents.Handler(player);
            for (ServerPlayConnectionEvents.Join listener : ServerPlayConnectionEvents.JOIN.invokers()) {
                listener.onPlayReady(handler, null, player.getServer());
            }
            // 重载世界后，首位玩家加入时自动恢复上一局存档
            net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.onPlayerJoin(player);
            net.exmo.sixty_seconds.weather.WeatherSync.resend(player);
        }
    }

    /**
     * 服务器停止（单人游戏里即「退出到主菜单 / 关闭存档」）时清空模组运行时状态。
     *
     * <p>本模组的存档相关状态是 static 的，而单人游戏换世界并不会重启 JVM，
     * 静态字段会原样带到下一个世界。残留的待恢复快照会让新世界的一局被上一世界的存档覆盖，
     * 表现为天数为 0、状态栏与 HUD 不显示、背包只剩 2 格等随机症状。</p>
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // 服务器即将关闭：立即保存当前对局进度，避免退出存档后进度丢失
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (SixtySecondsMod.RUNNING && SixtySecondsMod.isActive(level)) {
                net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.save(level);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // 先重置 RUNNING 标志，否则重进存档时 onPlayerJoin 检查 !RUNNING 为 false，不会触发 resume
        SixtySecondsMod.RUNNING = false;
        net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.resetRuntimeState();
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayConnectionEvents.Handler handler = new ServerPlayConnectionEvents.Handler(player);
            for (ServerPlayConnectionEvents.Disconnect listener : ServerPlayConnectionEvents.DISCONNECT.invokers()) {
                listener.onPlayDisconnect(handler, player.getServer());
            }
        }
    }

    // 庇护所内禁止使用采掘镐 / 采掘锹 / 采掘剪刀挖掘方块。
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (!SixtySecondsMod.isActive(level) || !SixtySecondsDailyEvents.isPlayerInShelter(player)) {
            return;
        }
        Item held = player.getMainHandItem().getItem();
        if (held == ModItems.SIXTY_SECONDS_MINING_PICKAXE
                || held == ModItems.SIXTY_SECONDS_MINING_SHOVEL
                || held == ModItems.SIXTY_SECONDS_MINING_SHEARS) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable(
                    "message.sixty_seconds.sixty_seconds.mining_tool.disabled_in_shelter").withStyle(ChatFormatting.RED), true);
        }
    }
}
