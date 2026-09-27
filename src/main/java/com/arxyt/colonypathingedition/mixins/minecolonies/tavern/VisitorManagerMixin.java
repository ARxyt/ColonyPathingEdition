package com.arxyt.colonypathingedition.mixins.minecolonies.tavern;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.managers.interfaces.IVisitorManager;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.util.EntityUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import com.minecolonies.core.colony.managers.VisitorManager;
import com.minecolonies.core.entity.visitor.VisitorCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.util.constant.Constants.SLIGHTLY_UP;
import static com.minecolonies.api.util.constant.PathingConstants.HALF_A_BLOCK;
import static com.minecolonies.api.util.constant.TranslationConstants.WARNING_COLONY_NO_ARRIVAL_SPACE;

@Mixin(value = VisitorManager.class, remap = false)
public abstract class VisitorManagerMixin implements IVisitorManager{
    @Final @Shadow(remap = false) private IColony colony;

    public <T extends ICivilianData> T spawnOrCreateCivilian(T data, final Level world, List<BlockPos> spawnPositions, final boolean force)
    {
        if (!colony.getServerBuildingManager().hasTownHall() || (!colony.getSettings().getSetting(BuildingTownHall.MOVE_IN).getValue() && !force))
        {
            return data;
        }

        if (colony.getServerBuildingManager().hasTownHall())
        {
            spawnPositions = new ArrayList<>(spawnPositions);
            spawnPositions.add(colony.getServerBuildingManager().getTownHall().getPosition());
        }

        for (final BlockPos spawnLocation : spawnPositions)
        {
            if (spawnLocation == null || spawnLocation.equals(BlockPos.ZERO))
            {
                continue;
            }

            if (WorldUtil.isEntityBlockLoaded(world, spawnLocation))
            {
                BlockPos calculatedSpawn = EntityUtils.getSpawnPoint(world, spawnLocation);
                if (calculatedSpawn != null)
                {
                    VisitorCitizen citizenEntity = (VisitorCitizen) ModEntities.VISITOR.create(colony.getWorld());

                    if (citizenEntity == null)
                    {
                        return data;
                    }

                    citizenEntity.setUUID(data.getUUID());
                    citizenEntity.setPos(calculatedSpawn.getX() + HALF_A_BLOCK, calculatedSpawn.getY() + SLIGHTLY_UP, calculatedSpawn.getZ() + HALF_A_BLOCK);
                    world.addFreshEntity(citizenEntity);

                    citizenEntity.setCitizenId(data.getId());
                    citizenEntity.getCitizenColonyHandler().setColonyId(colony.getID());
                    if (citizenEntity.isAddedToLevel())
                    {
                        citizenEntity.getCitizenColonyHandler().registerWithColony(data.getColony().getID(), data.getId());
                    }

                    return data;
                }
            }
        }

        if (colony.getServerBuildingManager().hasTownHall() && WorldUtil.isEntityBlockLoaded(world, colony.getServerBuildingManager().getTownHall().getPosition()))
        {
            final BlockPos townhallPos = colony.getServerBuildingManager().getTownHall().getPosition();
            MessageUtils.format(WARNING_COLONY_NO_ARRIVAL_SPACE, townhallPos.getX(), townhallPos.getY(), townhallPos.getZ()).sendTo(colony).forAllPlayers();
        }
        return data;
    }
}
