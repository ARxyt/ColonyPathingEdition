package com.arxyt.colonypathingedition.mixins.minecolonies.lumberjack;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedList;

@Mixin(value = Tree.class, remap = false)
public abstract class MangroveTreeMixin {
    @Shadow(remap = false) private LinkedList<BlockPos> woodBlocks;
    @Shadow(remap = false) private ArrayList<BlockPos> stumpLocations;
    @Shadow(remap = false) private ItemStack sapling;
    @Shadow(remap = false) private BlockPos topLog;
    @Shadow(remap = false) private BlockPos location;

    @Unique
    public void fillMangroveTreeStumps(final int yLevel, Level world)
    {
        for (@NotNull final BlockPos pos : woodBlocks)
        {
            if (pos.getY() == yLevel && !sapling.is(Items.MANGROVE_PROPAGULE))
            {
                stumpLocations.add(pos);
            }
            else if (pos.getY() <= topLog.getY() && pos.getY() >= yLevel && sapling.is(Items.MANGROVE_PROPAGULE) &&
                    world.getBlockState(pos).is(Blocks.MANGROVE_LOG) )
            {
                stumpLocations.add(pos);
            }
        }

        // 红树树桩处理
        if (!stumpLocations.isEmpty() && sapling.is(Items.MANGROVE_PROPAGULE))
        {
            BlockPos.MutableBlockPos acc = BlockPos.ZERO.mutable();
            acc.move(0,320,0);
            for (final BlockPos stump : stumpLocations)
            {
                if ( acc.getY() > stump.getY() )
                {
                    acc = BlockPos.ZERO.mutable().move(stump);
                }
            }
            if (!stumpLocations.isEmpty() && sapling.is(Items.MANGROVE_PROPAGULE))
            {
                // 保留原有 acc 的 X 和 Z 坐标
                int targetX = acc.getX();
                int targetZ = acc.getZ();

                // 从 topLog 的 Y 坐标向下搜索到 yLevel（树的根部高度）
                BlockPos plantPos = null;
                for (int y = topLog.getY(); y >= yLevel; y--)
                {
                    BlockPos checkPos = new BlockPos(targetX, y, targetZ);
                    BlockState checkBlock = world.getBlockState(checkPos);
                    BlockState belowBlock = world.getBlockState(checkPos.below());

                    // 检查基底方块是否可种植（如泥巴），且上方可放置树苗（空气/水）
                    if ( (belowBlock.is(BlockTags.DIRT)||belowBlock.is(Blocks.MUD)) &&
                            (checkBlock.canBeReplaced()||checkBlock.is(Blocks.WATER))) {
                        plantPos = checkPos; // 有效位置为基底方块的上方
                        break;
                    }
                }
                stumpLocations.clear();
                if (plantPos != null)
                {
                    stumpLocations.add(plantPos);
                }
                else
                {
                    BlockPos defaultPos = new BlockPos(targetX, yLevel, targetZ);
                    stumpLocations.add(defaultPos);
                }

            }
        }

    }

    /**
     * @author ARxyt
     * @reason 修改一下红树木的补种策略,所以在这里重构了fillTreeStumps，这里转而调用重构后的算法，重置函数以保证稳定性。
     */
    @Inject(
            method = "findLogs",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/util/Tree;fillTreeStumps(I)V",
                    shift = At.Shift.BEFORE  // 在 fillTreeStumps 调用之前注入
            ),
            cancellable = true,  // 允许取消原方法调用
            remap = false
    )
    private void beforeFillTreeStumps(Level world, IColony colony, CallbackInfo ci) {
        if (sapling.is(Items.MANGROVE_PROPAGULE)) {
            // 调用你的 fillTreeStumps 逻辑
            fillMangroveTreeStumps(location.getY(), world);
            // 取消原方法的 fillTreeStumps 调用
            ci.cancel();
        }
        // 否则继续执行原方法
    }
}
