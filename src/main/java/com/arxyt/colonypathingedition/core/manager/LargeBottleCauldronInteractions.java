package com.arxyt.colonypathingedition.core.manager;

import com.minecolonies.api.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LargeBottleCauldronInteractions {
    private LargeBottleCauldronInteractions() {}

    /** 在 common setup 阶段调用 */
    public static void register() {

        /* ========= 空瓶 → 水炼药锅 ========= */
        CauldronInteraction.WATER.put(
                ModItems.large_empty_bottle,
                LargeBottleCauldronInteractions::fillLargeBottle
        );

        /* ========= 水瓶 → 空炼药锅 ========= */
        CauldronInteraction.EMPTY.put(
                ModItems.large_water_bottle,
                LargeBottleCauldronInteractions::emptyLargeBottleToEmptyCauldron
        );
        CauldronInteraction.WATER.put(
                ModItems.large_water_bottle,
                LargeBottleCauldronInteractions::emptyLargeBottle
        );
    }

    /* 从水炼药锅取水 */
    private static InteractionResult fillLargeBottle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
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


    private static InteractionResult emptyLargeBottleToEmptyCauldron(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
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
    private static InteractionResult emptyLargeBottle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
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
