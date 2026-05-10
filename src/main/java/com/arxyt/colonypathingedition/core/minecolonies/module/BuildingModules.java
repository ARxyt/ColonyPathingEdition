package com.arxyt.colonypathingedition.core.minecolonies.module;

import com.arxyt.colonypathingedition.core.colony.module.*;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;

public class BuildingModules {

    public static final BuildingEntry.ModuleProducer<TavernRecruitModule, TavernRecruitModuleView> TAVERN_RECRUIT =
        new BuildingEntry.ModuleProducer<>("consume_stats", TavernRecruitModule::new, () -> TavernRecruitModuleView::new);

    public static final BuildingEntry.ModuleProducer<FoodBlackListMenuModule, FoodBlackListMenuModuleView> FOOD_BLACK_LIST =
            new BuildingEntry.ModuleProducer<>("food_black_list", FoodBlackListMenuModule::new, () -> FoodBlackListMenuModuleView::new);

    public static void init(){

    }
}
