package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.CitizenSkillDataCreator;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import com.minecolonies.core.util.ExperienceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Random;

import static com.minecolonies.api.util.constant.CitizenConstants.MAX_CITIZEN_LEVEL;
import static com.minecolonies.api.util.constant.Constants.MAX_BUILDING_LEVEL;

@Mixin(value = CitizenSkillHandler.class, remap = false)
public abstract class CitizenSkillHandlerMixin implements CitizenSkillDataCreator {
    @Shadow(remap = false) public Map<Skill, CitizenSkillHandler.SkillData> skillMap;

    @Shadow(remap = false) public abstract void init(int levelCap);
    @Shadow(remap = false) public abstract void levelUp(ICitizenData data);

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
            // Thus max = 49 + random factor ( <=10 ) = 59;  min = level base on parents.
            skillMap.get(skill).setLevel(levelBase + rand.nextInt(levelMax - levelBase + 1) + randomFactor);
        }
    }

    /**
     * @author ARxyt
     * @reason Weird, remastered
     */
    @Overwrite(remap = false)
    public void addXpToSkill(final Skill skill, final double xp, final ICitizenData data)
    {
        final CitizenSkillHandler.SkillData skillData = skillMap.getOrDefault(skill, CitizenSkillDataCreator.createSkillData(0, 0.0D));

        final IBuilding home = data.getHomeBuilding();

        final double citizenHutLevel = home == null ? 0 : home.getBuildingLevelEquivalent();
        final double citizenHutMaxLevel = home == null ? MAX_BUILDING_LEVEL : home.getMaxBuildingLevel();

        if (citizenHutLevel < citizenHutMaxLevel && MAX_CITIZEN_LEVEL * (citizenHutLevel + 1) / (citizenHutMaxLevel + 1) < skillData.getLevel())
        {
            return;
        }

        final int orgLevel = skillData.getLevel();
        double xpToLevelUp = Math.min(Double.MAX_VALUE, skillData.getExperience() + xp);
        while (xpToLevelUp > 0)
        {
            final double nextLevel = ExperienceUtils.getXPNeededForNextLevel(skillData.getLevel());
            if (nextLevel > xpToLevelUp)
            {
                skillData.setExperience(xpToLevelUp);
                break;
            }
            else
            {
                xpToLevelUp = xpToLevelUp - nextLevel;
                skillData.setLevel(skillData.getLevel() + 1);
            }
        }

        if (skillData.getLevel() > orgLevel)
        {
            levelUp(data);
            data.markDirty(10);
        }
    }

}
