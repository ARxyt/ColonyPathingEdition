package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CitizenSkillHandler.SkillData.class, remap = false)
public interface CitizenSkillDataCreator {
    @Invoker("<init>")
    static CitizenSkillHandler.SkillData createSkillData (int level, double experience){
        throw new AssertionError();
    }
}
