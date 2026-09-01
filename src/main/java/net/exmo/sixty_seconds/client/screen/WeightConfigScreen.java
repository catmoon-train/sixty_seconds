package net.exmo.sixty_seconds.client.screen;

import net.exmo.sixty_seconds.client.WeightConfigClient;
import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.network.WeightConfigSaveC2SPacket;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负重快速配置面板（客户端）。编辑物品重量与全局参数后通过 {@link WeightConfigSaveC2SPacket} 存回服务端。
 */
public class WeightConfigScreen extends Screen {

    private final SixtySecondsWeightConfig config;

    private final List<ItemWeightEntry> entries = new ArrayList<>();
    private final List<EditBox> allEditBoxes = new ArrayList<>();
    private EditBox focused = null;

    private EditBox backpackField, handField, maxLoadField, penaltyField, defaultField, addIdField, addWeightField;
    private Button enabledButton;

    private int listX, listY, listW, listH, scroll;

    public WeightConfigScreen(SixtySecondsWeightConfig cfg) {
        super(Component.translatable("message.sixty_seconds.weight.config_title"));
        this.config = copy(cfg);
    }

    private static SixtySecondsWeightConfig copy(SixtySecondsWeightConfig c) {
        SixtySecondsWeightConfig r = new SixtySecondsWeightConfig();
        r.enabled = c.enabled;
        r.backpackMultiplier = c.backpackMultiplier;
        r.handMultiplier = c.handMultiplier;
        r.maxLoad = c.maxLoad;
        r.speedPenaltyEnabled = c.speedPenaltyEnabled;
        r.speedPenaltyPerLoad = c.speedPenaltyPerLoad;
        r.defaultWeight = c.defaultWeight;
        r.tagWeights = new LinkedHashMap<>(c.tagWeights);
        r.itemWeights = new LinkedHashMap<>(c.itemWeights);
        return r;
    }

    @Override
    protected void init() {
        int w = this.width, h = this.height;
        int panelX = Math.max(10, w / 2 - 200);
        int panelW = Math.min(400, w - 20);
        listX = panelX;
        listY = 110;
        listW = panelW;
        listH = h - listY - 80;
        scroll = 0;

        enabledButton = Button.builder(enabledText(), b -> {
            config.enabled = !config.enabled;
            b.setMessage(enabledText());
        }).bounds(panelX, 28, 120, 20).build();
        this.addRenderableWidget(enabledButton);

        backpackField = numField(panelX + 150, 28, config.backpackMultiplier);
        handField = numField(panelX, 54, config.handMultiplier);
        maxLoadField = numField(panelX + 150, 54, config.maxLoad);
        penaltyField = numField(panelX, 80, config.speedPenaltyPerLoad);
        defaultField = numField(panelX + 150, 80, config.defaultWeight);

        for (Map.Entry<String, Double> e : config.itemWeights.entrySet()) {
            addEntry(e.getKey(), e.getValue());
        }

        addIdField = new EditBox(this.font, panelX, h - 66, 200, 18, Component.literal("id"));
        addIdField.setHint(Component.translatable("message.sixty_seconds.weight.hint_item_id"));
        addWeightField = new EditBox(this.font, panelX + 205, h - 66, 60, 18, Component.literal("w"));
        addWeightField.setHint(Component.translatable("message.sixty_seconds.weight.hint_weight"));
        this.addRenderableWidget(addIdField);
        this.addRenderableWidget(addWeightField);
        allEditBoxes.add(addIdField);
        allEditBoxes.add(addWeightField);

        Button addBtn = Button.builder(Component.translatable("message.sixty_seconds.weight.button_add"), b -> addItem())
                .bounds(panelX + 270, h - 66, 60, 18).build();
        Button saveBtn = Button.builder(Component.translatable("message.sixty_seconds.weight.button_save"), b -> save())
                .bounds(panelX, h - 40, 120, 20).build();
        Button cancelBtn = Button.builder(Component.translatable("message.sixty_seconds.weight.button_close"), b -> this.onClose())
                .bounds(panelX + 280, h - 40, 120, 20).build();
        this.addRenderableWidget(addBtn);
        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private EditBox numField(int x, int y, double val) {
        EditBox box = new EditBox(this.font, x, y, 120, 18, Component.literal("num"));
        box.setValue(String.valueOf(val));
        this.addRenderableWidget(box);
        allEditBoxes.add(box);
        return box;
    }

    private Component enabledText() {
        return config.enabled ? Component.translatable("message.sixty_seconds.weight.enabled_on")
                : Component.translatable("message.sixty_seconds.weight.enabled_off");
    }

    private void addEntry(String id, double val) {
        entries.add(new ItemWeightEntry(id, val));
        allEditBoxes.add(entries.get(entries.size() - 1).weight);
    }

    private void addItem() {
        String id = addIdField.getValue().trim();
        if (id.isEmpty()) return;
        double v;
        try {
            v = Double.parseDouble(addWeightField.getValue().trim().isEmpty() ? "1.0" : addWeightField.getValue().trim());
        } catch (NumberFormatException ex) {
            v = 1.0;
        }
        config.itemWeights.put(id, v);
        addEntry(id, v);
        addIdField.setValue("");
        addWeightField.setValue("");
    }

    private void save() {
        try {
            config.backpackMultiplier = Double.parseDouble(backpackField.getValue());
            config.handMultiplier = Double.parseDouble(handField.getValue());
            config.maxLoad = Double.parseDouble(maxLoadField.getValue());
            config.speedPenaltyPerLoad = Double.parseDouble(penaltyField.getValue());
            config.defaultWeight = Double.parseDouble(defaultField.getValue());
        } catch (NumberFormatException ignored) {
        }
        for (ItemWeightEntry e : entries) {
            e.commit(config);
        }
        ClientPlayNetworking.send(new WeightConfigSaveC2SPacket(config));
        WeightConfigClient.set(config);
        this.onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, entries.size() * 18 - listH);
        scroll = (int) Math.max(0, Math.min(max, scroll - sy * 12));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (EditBox box : allEditBoxes) {
            if (box.mouseClicked(mx, my, btn)) {
                focused = box;
                return true;
            }
        }
        int i = 0;
        for (ItemWeightEntry e : entries) {
            int rowY = listY + 4 - scroll + i * 18;
            if (rowY >= listY - 18 && rowY <= listY + listH) {
                e.weight.setPosition(listX + 220, rowY);
                if (mx >= listX + 220 && mx <= listX + 284 && my >= rowY && my <= rowY + 16) {
                    e.weight.mouseClicked(mx, my, btn);
                    focused = e.weight;
                    return true;
                }
                if (mx >= listX + 300 && mx <= listX + 316 && my >= rowY && my <= rowY + 16) {
                    config.itemWeights.remove(e.id);
                    entries.remove(e);
                    allEditBoxes.remove(e.weight);
                    return true;
                }
            }
            i++;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (focused != null && focused.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (focused != null && focused.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float delta) {
        this.renderBackground(gui, mx, my, delta);
        gui.drawCenteredString(this.font,
                Component.translatable("message.sixty_seconds.weight.header"),
                this.width / 2, 8, 0xFFFFFF);
        super.render(gui, mx, my, delta);

        int i = 0;
        for (ItemWeightEntry e : entries) {
            int rowY = listY + 4 - scroll + i * 18;
            if (rowY < listY - 18 || rowY > listY + listH) {
                i++;
                continue;
            }
            if (!e.icon.isEmpty()) {
                gui.renderItem(e.icon, listX, rowY);
            }
            gui.drawString(this.font, Component.literal(e.id), listX + 22, rowY + 4, 0xFFFFFF);
            e.weight.setPosition(listX + 220, rowY);
            e.weight.render(gui, mx, my, delta);
            gui.drawString(this.font, Component.literal("x"), listX + 304, rowY + 4, 0xFF6666);
            i++;
        }
    }

    public static class ItemWeightEntry {
        final String id;
        final EditBox weight;
        final ItemStack icon;

        ItemWeightEntry(String id, double val) {
            this.id = id;
            ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
            this.icon = BuiltInRegistries.ITEM.getOptional(rl).map(ItemStack::new).orElse(ItemStack.EMPTY);
            this.weight = new EditBox(Minecraft.getInstance().font, 0, 0, 64, 16, Component.literal("w"));
            this.weight.setValue(String.valueOf(val));
        }

        void commit(SixtySecondsWeightConfig cfg) {
            try {
                cfg.itemWeights.put(id, Double.parseDouble(weight.getValue()));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
