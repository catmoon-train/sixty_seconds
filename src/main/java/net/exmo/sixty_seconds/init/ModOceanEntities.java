package net.exmo.sixty_seconds.init;

import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

/** Compatibility aliases so copied ocean code can keep using ModOceanEntities. */
public final class ModOceanEntities {
    public static EntityType<OceanSharkEntity> OCEAN_SHARK;
    public static EntityType<OceanSeaMonsterEntity> OCEAN_SEA_MONSTER;

    public static void bind() {
        OCEAN_SHARK = ModEntities.OCEAN_SHARK;
        OCEAN_SEA_MONSTER = ModEntities.OCEAN_SEA_MONSTER;
    }

    public static void initAttributes() {
    }

    private ModOceanEntities() {
    }
}
