package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.core.window.NewWindowField;
import com.minecolonies.api.blocks.huts.AbstractBlockMinecoloniesDefault;
import com.minecolonies.api.blocks.interfaces.IBuildingBrowsableBlock;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.tileentities.TileEntityScarecrow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.core.blocks.BlockScarecrow.HALF;

@Mixin(value = BlockScarecrow.class, remap = false)
public abstract class BlockScarecrowMixin extends AbstractBlockMinecoloniesDefault<BlockScarecrow> implements EntityBlock, IBuildingBrowsableBlock {

    public BlockScarecrowMixin()
    {
        super(Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(HARDNESS, RESISTANCE));
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Inject(
            method = "useItemOn",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    public void useHead(
            final ItemStack stack,
            final BlockState state,
            final Level worldIn,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult ray,
            CallbackInfoReturnable<ItemInteractionResult> cir
    ){
        // If the world is client, open the inventory of the field.
        if (worldIn.isClientSide)
        {
            // Get the entity of the bottom half
            DoubleBlockHalf half = state.getValue(HALF);
            final BlockEntity entity = worldIn.getBlockEntity(half == DoubleBlockHalf.UPPER ? pos.below() : pos);

            if (entity instanceof TileEntityScarecrow scarecrow)
            {
                new NewWindowField(scarecrow).open();
                cir.setReturnValue(ItemInteractionResult.SUCCESS);
            }
            else
            {
                cir.setReturnValue(ItemInteractionResult.FAIL);
            }
        }

        // This must succeed in Remote to stop more right click interactions like placing blocks
        cir.setReturnValue(ItemInteractionResult.SUCCESS);
    }

}
