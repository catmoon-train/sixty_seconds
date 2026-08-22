package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
        for (ServerTickEvents.EndServerTick listener : ServerTickEvents.END_SERVER_TICK.invokers()) {
            listener.onEndTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SixtySecGameWorldComponent.KEY.get(level).serverTick();
            for (ServerTickEvents.EndWorldTick listener : ServerTickEvents.END_WORLD_TICK.invokers()) {
                listener.onEndTick(level);
            }
        }
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
            for (ServerEntityEvents.Load listener : ServerEntityEvents.ENTITY_LOAD.invokers()) {
                listener.onLoad(event.getEntity(), level);
            }
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
        }
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
}
