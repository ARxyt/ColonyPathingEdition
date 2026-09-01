package com.arxyt.colonypathingedition.core.costants;


import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.jobs.*;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class AdditionalContants {
    public static final int TAG_UPDATE_INTERVAL = 200;

    public static final int IMMEDIATELY_DELAY = 1;
    public static final int WAITING_DELAY = 10;
    public static final int WALKING_DELAY = 20;
    public static final int ADVENTURE_DELAY = 40;
    public static final int STUCK_DELAY = 100;

    public static final String SICK_TIME = "sick_time";

    public static final Set<Class<?>> JOBS_FORCE_EAT_AT_HUT = Set.of(JobChef.class, JobCook.class, JobMiner.class);
    public static final Set<Class<?>> JOBS_EAT_IMMEDIATELY = Set.of(JobChef.class, JobCook.class);
    public static final Set<Class<?>> JOBS_MAY_NOT_SLEEP = Set.of(AbstractJobGuard.class, JobHealer.class);

    public static final String EATING_MODULE_ = "com.arxyt.colonypathingedition.core.eating.";

    public static final String UNSAFE_UPDATE = "com.arxyt.colonypathingedition.core.update.unstable";
    public static final String SAFE_UPDATE = "com.arxyt.colonypathingedition.core.update.stable";
    public static final String UPDATE_MESSAGE = "com.arxyt.colonypathingedition.core.update.latest";
    public static final String OUT_OF_DATE_MESSAGE = "com.arxyt.colonypathingedition.core.update.out_of_date";
    public static final String CHANGELOG = "com.arxyt.colonypathingedition.core.update.changelog";
    public static final String CLOSER = "com.arxyt.colonypathingedition.core.update.closer";
    public static final String HANDLE_ALERT = "com.arxyt.colonypathingedition.core.actions.mining.alert";
    public static final String HURT_ALERT = "com.arxyt.colonypathingedition.core.easycolony.colony.hurt.message";
    public static final String READ_MIND = "com.arxyt.colonypathingedition.core.easycolony.colony.read_mind.message";
    public static final String READ_MIND_STATE = "com.arxyt.colonypathingedition.core.easycolony.colony.read_mind.state";
    public static final String RESURRECT = "com.arxyt.colonypathingedition.core.easycolony.colony.resurrect";
    public static final String MODULE_WARN = "com.arxyt.colonypathingedition.core.module.no_such_module";

    public static final ResourceLocation PRECISE_FARMING = new ResourceLocation(Constants.MOD_ID, "effects/colonypathedition_precise_farming");
}
