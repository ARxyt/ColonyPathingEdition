package com.arxyt.colonypathingedition.core.event;

import com.minecolonies.api.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class LargeBottleCauldronInteractions {

    @SubscribeEvent
    public void onRightClickCauldron(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!(state.getBlock() instanceof AbstractCauldronBlock)) {
            return;
        }

        InteractionResult result = InteractionResult.PASS;

        /* ========= 空瓶 → 水炼药锅 ========= */
        if (stack.is(ModItems.large_empty_bottle)
                && state.getBlock() instanceof LayeredCauldronBlock) {

            result = fillLargeBottle(state, level, pos, player, stack);
        }

        /* ========= 水瓶 → 空炼药锅 ========= */
        else if (stack.is(ModItems.large_water_bottle)
                && state.is(Blocks.CAULDRON)) {

            result = emptyLargeBottleToEmptyCauldron(level, pos, player, stack);
        }

        /* ========= 水瓶 → 水炼药锅 ========= */
        else if (stack.is(ModItems.large_water_bottle)
                && state.getBlock() instanceof LayeredCauldronBlock) {

            result = emptyLargeBottle(state, level, pos, player, stack);
        }

        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.sidedSuccess(level.isClientSide)
            );
        }
    }

    /* 从水炼药锅取水 */
    private InteractionResult fillLargeBottle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack stack
    ) {
        if (!level.isClientSide) {
            int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            ItemStack filled = new ItemStack(ModItems.large_water_bottle);

            if(!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(filled);
            }

            int newLevel = waterLevel - 2;
            if (newLevel > 0) {
                level.setBlock(
                        pos,
                        state.setValue(LayeredCauldronBlock.LEVEL, newLevel),
                        3
                );
            } else {
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }


    private InteractionResult emptyLargeBottleToEmptyCauldron(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack stack
    ) {
        if (!level.isClientSide) {
            ItemStack emptyed = new ItemStack(ModItems.large_empty_bottle);

            if(!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(emptyed);
            }

            level.setBlock(
                    pos,
                    Blocks.WATER_CAULDRON
                            .defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, 2),
                    3
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /* 把水倒进空炼药锅 */
    private InteractionResult emptyLargeBottle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack stack
    ) {
        if (!level.isClientSide) {
            int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            if(waterLevel >= 3){
                return InteractionResult.PASS;
            }
            ItemStack emptyed = new ItemStack(ModItems.large_empty_bottle);

            if(!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(emptyed);
            }

            int newLevel = waterLevel + 2;
            if(newLevel <= 3) {
                level.setBlock(
                        pos,
                        Blocks.WATER_CAULDRON
                                .defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, newLevel),
                        3
                );
            }
            else{
                level.setBlock(
                        pos,
                        Blocks.WATER_CAULDRON
                                .defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 3),
                        3
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
