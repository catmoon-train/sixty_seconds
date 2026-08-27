package net.exmo.sixty_seconds.client.screen.minigame;

import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.network.PhoneDialC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 电话拨号界面：6 位号码输入 + 拨号盘，拨号后把号码发往服务端处理。
 */
public class PhoneDialScreen extends Screen {
    private final Minecraft minecraft;
    private final ItemStack heldStack;
    private final InteractionHand hand;
    private EditBox numberField;

    public PhoneDialScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("gui.sixty_seconds.sixty_seconds.phone_dial_title"));
        this.minecraft = Minecraft.getInstance();
        this.heldStack = stack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        assert this.minecraft != null;
        int left = (this.width - 200) / 2;
        int top = (this.height - 240) / 2;

        numberField = new EditBox(this.font, left + 20, top + 40, 160, 20, Component.empty());
        numberField.setMaxLength(6);
        numberField.setValue("");
        numberField.setFocused(true);
        this.addRenderableWidget(numberField);

        int startY = top + 70;
        int btnW = 50, btnH = 26, gap = 6;
        for (int i = 0; i < 9; i++) {
            int r = i / 3, c = i % 3;
            int x = left + 20 + c * (btnW + gap);
            int y = startY + r * (btnH + gap);
            String d = String.valueOf(i + 1);
            this.addRenderableWidget(Button.builder(Component.literal(d), b -> appendDigit(d))
                    .bounds(x, y, btnW, btnH).build());
        }
        int row3 = startY + 3 * (btnH + gap);
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> backspace())
                .bounds(left + 20 + 0 * (btnW + gap), row3, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("0"), b -> appendDigit("0"))
                .bounds(left + 20 + 1 * (btnW + gap), row3, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.sixty_seconds.sixty_seconds.phone_dial"), b -> dial())
                .bounds(left + 20 + 2 * (btnW + gap), row3, btnW, btnH).build());
        int row4 = startY + 4 * (btnH + gap);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.sixty_seconds.sixty_seconds.phone_hangup"), b -> onClose())
                .bounds(left + 20, row4, 3 * btnW + 2 * gap, btnH).build());
    }

    private void appendDigit(String d) {
        if (numberField.getValue().length() < 6) {
            numberField.setValue(numberField.getValue() + d);
        }
    }

    private void backspace() {
        String v = numberField.getValue();
        if (!v.isEmpty()) numberField.setValue(v.substring(0, v.length() - 1));
    }

    private void dial() {
        ClientPlayNetworking.send(new PhoneDialC2SPacket(numberField.getValue()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = (this.height - 240) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, 0xFFFFFF);
    }
}
