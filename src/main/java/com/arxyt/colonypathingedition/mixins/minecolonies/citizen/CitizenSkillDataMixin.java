package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.arxyt.colonypathingedition.api.SkillDataExtra;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CitizenSkillHandler.SkillData.class, remap = false)
public class CitizenSkillDataMixin implements SkillDataExtra {
    @Shadow(remap = false) private double experience;

    @Unique
    public void setExperience(double experience) {
        this.experience = experience;
    }
}
