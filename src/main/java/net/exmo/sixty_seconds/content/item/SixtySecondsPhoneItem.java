package net.exmo.sixty_seconds.content.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.exmo.sixty_seconds.client.screen.minigame.PhoneDialScreen;

/**
 * 电话：右手持有时左键打开拨号界面，拨打热线号码。服务端由 PhoneDialC2SPacket 处理。
 */
public class SixtySecondsPhoneItem extends Item {
    public SixtySecondsPhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new PhoneDialScreen(stack, hand));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
