package com.arxyt.colonypathingedition.core.colony.module;

import static com.minecolonies.api.util.constant.StatisticsConstants.VISITORS_ABSCONDED;
import static com.minecolonies.api.util.constant.StatisticsConstants.VISITORS_RECRUITED;
import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_RECRUITMENT_RAN_OFF;
import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_RECRUITMENT_SUCCESS;
import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_RECRUITMENT_SUCCESS_CUSTOM;
import static com.minecolonies.api.util.constant.TranslationConstants.WARNING_NO_COLONY_SPACE;
import static com.minecolonies.api.util.constant.TranslationConstants.WARNING_RECRUITMENT_INSUFFICIENT_ITEMS;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IVisitorData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.eventbus.events.colony.citizens.CitizenAddedModEvent;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.wrapper.InvWrapper;

public class TavernRecruitModule extends AbstractBuildingModule implements IBuildingModule {

    private static final int BAD_VISITOR_CHANCE = 2;

    @Override
    public void serializeToView(FriendlyByteBuf buf) {
        List<Integer> externalCitizens = this.building.getModule(BuildingModules.TAVERN_VISITOR).getExternalCitizens();
        // 这里必须添加过滤，因为殖民地中访客管理器的数据和建筑模块中的数据过度分布式储存
        // 导致在雇佣后可能出现不一致的情况。
        externalCitizens = externalCitizens.stream().filter(citizenId -> building.getColony().getVisitorManager().getVisitor(citizenId) != null).toList();
        buf.writeInt(externalCitizens.size());
        externalCitizens.forEach(citizen -> {
            IVisitorData visitor = building.getColony().getVisitorManager().getVisitor(citizen);
            buf.writeInt(visitor.getId());
            Map<Skill, CitizenSkillHandler.SkillData> skills = visitor.getCitizenSkillHandler().getSkills();
            for (Skill skill : Skill.values()) {
                buf.writeInt(skills.get(skill).getLevel());
            }
            buf.writeUtf(visitor.getName());
            buf.writeItem(visitor.getRecruitCost());
        });
    }

    public void recruit(int visitorId, Player player) {
        // 下面一整段代码直接取自模拟殖民地的RecruitmentInteraction，
        // 由于其所封装的交互系统具有高度的封装，可能是我水平不够，
        // 不知道怎么单独抽离出来，所以这里直接复制了
        IColony colony = building.getColony();
        IVisitorData data = colony.getVisitorManager().getVisitor(visitorId);

        if (building.getColony().getCitizenManager().getCurrentCitizenCount() < colony.getCitizenManager().getPotentialMaxCitizens()) {
            if (player.isCreative() || InventoryUtils.attemptReduceStackInItemHandler(new InvWrapper(player.getInventory()),
                data.getRecruitCost(),
                data.getRecruitCost().getCount(), true, true)
            ) {
                markDirty();

                // Recruits visitor as new citizen and respawns entity
                colony.getVisitorManager().removeCivilian(data);
                data.setHomeBuilding(null);
                data.setJob(null);

                final IBuilding tavern = colony.getBuildingManager().getFirstBuildingMatching(b -> b.getBuildingType() == ModBuildings.tavern.get());

                if (colony.getWorld().random.nextInt(100) <= BAD_VISITOR_CHANCE) {
                    StatsUtil.trackStat(tavern, VISITORS_ABSCONDED, 1);
                    colony.getStatisticsManager().increment(VISITORS_ABSCONDED, colony.getDay());

                    MessageUtils.format(MESSAGE_RECRUITMENT_RAN_OFF, data.getName()).sendTo(colony).forAllPlayers();
                    return;
                }
                StatsUtil.trackStat(tavern, VISITORS_RECRUITED, 1);
                colony.getStatisticsManager().increment(VISITORS_RECRUITED, colony.getDay());

                // Create and read new citizen
                ICitizenData newCitizen = colony.getCitizenManager().createAndRegisterCivilianData();
                newCitizen.deserializeNBT(data.serializeNBT());
                newCitizen.setParents("", "");
                newCitizen.setLastPosition(data.getLastPosition());

                // Exchange entities
                newCitizen.updateEntityIfNecessary();
                data.getEntity().ifPresent(e -> e.remove(Entity.RemovalReason.DISCARDED));

                if (data.hasCustomTexture()) {
                    MessageUtils.format(MESSAGE_RECRUITMENT_SUCCESS_CUSTOM, data.getName()).sendTo(colony).forAllPlayers();
                } else {
                    MessageUtils.format(MESSAGE_RECRUITMENT_SUCCESS, data.getName()).sendTo(colony).forAllPlayers();
                }

                IMinecoloniesAPI.getInstance()
                    .getEventBus()
                    .post(new CitizenAddedModEvent(newCitizen, CitizenAddedModEvent.CitizenAddedSource.HIRED));
            } else {
                MessageUtils.format(WARNING_RECRUITMENT_INSUFFICIENT_ITEMS).sendTo(player);
            }
        } else {
            MessageUtils.format(WARNING_NO_COLONY_SPACE).sendTo(player);
        }
    }
}
