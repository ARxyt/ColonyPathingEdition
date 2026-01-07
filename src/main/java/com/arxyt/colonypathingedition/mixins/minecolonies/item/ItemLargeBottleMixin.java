package com.arxyt.colonypathingedition.mixins.minecolonies.item;

import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.items.ItemLargeBottle;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ItemLargeBottle.class, remap = false)
public abstract class ItemLargeBottleMixin extends Item {

    public ItemLargeBottleMixin(@NotNull final Item.Properties builder)
    {
        super(builder);
    }

    /**
     * @author ARxyt
     * @reason No More Cooldowns
     */
    @Overwrite
    public @NotNull InteractionResult interactLivingEntity(
            @NotNull final ItemStack stack,
            @NotNull final Player player,
            @NotNull final LivingEntity entity,
            @NotNull final InteractionHand hand)
    {
        if (this != ModItems.large_empty_bottle)
        {
            return super.interactLivingEntity(stack, player, entity, hand);
        }

        if (entity instanceof Cow && !entity.isBaby())
        {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            if (!InventoryUtils.addItemStackToItemHandler(new PlayerMainInvWrapper(player.getInventory()), ModItems.large_milk_bottle.getDefaultInstance()))
            {
                player.drop(ModItems.large_milk_bottle.getDefaultInstance(), false);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        else if (entity instanceof Goat && !entity.isBaby())
        {
            player.playSound(SoundEvents.GOAT_MILK, 1.0F, 1.0F);
            if (!InventoryUtils.addItemStackToItemHandler(new PlayerMainInvWrapper(player.getInventory()), ModItems.large_milk_bottle.getDefaultInstance()))
            {
                player.drop(ModItems.large_milk_bottle.getDefaultInstance(), false);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(stack, player, entity, hand);
    }

    /**
     * @author ARxyt
     * @reason No More Cooldowns / React with cauldron
     */
    @Overwrite
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull final Level level, final Player player, @NotNull final InteractionHand hand)
    {
        final ItemStack itemstack = player.getItemInHand(hand);
        if (this != ModItems.large_empty_bottle)
        {
            return InteractionResultHolder.pass(itemstack);
        }

        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.MISS)
        {
            if (blockhitresult.getType() == HitResult.Type.BLOCK)
            {
                BlockPos blockpos = blockhitresult.getBlockPos();
                if (!level.mayInteract(player, blockpos))
                {
                    return InteractionResultHolder.pass(itemstack);
                }

                if (level.getFluidState(blockpos).is(FluidTags.WATER))
                {
                    level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    if (!InventoryUtils.addItemStackToItemHandler(new PlayerMainInvWrapper(player.getInventory()), ModItems.large_water_bottle.getDefaultInstance()))
                    {
                        player.drop(ModItems.large_water_bottle.getDefaultInstance(), false);
                    }
                    itemstack.shrink(1);
                    return InteractionResultHolder.success(itemstack);
                }
            }
        }
        return InteractionResultHolder.pass(itemstack);
    }
}
