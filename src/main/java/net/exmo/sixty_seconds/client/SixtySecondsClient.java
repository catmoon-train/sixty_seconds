package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.bridge.client.CommonHudRenderCallback;
import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.exmo.sixty_seconds.bridge.client.TaskBlockOverlayRenderer;
import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.exmo.sixty_seconds.bridge.fabric.ClientPlayConnectionEvents;
import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.bridge.fabric.ClientTickEvents;
import net.exmo.sixty_seconds.bridge.fabric.HudRenderCallback;
import net.exmo.sixty_seconds.bridge.fabric.ItemTooltipCallback;
import net.exmo.sixty_seconds.bridge.fabric.KeyBindingHelper;
import net.exmo.sixty_seconds.bridge.fabric.WorldRenderContext;
import net.exmo.sixty_seconds.bridge.fabric.WorldRenderEvents;
import net.exmo.sixty_seconds.client.map.AreaMapHud;
import net.exmo.sixty_seconds.client.map.AreaMapManager;
import net.exmo.sixty_seconds.client.map.StarMapHud;
import net.exmo.sixty_seconds.client.map.StarMapManager;
import net.exmo.sixty_seconds.client.render.OceanSeaMonsterRenderer;
import net.exmo.sixty_seconds.client.render.OceanSharkRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsArrowRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsFlyingVehicleRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsMonsterRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsNpcRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsRvRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsSeaVehicleRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsTurretRenderer;
import net.exmo.sixty_seconds.client.render.SixtySecondsVehicleRenderer;
import net.exmo.sixty_seconds.client.gui.screen.NewspaperScreen;
import net.exmo.sixty_seconds.client.gui.screen.RadioChannelScreen;
import net.exmo.sixty_seconds.client.screen.AirdropLootEditScreen;
import net.exmo.sixty_seconds.client.screen.BreakInSelectScreen;
import net.exmo.sixty_seconds.client.screen.DismantleScreen;
import net.exmo.sixty_seconds.client.screen.LootTableEditScreen;
import net.exmo.sixty_seconds.client.screen.NpcDialogueScreen;
import net.exmo.sixty_seconds.client.screen.NpcShopEditScreen;
import net.exmo.sixty_seconds.client.screen.NpcShopScreen;
import net.exmo.sixty_seconds.client.screen.PowerPanelScreen;
import net.exmo.sixty_seconds.client.screen.RandomSupplyBoxConfigScreen;
import net.exmo.sixty_seconds.client.screen.ShelterDoorScreen;
import net.exmo.sixty_seconds.client.screen.ShelterPanelScreen;
import net.exmo.sixty_seconds.client.screen.SixtySecondsDoorScreen;
import net.exmo.sixty_seconds.client.screen.SixtySecondsRvScreen;
import net.exmo.sixty_seconds.client.screen.StationCraftScreen;
import net.exmo.sixty_seconds.client.screen.TeamLobbyScreen;
import net.exmo.sixty_seconds.client.screen.TechTreeScreen;
import net.exmo.sixty_seconds.client.screen.TradeScreen;
import net.exmo.sixty_seconds.client.screen.VisitChatScreen;
import net.exmo.sixty_seconds.client.screen.VisitPromptScreen;
import net.exmo.sixty_seconds.client.screen.VisitRequestScreen;
import net.exmo.sixty_seconds.content.entity.SixtySecondsFlyingVehicleEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsRvEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSeaVehicleEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsVehicleEntity;
import net.exmo.sixty_seconds.content.entity.WheelchairEntity;
import net.exmo.sixty_seconds.network.GunTracerS2CPacket;
import net.exmo.sixty_seconds.network.OpenAirdropEditS2CPacket;
import net.exmo.sixty_seconds.network.OpenBreakInSelectS2CPacket;
import net.exmo.sixty_seconds.network.OpenDismantleS2CPacket;
import net.exmo.sixty_seconds.network.OpenLootTableEditS2CPacket;
import net.exmo.sixty_seconds.network.OpenNpcDialogueS2CPacket;
import net.exmo.sixty_seconds.network.OpenNpcShopEditS2CPacket;
import net.exmo.sixty_seconds.network.OpenNpcShopS2CPacket;
import net.exmo.sixty_seconds.network.OpenPowerPanelS2CPacket;
import net.exmo.sixty_seconds.network.OpenRadioChannelS2CPacket;
import net.exmo.sixty_seconds.network.OpenRandomSupplyBoxConfigS2CPacket;
import net.exmo.sixty_seconds.network.OpenRvConsoleS2CPacket;
import net.exmo.sixty_seconds.network.OpenShelterDoorS2CPacket;
import net.exmo.sixty_seconds.network.OpenShelterPanelS2CPacket;
import net.exmo.sixty_seconds.network.OpenSixtySecondsDoorS2CPacket;
import net.exmo.sixty_seconds.network.OpenStationS2CPacket;
import net.exmo.sixty_seconds.network.OpenTeamLobbyS2CPacket;
import net.exmo.sixty_seconds.network.OpenTechTreeS2CPacket;
import net.exmo.sixty_seconds.network.OpenTradeS2CPacket;
import net.exmo.sixty_seconds.network.OpenVaultLockpickS2CPacket;
import net.exmo.sixty_seconds.network.OpenVisitChatS2CPacket;
import net.exmo.sixty_seconds.network.OpenVisitPromptS2CPacket;
import net.exmo.sixty_seconds.network.OpenVisitRequestS2CPacket;
import net.exmo.sixty_seconds.network.PlayerHealthS2CPacket;
import net.exmo.sixty_seconds.network.ShowCustomNewspaperPacket;
import net.exmo.sixty_seconds.network.SleepBlackoutS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsCorpseMarkS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsIntroPayload;
import net.exmo.sixty_seconds.network.SixtySecondsMapZoneS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartArrivalS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartPositionsS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartReturnCancelS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartReturnStartS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartSailStartS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsStarMapS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsStationStockS2CPacket;
import net.exmo.sixty_seconds.network.SupplySearchS2CPacket;
import net.exmo.sixty_seconds.network.VaultLockpickCompleteC2SPacket;
import net.exmo.sixty_seconds.network.VehicleCameraS2CPacket;
import net.exmo.sixty_seconds.network.VisitChatMessageS2CPacket;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.exmo.sixty_seconds.content.entity.WheelchairEntityModel;
import net.exmo.sixty_seconds.content.entity.WheelchairEntityRenderer;
import net.exmo.sixty_seconds.content.entity.WheelchairFieldItemRenderer;
import net.exmo.sixty_seconds.content.item.NewspaperItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = SixtySeconds.MOD_ID, value = Dist.CLIENT)
public final class SixtySecondsClient {
    private SixtySecondsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerPayloadReceivers();
            NewspaperItem.runner = (stack, hand) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (!hand.equals(InteractionHand.MAIN_HAND)) {
                    return false;
                }
                minecraft.setScreen(new NewspaperScreen(stack, hand));
                return true;
            };
            SixtySecondsHud.register();
            SixtySecondsCombatHud.register();
            SixtySecondsSearchHud.register();
            SixtySecondsSleepOverlay.register();
            SixtySecondsRvHud.register();
            SixtySecondsBlockNameHud.register();
            SixtySecondsTooltips.register();
            SixtySecondsNameTag.register();
            SixtySecondsClientMapZone.register();
            TeamPingClientHandler.register();
            SixtySecondsHelicopterClient.register();
            SixtySecondsEndGameClient.register();
            SeaChartReturnHud.register();
            StarMapManager.register();
            StarMapHud.register();
            AreaMapManager.register();
            AreaMapHud.register();
            ClientPlayNetworking.registerGlobalReceiver(VehicleCameraS2CPacket.ID, new VehicleCameraS2CPacket.ClientReceiver());
            ClientPlayNetworking.registerGlobalReceiver(OpenRadioChannelS2CPacket.ID, (payload, context) ->
                    context.client().execute(() -> context.client().setScreen(new RadioChannelScreen(payload.currentChannel()))));
            ClientPlayNetworking.registerGlobalReceiver(ShowCustomNewspaperPacket.ID, (payload, context) ->
                    context.client().execute(() -> context.client().setScreen(new NewspaperScreen(payload.pages(),
                            payload.title().orElse(Component.literal("")),
                            payload.author().orElse(Component.literal(""))))));
            NeoForge.EVENT_BUS.addListener(SixtySecondsClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(SixtySecondsClient::onLogout);
            NeoForge.EVENT_BUS.addListener(SixtySecondsClient::onWorldRender);
            NeoForge.EVENT_BUS.addListener(SixtySecondsClient::onTooltip);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.SIXTY_SECONDS_TURRET_ENTITY, SixtySecondsTurretRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_RAFT,
                ctx -> new SixtySecondsSeaVehicleRenderer(ctx, SixtySecondsSeaVehicleEntity.Kind.RAFT));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_MOTORBOAT,
                ctx -> new SixtySecondsSeaVehicleRenderer(ctx, SixtySecondsSeaVehicleEntity.Kind.MOTORBOAT));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_FISHING_BOAT,
                ctx -> new SixtySecondsSeaVehicleRenderer(ctx, SixtySecondsSeaVehicleEntity.Kind.FISHING_BOAT));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_MOTORCYCLE,
                ctx -> new SixtySecondsVehicleRenderer(ctx, SixtySecondsVehicleEntity.Kind.MOTORCYCLE));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_CAR,
                ctx -> new SixtySecondsVehicleRenderer(ctx, SixtySecondsVehicleEntity.Kind.CAR));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_RV, SixtySecondsRvRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_FLYER,
                ctx -> new SixtySecondsFlyingVehicleRenderer(ctx, SixtySecondsFlyingVehicleEntity.Kind.FLYER));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_HELICOPTER,
                ctx -> new SixtySecondsFlyingVehicleRenderer(ctx, SixtySecondsFlyingVehicleEntity.Kind.HELICOPTER));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_AIRPLANE,
                ctx -> new SixtySecondsFlyingVehicleRenderer(ctx, SixtySecondsFlyingVehicleEntity.Kind.AIRPLANE));
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_MONSTER, SixtySecondsMonsterRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_BOSS, SixtySecondsMonsterRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_ACID_SPIT, ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_ARROW, SixtySecondsArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_NPC, SixtySecondsNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.OCEAN_SHARK, OceanSharkRenderer::new);
        event.registerEntityRenderer(ModEntities.OCEAN_SEA_MONSTER, OceanSeaMonsterRenderer::new);
        event.registerEntityRenderer(ModEntities.SIXTY_SECONDS_GRENADE, ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WHEELCHAIR, WheelchairEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.WHEELCHAIR_FIELD_ITEM, WheelchairFieldItemRenderer::new);
        event.registerEntityRenderer(ModEntities.PLAYER_BODY, SixtySecondsClient::bodyRenderer);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WheelchairEntityModel.LAYER_LOCATION, WheelchairEntityModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (var key : KeyBindingHelper.KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SixtySeconds.id("hud"), (graphics, deltaTracker) -> {
            for (HudRenderCallback callback : HudRenderCallback.EVENT.invokers()) {
                callback.onHudRender(graphics, deltaTracker);
            }
            FakeGuiGraphics fake = new FakeGuiGraphics(graphics, true);
            for (var consumer : CommonHudRenderCallback.EVENT.getConsumer()) {
                consumer.accept(fake, deltaTracker);
            }
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            SixtySecGameWorldComponent.KEY.get(client.level).clientTick();
        }
        for (ClientTickEvents.EndTick listener : ClientTickEvents.END_CLIENT_TICK.invokers()) {
            listener.onEndTick(client);
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft client = Minecraft.getInstance();
        for (ClientPlayConnectionEvents.Disconnect listener : ClientPlayConnectionEvents.DISCONNECT.invokers()) {
            listener.onPlayDisconnect(client.getConnection(), client);
        }
    }

    private static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        WorldRenderContext context = new WorldRenderContext.Simple(event.getCamera(), event.getPoseStack(),
                event.getLevelRenderer() != null ? Minecraft.getInstance().renderBuffers().bufferSource() : null);
        if (context.consumers() == null) {
            return;
        }
        TaskBlockOverlayRenderer.render(context);
        GunTracerRenderer.render(context);
        SixtySecondsDoorOverlay.render(context);
        for (WorldRenderEvents.AfterTranslucent listener : WorldRenderEvents.AFTER_TRANSLUCENT.invokers()) {
            listener.afterTranslucent(context);
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        List<Component> lines = event.getToolTip();
        ItemStack stack = event.getItemStack();
        for (ItemTooltipCallback callback : ItemTooltipCallback.EVENT.invokers()) {
            callback.getTooltip(stack, event.getContext(), event.getFlags(), lines);
        }
    }

    private static HumanoidMobRenderer<PlayerBodyEntity, HumanoidModel<PlayerBodyEntity>> bodyRenderer(
            EntityRendererProvider.Context ctx) {
        return new HumanoidMobRenderer<>(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
            @Override
            public ResourceLocation getTextureLocation(PlayerBodyEntity entity) {
                return DefaultPlayerSkin.getDefaultTexture();
            }
        };
    }

    private static void registerPayloadReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OpenSixtySecondsDoorS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new SixtySecondsDoorScreen(payload.purpose()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenShelterDoorS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new ShelterDoorScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(OpenNpcDialogueS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new NpcDialogueScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(OpenNpcShopS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new NpcShopScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(OpenNpcShopEditS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new NpcShopEditScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(OpenLootTableEditS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new LootTableEditScreen(payload.table()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenRandomSupplyBoxConfigS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new RandomSupplyBoxConfigScreen(payload.pos(), payload.tier(),
                                payload.allCategories(), payload.enabledCategories()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenAirdropEditS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new AirdropLootEditScreen(payload.table()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenVisitRequestS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new VisitRequestScreen(payload.teamIds(), payload.labels()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenVisitPromptS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new VisitPromptScreen(payload.visitor(), payload.visitorName(), payload.requestType()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenVisitChatS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new VisitChatScreen(payload.partnerName()))));
        ClientPlayNetworking.registerGlobalReceiver(VisitChatMessageS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    VisitChatScreen.record(payload.sender(), payload.text());
                    if (!(context.client().screen instanceof VisitChatScreen)
                            && !(context.client().screen instanceof TradeScreen)
                            && context.client().player != null) {
                        context.client().player.displayClientMessage(
                                Component.translatable("message.sixty_seconds.sixty_seconds.visit_chat_incoming"), true);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenBreakInSelectS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new BreakInSelectScreen(payload.teamIds(), payload.labels(), payload.alarms()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenVaultLockpickS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> ClientPlayNetworking.send(new VaultLockpickCompleteC2SPacket(payload.vaultPos()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenTradeS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new TradeScreen(payload.partnerName()))));
        ClientPlayNetworking.registerGlobalReceiver(SupplySearchS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SixtySecondsSearchHud.onPacket(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SleepBlackoutS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SixtySecondsSleepOverlay.start(payload.durationTicks())));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsIntroPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new SixtySecondsIntroScreen())));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsMapZoneS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.active()) {
                        SixtySecondsClientMapZone.setZone(payload.toAabb(), payload.hasHome() ? payload.home() : null,
                                payload.safeZone(), payload.shelterDoors());
                    } else {
                        SixtySecondsClientMapZone.clearZone();
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsCorpseMarkS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.add()) {
                        SixtySecondsClientMapZone.setCorpseMarker(payload.x() + 0.5, payload.z() + 0.5);
                    } else {
                        SixtySecondsClientMapZone.clearCorpseMarker();
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(PlayerHealthS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> PlayerHealthS2CPacket.CLIENT_HEALTH
                        .put(payload.playerId(), new int[]{payload.health(), payload.healthMax()})));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SixtySecondsClientSeaChart.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsStarMapS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SixtySecondsClientStarMap.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartReturnStartS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SeaChartReturnHud.onReturnStart(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartArrivalS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SeaChartReturnHud.onArrivalSync(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartReturnCancelS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SeaChartReturnHud.cancel()));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartSailStartS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SeaChartReturnHud.onSailStart(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsSeaChartPositionsS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SeaChartReturnHud.onPositions(payload)));
        ClientPlayNetworking.registerGlobalReceiver(OpenPowerPanelS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new PowerPanelScreen(payload.remainingTicks()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenShelterPanelS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new ShelterPanelScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(OpenRvConsoleS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().level != null
                            && context.client().level.getEntity(payload.entityId()) instanceof SixtySecondsRvEntity rv) {
                        context.client().setScreen(new SixtySecondsRvScreen(rv));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenTeamLobbyS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.forceOpen()) {
                        context.client().setScreen(new TeamLobbyScreen(payload));
                    } else if (context.client().screen instanceof TeamLobbyScreen lobby) {
                        lobby.refresh(payload);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenTechTreeS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().screen instanceof TechTreeScreen tech) {
                        tech.refresh(payload);
                    } else {
                        context.client().setScreen(new TechTreeScreen(payload));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenStationS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new StationCraftScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(SixtySecondsStationStockS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> StationCraftScreen.updateHomeStock(payload.stock())));
        ClientPlayNetworking.registerGlobalReceiver(OpenDismantleS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new DismantleScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(GunTracerS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> GunTracerRenderer.onPacket(payload)));
    }
}
