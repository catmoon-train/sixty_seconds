package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.menu.SupplySearchMenu;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, net.exmo.sixty_seconds.SixtySeconds.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SupplySearchMenu>> SUPPLY_SEARCH =
            MENUS.register("supply_search",
                    () -> new MenuType<>(SupplySearchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<SpecialInventoryMenu>> SPECIAL_INVENTORY =
            MENUS.register("special_inventory",
                    () -> IMenuTypeExtension.create(SpecialInventoryMenu::fromNetwork));

    private ModMenuTypes() {
    }

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        MENUS.register(bus);
    }
}
