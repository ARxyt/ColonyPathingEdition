package com.arxyt.colonypathingedition.core.ai.minimal;

import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.AIOneTimeEventTarget;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIFlee.NewFleeStates.*;
import static com.minecolonies.api.util.constant.CitizenConstants.MAX_GUARD_CALL_RANGE;

public class NewEntityAIFlee implements IStateAI {
    /**
     * The amount of area checks before the citizen assumes it is safe. 40 are done in 10seconds.
     */
    private static final int CHECKS_BEFORE_SAFE = 20;

    /**
     * Move away distances.
     */
    private static final float MOVE_AWAY_DIST = 20;
    private static final int KEEP_AWAY_DIST = 5;
    public static final float SPREAD_FEAR_DIST_X = 10;
    public static final float SPREAD_FEAR_DIST_Y = 5;

    /**
     * The entity we are attached to.
     */
    private final EntityCitizen           citizen;
    private final double                  fasterSpeed;
    private final Class<? extends Entity> targetEntityClass;

    @Nullable
    private Entity closestLivingEntity;

    /**
     * Time spent fleeing.
     */
    private int safeTime = 0;

    /**
     * The pathresult of trying to move away
     */
    private PathResult<?> moveAwayPath;

    public enum NewFleeStates implements IState
    {
        CHECK_ENTITIES,
        RUNNING,
        STAY_IN_SAFE_PLACE
    }

    /**
     * The blockpos from where the citizen started fleeing.
     */
    private BlockPos startingPos;

    /**
     * Constructor.
     *
     * @param entity             current entity.
     * @param targetEntityClass  entity class we want to avoid.
     * @param fasterSpeed        how fast we should flee.
     */
    public NewEntityAIFlee(
            @NotNull final EntityCitizen entity, @NotNull final Class<? extends Entity> targetEntityClass,
            final double fasterSpeed)
    {
        super();
        this.citizen = entity;
        this.startingPos = entity.blockPosition();
        this.targetEntityClass = targetEntityClass;
        this.fasterSpeed = fasterSpeed;

        citizen.getCitizenAI().addTransition(new AITarget<>(CitizenAIState.FLEE, () -> true, () -> {
            startingPos = citizen.blockPosition();
            return CHECK_ENTITIES;
        }, 1));
        citizen.getCitizenAI().addTransition(new AITarget<>(CHECK_ENTITIES, () -> true, this::isEntityClose, 5));
        citizen.getCitizenAI().addTransition(new AITarget<>(RUNNING, this::updateMoving, () -> CHECK_ENTITIES, 5));
        citizen.getCitizenAI().addTransition(new AITarget<>(STAY_IN_SAFE_PLACE, this::updateMoving, () -> CHECK_ENTITIES, 5));
    }

    /**
     * Check for close entities
     *
     * @return IState of NewFleeStates or IDLE
     */
    public IState isEntityClose()
    {
        final Entity currentClosest = getClosestToAvoid();
        if (currentClosest != null)
        {
            if (closestLivingEntity == null || currentClosest.getId() != closestLivingEntity.getId())
            {
                // Calling for help for the new enemy
                citizen.callForHelp(currentClosest, MAX_GUARD_CALL_RANGE);
            }
            closestLivingEntity = currentClosest;
            safeTime = 0;
            performMoveAway();
            citizen.getCitizenAI().setCurrentDelay(1);
            if(currentClosest instanceof LivingEntity livingEntity && !(livingEntity.hasEffect(MobEffects.GLOWING))) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 15));
            }
            return RUNNING;
        }

        safeTime++;

        if (safeTime > CHECKS_BEFORE_SAFE)
        {
            reset();
            return CitizenAIState.IDLE;
        }

        return CHECK_ENTITIES;
    }

    /**
     * Returns the closest entity to avoid.
     *
     * @return Entity to avoid.
     */
    private Entity getClosestToAvoid()
    {

        if (targetEntityClass == Player.class)
        {
            return CompatibilityUtils.getWorldFromCitizen(citizen).getNearestPlayer(citizen, MOVE_AWAY_DIST);
        }
        else
        {
            float healthMultiplier = 1 + 2 * (citizen.getMaxHealth() - citizen.getHealth()) / citizen.getMaxHealth();
            final Optional<Entity> entityOptional = CompatibilityUtils.getWorldFromCitizen(citizen).getEntities(
                            citizen,
                            citizen.getBoundingBox().inflate(
                                    MOVE_AWAY_DIST * healthMultiplier,
                                    3.0D,
                                    MOVE_AWAY_DIST * healthMultiplier),
                            Entity::isAlive)
                    .stream()
                    .filter(targetEntityClass::isInstance)
                    .findFirst();
            return entityOptional.orElse(null);
        }
    }

    /**
     * Makes entity move away from {@link #closestLivingEntity}.
     *
     * @return whether the citizen started moving away.
     */
    private boolean performMoveAway()
    {
        float healthMultiplier = 1 + 2 * (citizen.getMaxHealth() - citizen.getHealth()) / citizen.getMaxHealth();
        if ((moveAwayPath == null || !moveAwayPath.isInProgress()) && citizen.getNavigation().isDone() && closestLivingEntity != null)
        {
            EntityNavigationUtils.walkAwayFrom(citizen,
                    closestLivingEntity.blockPosition(),
                    Math.max((int)(MOVE_AWAY_DIST * healthMultiplier), KEEP_AWAY_DIST),
                    fasterSpeed);
            moveAwayPath = citizen.getNavigation().getPathResult();
            return true;
        }
        return false;
    }

    /**
     * Updates the task.
     *
     * @return false if the citizen is fleeing.
     */
    private boolean updateMoving()
    {
        citizen.playMoveAwaySound();
        SpreadFear();

        if (moveAwayPath == null || !moveAwayPath.isInProgress())
        {
            citizen.getCitizenAI().setCurrentDelay(1);
            return true;
        }
        else
        {
            float healthMultiplier = 1 + 2 * (citizen.getMaxHealth() - citizen.getHealth()) / citizen.getMaxHealth();
            if (closestLivingEntity == null || citizen.distanceTo(closestLivingEntity) > MOVE_AWAY_DIST * healthMultiplier)
            {
                citizen.getNavigation().setSpeedModifier(1);
            }
            else
            {
                citizen.getNavigation().setSpeedModifier(fasterSpeed);
            }
        }
        return false;
    }

    /**
     * Spread fear.
     */
    private void SpreadFear()
    {
        List<Entity> toSpreadFear = CompatibilityUtils.getWorldFromCitizen(citizen).getEntities(
                        citizen,
                        citizen.getBoundingBox().inflate(
                                SPREAD_FEAR_DIST_X,
                                SPREAD_FEAR_DIST_Y,
                                SPREAD_FEAR_DIST_X),
                        Entity::isAlive)
                .stream()
                .filter(entity -> entity instanceof EntityCitizen aCitizen && !(aCitizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard<?>))
                .toList();
        for(Entity entity : toSpreadFear) {
            if(entity instanceof EntityCitizen aCitizen) {
                aCitizen.getCitizenAI().addTransition(new AIOneTimeEventTarget<>(CitizenAIState.FLEE));
            }
        }
    }

    /**
     * Resets the task.
     */
    public void reset()
    {
        safeTime = 0;
        if (startingPos != null)
        {
            EntityNavigationUtils.walkToPos(citizen, startingPos, 1, true);
        }
        closestLivingEntity = null;
        moveAwayPath = null;
        startingPos = null;
    }
}
