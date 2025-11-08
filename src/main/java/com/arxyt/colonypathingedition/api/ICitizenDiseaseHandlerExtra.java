package com.arxyt.colonypathingedition.api;

import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenDiseaseHandler;

// 战争遗产，先留着，怕之后有用
public interface ICitizenDiseaseHandlerExtra extends ICitizenDiseaseHandler {
    int getSickTime();
    void setSickTime(int sickTime);
}
