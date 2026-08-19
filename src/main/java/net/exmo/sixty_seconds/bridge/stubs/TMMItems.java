package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class TMMItems {
    private TMMItems() {}
    public static Item REVOLVER = Items.AIR;
    public static Item STANDARD_REVOLVER = Items.AIR;
    public static Item DERRINGER = Items.AIR;
    public static Item SNIPER_RIFLE = Items.AIR;
    public static Item NOTE = Items.PAPER;
    public static void bind() {
        if (ModItems.SIXTY_SECONDS_PISTOL != null) {
            REVOLVER = ModItems.SIXTY_SECONDS_PISTOL;
            STANDARD_REVOLVER = ModItems.SIXTY_SECONDS_PISTOL;
            DERRINGER = ModItems.SIXTY_SECONDS_PISTOL;
            SNIPER_RIFLE = ModItems.SIXTY_SECONDS_SNIPER;
        }
        if (ModItems.SIXTY_SECONDS_DRAFT_PAPER != null) {
            NOTE = ModItems.SIXTY_SECONDS_DRAFT_PAPER;
        }
    }
}
