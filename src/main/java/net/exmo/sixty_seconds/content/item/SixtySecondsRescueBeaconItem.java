package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsRescue;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 救援信标（工程学科技合成）：仅当使用者是<b>队伍中唯一存活成员</b>时，可于游戏日<b>户外</b>右键激活 →
 * 全服广播救援倒计时，并给使用者打上「救援标记」：被标记者<b>进入撤离点建筑即可直接撤离</b>；
 * 倒计时归零时，一架救援直升机将出现在使用者身旁并附赠 2 桶航空煤油（{@link SixtySecondsRescue}）。
 * 激活即消耗，一局仅一次呼叫。
 */
public class SixtySecondsRescueBeaconItem extends Item {

    public SixtySecondsRescueBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide || !(user instanceof ServerPlayer player)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.consume(stack);
        }
        if (!SixtySecondsMod.isActive(level) || !GameUtils.isPlayerAliveAndSurvival(player)) {
            return InteractionResultHolder.pass(stack);
        }
        SixtySecondsState.Data data = SixtySecondsState.get(serverLevel);
        if (data.phase != SixtySecondsPhase.DAY) {
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.rescue_only_day"), true);
            return InteractionResultHolder.consume(stack);
        }
        if (SixtySecondsRescue.isActive()) {
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.rescue_already_active"), true);
            return InteractionResultHolder.consume(stack);
        }
        // 队伍必须仅剩使用者一人（其余成员已阵亡/变怪/不在场），否则信标无法激活
        SixtySecondsState.TeamData team = data.teams.get(SixtySecondsStatsComponent.KEY.get(player).teamId);
        if (team != null) {
            long alive = team.members.stream()
                    .filter(uuid -> serverLevel.getPlayerByUUID(uuid) instanceof ServerPlayer m
                            && GameUtils.isPlayerAliveAndSurvival(m)
                            && !SixtySecondsStatsComponent.KEY.get(m).monster)
                    .count();
            if (alive > 1) {
                player.displayClientMessage(
                        Component.translatable("message.sixty_seconds.sixty_seconds.rescue_only_one"), true);
                return InteractionResultHolder.consume(stack);
            }
        }
        // 必须在户外：不在自家住宅/避难所盒内（信号被屋顶遮蔽）
        if (team != null
                && ((team.shelterBox != null && team.shelterBox.contains(player.position()))
                        || (team.residentialBox != null && team.residentialBox.contains(player.position())))) {
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.rescue_indoors"), true);
            return InteractionResultHolder.consume(stack);
        }
        // 校验通过：消耗信标、标记玩家（可凭标记在撤离点直接撤离）并激活救援
        stack.shrink(1);
        SixtySecondsStatsComponent.KEY.get(player).rescueMarked = true;
        SixtySecondsStatsComponent.KEY.sync(player);
        SixtySecondsRescue.activate(serverLevel, player);
        player.displayClientMessage(
                Component.translatable("message.sixty_seconds.sixty_seconds.rescue_marked"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
