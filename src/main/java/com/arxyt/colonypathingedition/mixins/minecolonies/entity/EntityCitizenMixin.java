package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IGuardBuilding;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenColonyHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenJobHandler;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(EntityCitizen.class)
public abstract class EntityCitizenMixin extends AbstractEntityCitizen {
    @Shadow(remap = false) private ICitizenColonyHandler citizenColonyHandler;

    @Shadow(remap = false) public abstract ITickRateStateMachine<IState> getCitizenAI();
    @Shadow(remap = false) public abstract ICitizenColonyHandler getCitizenColonyHandler();
    @Shadow(remap = false) public abstract ICitizenJobHandler getCitizenJobHandler();

    public EntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        //市民全局状态信息
        tag.putString("aiState", getCitizenAI().getState().toString());
        if(getCitizenColonyHandler() != null && getCitizenColonyHandler().getColony() != null) {
            tag.putString("owner", getCitizenColonyHandler().getColony().getPermissions().getOwner().toString());
        }
    }

    /**
     * @author ARxyt
     * @reason Remove allowance of <1 damage caused by player.
     */
    @Overwrite(remap = false)
    private boolean checkIfValidDamageSource(final DamageSource source, final float damage)
    {
        final Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof EntityCitizen)
        {
            if (((EntityCitizen) sourceEntity).getCitizenColonyHandler().getColonyId() == citizenColonyHandler.getColonyId())
            {
                return false;
            }

            final IColony attackerColony = ((EntityCitizen) sourceEntity).getCitizenColonyHandler().getColonyOrRegister();
            if (attackerColony != null && citizenColonyHandler.getColonyOrRegister() != null)
            {
                final IPermissions permission = attackerColony.getPermissions();
                citizenColonyHandler.getColonyOrRegister().getPermissions().addPlayer(permission.getOwner(), permission.getOwnerName(), permission.getRank(permission.HOSTILE_RANK_ID));
            }
        }

        if (sourceEntity instanceof Player)
        {
            if (sourceEntity instanceof ServerPlayer)
            {
                if (!Objects.requireNonNull(getCitizenColonyHandler().getColonyOrRegister()).getPermissions().hasPermission((Player) sourceEntity, Action.HURT_CITIZEN))
                {
                    return false;
                }
                if (getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard)
                {
                    return IGuardBuilding.checkIfGuardShouldTakeDamage(this, (Player) sourceEntity);
                }
            }
            else
            {
                final IColonyView colonyView = IColonyManager.getInstance().getColonyView(getCitizenColonyHandler().getColonyId(), level().dimension());
                return colonyView == null || colonyView.getPermissions().hasPermission((Player) sourceEntity, Action.HURT_CITIZEN);
            }
        }

        return true;
    }
}
