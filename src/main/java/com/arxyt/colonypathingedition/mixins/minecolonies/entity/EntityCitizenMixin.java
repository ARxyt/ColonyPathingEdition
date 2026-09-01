package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.arxyt.colonypathingedition.core.util.SwitchUtils;
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
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.client.ItemParticleEffectMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Objects;

import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_INTERACTION_COOKIE;

@Mixin(value = EntityCitizen.class, remap = false)
public abstract class EntityCitizenMixin extends AbstractEntityCitizen {
    @Shadow(remap = false) private ICitizenColonyHandler citizenColonyHandler;

    @Shadow(remap = false) public abstract ITickRateStateMachine<IState> getCitizenAI();
    @Shadow(remap = false) public abstract ICitizenColonyHandler getCitizenColonyHandler();
    @Shadow(remap = false) public abstract ICitizenJobHandler getCitizenJobHandler();

    @Shadow
    private int interactionCooldown;

    public EntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void addAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        super.addAdditionalSaveData(compound);

        //市民全局状态信息
        compound.putString("aiState", getCitizenAI().getState().toString());
        if(getCitizenColonyHandler() != null && getCitizenColonyHandler().getColony() != null) {
            try {
                compound.putString("owner", getCitizenColonyHandler().getColony().getPermissions().getOwner().toString());
            }
            catch (Exception e) {
                Log.getLogger().error("We can't find owner for citizens!");
            }
        }
    }

    @Inject(method = "performMoveAway", at = @At("HEAD"), remap = false, cancellable = true)
    public void onPreformMoveAway(Entity attacker, CallbackInfo ci){
        if(getCitizenJobHandler().getColonyJob() instanceof JobNetherWorker jobNetherWorker && jobNetherWorker.isInNether()) {
            ci.cancel();
        }
    }

    /**
     * @author ARxyt
     * @reason Remove allowance of <1 damage caused by player.
     */
    @Overwrite(remap = false)
    private boolean checkIfValidDamageSource(final DamageSource source, final float damage)
    {
        if(source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return false;
        }

        if(getCitizenJobHandler().getColonyJob() instanceof JobNetherWorker jobNetherWorker && jobNetherWorker.isInNether() && !source.typeHolder().is(DamageSourceKeys.NETHER)) {
            return false;
        }

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
                citizenColonyHandler.getColonyOrRegister().getPermissions().addPlayer(permission.getOwner(), permission.getOwnerName(), permission.getRank(IPermissions.HOSTILE_RANK_ID));
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

    /**
     * @author ARxyt
     * @reason some addon.
     */
    @Overwrite(remap = false)
    private void childFoodInteraction(final ItemStack usedStack, final Player player, final InteractionHand hand)
    {
        if (usedStack.getDisplayName().getString().toLowerCase(Locale.US).contains("cookie"))
        {
            interactionCooldown = 100;

            if (!level().isClientSide())
            {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300));

                playSound(SoundEvents.GENERIC_EAT, 1.5f, (float) SoundUtils.getRandomPitch(getRandom()));
                new ItemParticleEffectMessage(usedStack.copy(), getX(), getY(), getZ(), getXRot(), getYRot(), getEyeHeight()).sendToTrackingEntity(this);
                SwitchUtils.consumeFoodSwitcher(usedStack, this, player);
            }
        }
        else
        {
            player.getInventory().removeItem(usedStack);
            player.drop(usedStack, true, true);
            if (!level().isClientSide())
            {
                playSound(SoundEvents.VILLAGER_NO, 1.0f, (float) SoundUtils.getRandomPitch(getRandom()));
                MessageUtils.format(MESSAGE_INTERACTION_COOKIE, this.getCitizenData().getName())
                        .withPriority(MessageUtils.MessagePriority.DANGER)
                        .sendTo(player);
            }
        }
    }
}
