package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.bridge.minigame.MinigameQuestServerNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.exmo.sixty_seconds.bridge.stubs.AdvancedCameraPayload;
import net.exmo.sixty_seconds.bridge.stubs.ShootMuzzleS2CPayload;
import net.exmo.sixty_seconds.bridge.stubs.TriggerScreenEdgeEffectPayload;
import net.exmo.sixty_seconds.network.OpenWeightConfigS2CPacket;
import net.exmo.sixty_seconds.network.WeightConfigSaveC2SPacket;
import net.exmo.sixty_seconds.network.TraitAllocateC2SPacket;
import net.exmo.sixty_seconds.network.WeatherS2CPacket;

public final class ModNetwork {
    private ModNetwork() {}

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> adapt(
            StreamCodec<?, T> codec) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BreakInExecuteC2SPacket.ID, adapt(BreakInExecuteC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(ComponentSyncS2CPacket.ID, adapt(ComponentSyncS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(CreateClientMarkingAreaPacket.ID, adapt(CreateClientMarkingAreaPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(DismantleC2SPacket.ID, adapt(DismantleC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(EditNewspaperPacket.ID, adapt(EditNewspaperPacket.STREAM_CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(GunTracerS2CPacket.ID, adapt(GunTracerS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(LootTableSaveC2SPacket.ID, adapt(LootTableSaveC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(MapTeleportC2SPacket.ID, adapt(MapTeleportC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(MinigameQuestPayload.SaveConfig.ID, adapt(MinigameQuestPayload.SaveConfig.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(MinigameQuestPayload.CompleteGame.ID, adapt(MinigameQuestPayload.CompleteGame.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(NpcDialogueActionC2SPacket.ID, adapt(NpcDialogueActionC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(NpcShopBuyC2SPacket.ID, adapt(NpcShopBuyC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(NpcShopSellC2SPacket.ID, adapt(NpcShopSellC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(NpcShopSaveC2SPacket.ID, adapt(NpcShopSaveC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(MinigameQuestPayload.OpenConfig.ID, adapt(MinigameQuestPayload.OpenConfig.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(MinigameQuestPayload.OpenGame.ID, adapt(MinigameQuestPayload.OpenGame.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenAirdropEditS2CPacket.ID, adapt(OpenAirdropEditS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenBreakInSelectS2CPacket.ID, adapt(OpenBreakInSelectS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenDismantleS2CPacket.ID, adapt(OpenDismantleS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenLootTableEditS2CPacket.ID, adapt(OpenLootTableEditS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenNpcDialogueS2CPacket.ID, adapt(OpenNpcDialogueS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenNpcShopEditS2CPacket.ID, adapt(OpenNpcShopEditS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenNpcShopS2CPacket.ID, adapt(OpenNpcShopS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenPowerPanelS2CPacket.ID, adapt(OpenPowerPanelS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenRadioChannelS2CPacket.ID, adapt(OpenRadioChannelS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenRandomSupplyBoxConfigS2CPacket.ID, adapt(OpenRandomSupplyBoxConfigS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenRvConsoleS2CPacket.ID, adapt(OpenRvConsoleS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenShelterDoorS2CPacket.ID, adapt(OpenShelterDoorS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenShelterPanelS2CPacket.ID, adapt(OpenShelterPanelS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenSixtySecondsDoorS2CPacket.ID, adapt(OpenSixtySecondsDoorS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenStationS2CPacket.ID, adapt(OpenStationS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenTeamLobbyS2CPacket.ID, adapt(OpenTeamLobbyS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenTechTreeS2CPacket.ID, adapt(OpenTechTreeS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenTradeS2CPacket.ID, adapt(OpenTradeS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenVaultLockpickS2CPacket.ID, adapt(OpenVaultLockpickS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenVisitChatS2CPacket.ID, adapt(OpenVisitChatS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenVisitPromptS2CPacket.ID, adapt(OpenVisitPromptS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenVisitRequestS2CPacket.ID, adapt(OpenVisitRequestS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(PhoneDialC2SPacket.TYPE, adapt(PhoneDialC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(PlayerHealthS2CPacket.ID, adapt(PlayerHealthS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(RadioChannelC2SPacket.ID, adapt(RadioChannelC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(RandomSupplyBoxConfigSaveC2SPacket.ID, adapt(RandomSupplyBoxConfigSaveC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(RvConsoleActionC2SPacket.ID, adapt(RvConsoleActionC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(ShelterDoorActionC2SPacket.ID, adapt(ShelterDoorActionC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(SixtySecondsCorpseMarkS2CPacket.ID, adapt(SixtySecondsCorpseMarkS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsEndGamePayload.ID, adapt(SixtySecondsEndGamePayload.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(SixtySecondsGunShootC2SPacket.ID, adapt(SixtySecondsGunShootC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(SixtySecondsHelicopterS2CPacket.ID, adapt(SixtySecondsHelicopterS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsIntroPayload.ID, adapt(SixtySecondsIntroPayload.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsMapZoneS2CPacket.ID, adapt(SixtySecondsMapZoneS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartArrivalS2CPacket.ID, adapt(SixtySecondsSeaChartArrivalS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartPositionsS2CPacket.ID, adapt(SixtySecondsSeaChartPositionsS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(SixtySecondsSeaChartReturnC2SPacket.ID, adapt(SixtySecondsSeaChartReturnC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartReturnCancelS2CPacket.ID, adapt(SixtySecondsSeaChartReturnCancelS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartReturnStartS2CPacket.ID, adapt(SixtySecondsSeaChartReturnStartS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartS2CPacket.ID, adapt(SixtySecondsSeaChartS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsSeaChartSailStartS2CPacket.ID, adapt(SixtySecondsSeaChartSailStartS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(SixtySecondsSeaChartWatchC2SPacket.ID, adapt(SixtySecondsSeaChartWatchC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(SixtySecondsStarMapRequestC2SPacket.ID, adapt(SixtySecondsStarMapRequestC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(SixtySecondsStarMapS2CPacket.ID, adapt(SixtySecondsStarMapS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SixtySecondsStationStockS2CPacket.ID, adapt(SixtySecondsStationStockS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(ShowCustomNewspaperPacket.ID, adapt(ShowCustomNewspaperPacket.STREAM_CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(SleepBlackoutS2CPacket.ID, adapt(SleepBlackoutS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(StationCraftC2SPacket.ID, adapt(StationCraftC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(SupplySearchS2CPacket.ID, adapt(SupplySearchS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(TeamLobbyActionC2SPacket.ID, adapt(TeamLobbyActionC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(TeamPingC2SPacket.ID, adapt(TeamPingC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(TeamPingS2CPacket.ID, adapt(TeamPingS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(TechUnlockC2SPacket.ID, adapt(TechUnlockC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(TokenExchangeC2SPacket.ID, adapt(TokenExchangeC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(TradeActionC2SPacket.ID, adapt(TradeActionC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(VaultLockpickCompleteC2SPacket.ID, adapt(VaultLockpickCompleteC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(TraitAllocateC2SPacket.ID, adapt(TraitAllocateC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(VehicleCameraS2CPacket.ID, adapt(VehicleCameraS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(VisitChatMessageS2CPacket.ID, adapt(VisitChatMessageS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(VisitChatSendC2SPacket.ID, adapt(VisitChatSendC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(VisitRequestC2SPacket.ID, adapt(VisitRequestC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(VisitResponseC2SPacket.ID, adapt(VisitResponseC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToServer(SubmarineControlC2SPacket.ID, adapt(SubmarineControlC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
        registrar.playToClient(AdvancedCameraPayload.TYPE, adapt(AdvancedCameraPayload.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(ShootMuzzleS2CPayload.TYPE, adapt(ShootMuzzleS2CPayload.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(TriggerScreenEdgeEffectPayload.TYPE, adapt(TriggerScreenEdgeEffectPayload.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(WeatherS2CPacket.TYPE, adapt(WeatherS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToClient(OpenWeightConfigS2CPacket.ID, adapt(OpenWeightConfigS2CPacket.CODEC), (payload, ctx) -> handleS2C(payload, ctx));
        registrar.playToServer(WeightConfigSaveC2SPacket.ID, adapt(WeightConfigSaveC2SPacket.CODEC), (payload, ctx) -> handleC2S(payload, ctx));
    }

    private static void handleC2S(CustomPacketPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            ServerPlayNetworking.SimpleContext fabric = new ServerPlayNetworking.SimpleContext(player);
            if (ServerPlayNetworking.dispatch(payload, fabric)) {
                return;
            }
            if (payload instanceof BreakInExecuteC2SPacket p) { BreakInExecuteC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TraitAllocateC2SPacket p) { TraitAllocateC2SPacket.handle(p, fabric); return; }
            if (payload instanceof DismantleC2SPacket p) { DismantleC2SPacket.handle(p, fabric); return; }
            if (payload instanceof EditNewspaperPacket p) { EditNewspaperPacket.handle(p, fabric); return; }
            if (payload instanceof LootTableSaveC2SPacket p) { LootTableSaveC2SPacket.handle(p, fabric); return; }
            if (payload instanceof MapTeleportC2SPacket p) { MapTeleportC2SPacket.handle(p, fabric); return; }
            if (payload instanceof MinigameQuestPayload.SaveConfig p) { MinigameQuestServerNetwork.handle(p, fabric); return; }
            if (payload instanceof MinigameQuestPayload.CompleteGame p) { MinigameQuestServerNetwork.handle(p, fabric); return; }
            if (payload instanceof NpcDialogueActionC2SPacket p) { NpcDialogueActionC2SPacket.handle(p, fabric); return; }
            if (payload instanceof NpcShopBuyC2SPacket p) { NpcShopBuyC2SPacket.handle(p, fabric); return; }
            if (payload instanceof NpcShopSellC2SPacket p) { NpcShopSellC2SPacket.handle(p, fabric); return; }
            if (payload instanceof NpcShopSaveC2SPacket p) { NpcShopSaveC2SPacket.handle(p, fabric); return; }
            if (payload instanceof PhoneDialC2SPacket p) { PhoneDialC2SPacket.handle(p, fabric); return; }
            if (payload instanceof RadioChannelC2SPacket p) { RadioChannelC2SPacket.handle(p, fabric); return; }
            if (payload instanceof RandomSupplyBoxConfigSaveC2SPacket p) { RandomSupplyBoxConfigSaveC2SPacket.handle(p, fabric); return; }
            if (payload instanceof RvConsoleActionC2SPacket p) { RvConsoleActionC2SPacket.handle(p, fabric); return; }
            if (payload instanceof ShelterDoorActionC2SPacket p) { ShelterDoorActionC2SPacket.handle(p, fabric); return; }
            if (payload instanceof SixtySecondsGunShootC2SPacket p) { SixtySecondsGunShootC2SPacket.handle(p, fabric); return; }
            if (payload instanceof SixtySecondsSeaChartReturnC2SPacket p) { SixtySecondsSeaChartReturnC2SPacket.handle(p, fabric); return; }
            if (payload instanceof SixtySecondsSeaChartWatchC2SPacket p) { SixtySecondsSeaChartWatchC2SPacket.handle(p, fabric); return; }
            if (payload instanceof SixtySecondsStarMapRequestC2SPacket) { SixtySecondsStarMapRequestC2SPacket.handle(player); return; }
            if (payload instanceof StationCraftC2SPacket p) { StationCraftC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TeamLobbyActionC2SPacket p) { TeamLobbyActionC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TeamPingC2SPacket p) { TeamPingC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TechUnlockC2SPacket p) { TechUnlockC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TokenExchangeC2SPacket p) { TokenExchangeC2SPacket.handle(p, fabric); return; }
            if (payload instanceof TradeActionC2SPacket p) { TradeActionC2SPacket.handle(p, fabric); return; }
            if (payload instanceof VaultLockpickCompleteC2SPacket p) { VaultLockpickCompleteC2SPacket.handle(p, fabric); return; }
            if (payload instanceof VisitChatSendC2SPacket p) { VisitChatSendC2SPacket.handle(p, fabric); return; }
            if (payload instanceof VisitRequestC2SPacket p) { VisitRequestC2SPacket.handle(p, fabric); return; }
            if (payload instanceof VisitResponseC2SPacket p) { VisitResponseC2SPacket.handle(p, fabric); return; }
            if (payload instanceof SubmarineControlC2SPacket p) { SubmarineControlC2SPacket.handle(p, fabric); return; }
            if (payload instanceof WeightConfigSaveC2SPacket p) { WeightConfigSaveC2SPacket.handle(p, fabric); return; }
        });
    }

    private static void handleS2C(CustomPacketPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (payload instanceof ComponentSyncS2CPacket sync) {
                ComponentSyncS2CPacket.handleClient(sync);
                return;
            }
            if (payload instanceof WeatherS2CPacket weather) {
                WeatherS2CPacket.handleClient(weather);
                return;
            }
            ClientPlayNetworking.dispatch(payload);
        });
    }
}
