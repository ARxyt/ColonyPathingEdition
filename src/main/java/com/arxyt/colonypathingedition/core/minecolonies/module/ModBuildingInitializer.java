package com.arxyt.colonypathingedition.core.minecolonies.module;

import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.*;

public class ModBuildingInitializer {
    public static void init() {
        insertBefore(ModBuildings.blacksmith.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.stoneMason.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.composter.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.crusher.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.deliveryman.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.sawmill.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.stoneSmelter.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.glassblower.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.dyer.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.fletcher.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.mechanic.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.plantation.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.concreteMixer.get(), STATS_MODULE, MIN_STOCK);
        insertBefore(ModBuildings.simpleQuarry.get(), SIMPLE_QUARRY, MIN_STOCK);
        insertBefore(ModBuildings.mediumQuarry.get(), MEDIUM_QUARRY, MIN_STOCK);
    }

    public static void insertBefore(
            BuildingEntry entry,
            BuildingEntry.ModuleProducer<?, ?> target,
            BuildingEntry.ModuleProducer<?, ?> ele
    ) {
        int index = entry.getModuleProducers().indexOf(target);
        entry.getModuleProducers().add(index, ele);
    }
}
