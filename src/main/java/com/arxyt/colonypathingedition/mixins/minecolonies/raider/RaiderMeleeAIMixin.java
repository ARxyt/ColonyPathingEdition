package com.arxyt.colonypathingedition.mixins.minecolonies.raider;

import com.minecolonies.api.entity.ai.combat.threat.IThreatTableEntity;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.combat.AttackMoveAI;
import com.minecolonies.core.entity.mobs.aitasks.RaiderMeleeAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.GUARD_FLEE;

@Mixin(RaiderMeleeAI.class)
public class RaiderMeleeAIMixin<T extends AbstractEntityMinecoloniesMonster & IThreatTableEntity> extends AttackMoveAI<T> {
    public RaiderMeleeAIMixin(
            final T owner,
            final ITickRateStateMachine<IState> stateMachine)
    {
        super(owner, stateMachine);
    }

    /**
     * @author ARxyt
     * 降低逃跑卫兵的威胁度
     */
    @Override
    protected boolean checkForTarget()
    {
        if (target != null && target.isAlive())
        {
            if(target instanceof AbstractEntityCitizen citizen){
                if(citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() == GUARD_FLEE){
                    user.getThreatTable().addThreat(target, 20 - user.getThreatTable().getThreatFor(target));
                }
            }
            else if(target instanceof Player player){
                if(player.isCreative()||player.isSpectator()){
                    resetTarget();
                    return false;
                }
            }
        }
        return super.checkForTarget();
    }

    /**
     * @author ARxyt
     * 初始化一部分威胁等级，让卫兵能够优先拉到仇恨，同时取消无目标时才能进入此算法的设定，野蛮人现在可以动态确定威胁目标
     */
    @Override
    protected boolean searchNearbyTarget()
    {
        final List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, getSearchArea());

        if (entities.isEmpty())
        {
            return false;
        }

        boolean foundTarget = false;
        for (final LivingEntity entity : entities)
        {
            if (!entity.isAlive())
            {
                continue;
            }

            if (skipSearch(entity))
            {
                return false;
            }

            if (user.getThreatTable().getThreatFor(user) != -1){
                continue;
            }

            if (isEntityValidTarget(entity) )
            {
                if (entity instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() != GUARD_FLEE){
                    user.getThreatTable().addThreat(entity, 40);
                }
                else if( user.getSensing().hasLineOfSight(entity)) {
                    if (entity instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard){
                        user.getThreatTable().addThreat(entity, 20);
                    }
                    else if (entity instanceof Player player && !(player.isCreative() || player.isSpectator())){
                        user.getThreatTable().addThreat(entity, 20);
                    }
                    else{
                        user.getThreatTable().addThreat(entity, 0);
                    }
                }
                else
                foundTarget = true;
            }
        }

        return foundTarget;
    }

    /**
     * @author ARxyt
     * @reason 这样改方便点，判定也少，将游客纳入野蛮人自然仇恨对象。
     */
    @Overwrite(remap = false)
    protected boolean isAttackableTarget(final LivingEntity target)
    {
        return (target instanceof AbstractEntityCitizen && !target.isInvisible()) || (target instanceof Player && !((Player) target).isCreative() && !target.isSpectator());
    }
}
