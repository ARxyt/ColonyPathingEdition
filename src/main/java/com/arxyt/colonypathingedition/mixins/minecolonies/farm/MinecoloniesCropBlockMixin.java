package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.minecolonies.api.blocks.AbstractBlockMinecolonies;
import com.minecolonies.core.blocks.MinecoloniesCropBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = MinecoloniesCropBlock.class, remap = false)
public abstract class MinecoloniesCropBlockMixin extends AbstractBlockMinecolonies<MinecoloniesCropBlock> {

    public MinecoloniesCropBlockMixin()
    {
        super(Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
    }

    /**
     * @author ARxyt
     * @reason There is bugs when add broken state into updateShape directly. So we overwrite it.
     */
    @Overwrite(remap = false)
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction dir, @NotNull BlockState newState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos)
    {
        return super.updateShape(state, dir, newState, level, pos, neighborPos);
    }
}
