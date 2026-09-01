package com.arxyt.colonypathingedition.core.minecolonies.module;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;

import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModules.FOOD_BLACK_LIST;
import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModules.TAVERN_RECRUIT;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.*;

public class ModBuildingInitializer {
    public static void init() {
        if(PathingConfig.ADDITIONAL_MINIMUM_STOCK_MODULE.get()) {
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
        if(PathingConfig.TAVERN_ASSIGNMENT_MODULE.get()) {
            insertBefore(ModBuildings.tavern.get(), BED, TAVERN_RECRUIT);
        }
        if(PathingConfig.FOOD_BLACK_LIST_MODULE.get()) {
            insertBefore(ModBuildings.farmer.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.cook.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.hospital.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.bakery.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.deliveryman.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.kitchen.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.cowboy.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.shepherd.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.swineHerder.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.chickenHerder.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.beekeeper.get(), STATS_MODULE, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.wareHouse.get(), MIN_STOCK, FOOD_BLACK_LIST);
            insertBefore(ModBuildings.lumberjack.get(), MIN_STOCK, FOOD_BLACK_LIST);
        }
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
