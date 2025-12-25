package com.arxyt.colonypathingedition.core.minecolonies.module;

import com.arxyt.colonypathingedition.core.colony.module.TavernRecruitModule;
import com.arxyt.colonypathingedition.core.colony.module.TavernRecruitModuleView;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;

public class BuildingModels {

    public static final BuildingEntry.ModuleProducer<TavernRecruitModule, TavernRecruitModuleView> TAVERN_RECRUIT =
        new BuildingEntry.ModuleProducer<>("consume_stats", TavernRecruitModule::new, () -> TavernRecruitModuleView::new);

}
