package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.AbstractEntityAIInteractExtra;
import com.minecolonies.api.blocks.AbstractColonyBlock;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAISkill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_BREAK_SPEED;

@Mixin(value = AbstractEntityAIInteract.class, remap = false)
public abstract class AbstractEntityAIInteractMixin <J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractEntityAISkill<J, B> implements AbstractEntityAIInteractExtra {
    @Shadow(remap = false) private int stillTicks = 0;
    @Shadow(remap = false) private int previousIndex = 0;
    @Shadow(remap = false) private List<BlockPos> items;

    @Shadow(remap = false) public abstract int getBreakSpeedLevel();

    /**
     * Sets up some important skeleton stuff for every ai.
     *
     * @param job the job class.
     */
    protected AbstractEntityAIInteractMixin(@NotNull J job) {
        super(job);
    }

    public boolean isStillTicksExceeded(int limit){
        return  ++stillTicks > limit;
    }

    public void resetStillTick(){
        stillTicks = 0;
    }

    public boolean tryMoveForward(int currentIndex){
        if (currentIndex != previousIndex)
        {
            resetStillTick();
            previousIndex = currentIndex;
            return true;
        }
        return false;
    }

    public boolean checkPuckUpItems(){
        return items != null && items.isEmpty();
    }

    public void  resetPickUpItems(){
        items = null;
    }

    /**
     * @author ARxyt
     * @reason So weird, remastered.
     */
    @Inject(method = "getBlockMiningTime", at = @At("HEAD"), remap = false, cancellable = true)
    public void getBlockMiningTime(BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir)
    {
        if(MineColonies.getConfig().getServer().pvp_mode.get() && state.getBlock() instanceof AbstractColonyBlock<?>) {
            cir.setReturnValue(500);
            return;
        }
        float destroySpeed = 1.0F;
        if (worker.getMainHandItem() != null)
        {
            ItemStack stack = worker.getMainHandItem();
            destroySpeed = stack.getItem().getDestroySpeed(worker.getMainHandItem(), state);

            int eff = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);
            if (eff > 0 ) {
                destroySpeed += eff * eff + 1;
            }
        }

        MobEffectInstance haste = worker.getEffect(MobEffects.DIG_SPEED);
        if (haste != null) {
            int level = haste.getAmplifier() + 1;
            destroySpeed *= 1.0F + 0.2F * level;
        }

        MobEffectInstance fatigue = worker.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) {
            int level = fatigue.getAmplifier() + 1;
            destroySpeed *= Math.max(0.05F, 1.0F - 0.2F * level);
        }

        final double mutiplier = 1 + worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_BREAK_SPEED);

        cir.setReturnValue((int) (30 / (1 + Math.pow(getBreakSpeedLevel(), 2.0) / 100)
                * (double) world.getBlockState(pos).getDestroySpeed(world, pos) / (double) (destroySpeed)
                / mutiplier));
    }
}
