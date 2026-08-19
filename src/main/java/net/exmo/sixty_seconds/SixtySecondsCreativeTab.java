package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SixtySecondsCreativeTab {
    public static final ResourceKey<CreativeModeTab> SIXTY_SECONDS_GROUP = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, SixtySeconds.id("sixty_seconds"));

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SixtySeconds.MOD_ID);

    static {
        TABS.register("sixty_seconds", () -> CreativeModeTab.builder()
                .title(Component.translatable("item_group.sixty_seconds.sixty_seconds"))
                .icon(() -> new ItemStack(ModItems.SIXTY_SECONDS_CLOCK))
                .displayItems((parameters, output) -> {
                    ModItems.ITEMS.getEntries().forEach(holder -> output.accept(holder.get()));
                    ModBlocks.ITEMS.getEntries().forEach(holder -> output.accept(holder.get()));
                })
                .build());
    }

    private SixtySecondsCreativeTab() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    /** Kept so copied gameplay that called the no-arg Fabric register still compiles. */
    public static void register() {
    }
}
