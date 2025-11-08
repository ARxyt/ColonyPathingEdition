package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Random;

@Mixin(CitizenSkillHandler.class)
public abstract class CitizenSkillHandlerMixin {
    @Shadow(remap = false) public abstract void init(int levelCap);

    @Shadow(remap = false) public Map<Skill, CitizenSkillHandler.SkillData> skillMap;

    /**
     * @author ARxyt
     * @reason Weird, remastered
     */
    @Overwrite(remap = false)
    public void init(@NotNull final IColony colony, @Nullable final ICitizenData firstParent, @Nullable final ICitizenData secondParent, final Random rand)
    {
        ICitizenData roleModelA;
        ICitizenData roleModelB;

        if (firstParent == null)
        {
            roleModelA = colony.getCitizenManager().getRandomCitizen();
        }
        else
        {
            roleModelA = firstParent;
        }

        if (secondParent == null)
        {
            roleModelB = colony.getCitizenManager().getRandomCitizen();
        }
        else
        {
            roleModelB = secondParent;
        }

        // Serve as a random factor
        final int levelCap = (int) colony.getOverallHappiness();
        init(levelCap);

        for (final Skill skill : Skill.values())
        {
            final int firstRoleModelLevel = roleModelA.getCitizenSkillHandler().getSkills().get(skill).getLevel();
            final int secondRoleModelLevel = roleModelB.getCitizenSkillHandler().getSkills().get(skill).getLevel();
            // max = 49
            final int levelMax = (firstRoleModelLevel + secondRoleModelLevel) / 4;
            final int levelBase = Math.min(firstRoleModelLevel,secondRoleModelLevel) / 2;
            final int randomFactor = skillMap.get(skill).getLevel();
            // Thus max = 49 + random factor ( <=10 ) = 59  min = level base on parents.
            skillMap.get(skill).setLevel(levelBase + rand.nextInt(levelMax - levelBase + 1) + randomFactor);
        }
    }

}
