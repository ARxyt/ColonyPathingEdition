package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.api.workersetting.BuildingLumberjackExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BuildingLumberjack.class)
public abstract class BuildingLumberjackMixin implements IBuilding, BuildingLumberjackExtra
{
    @Unique BlockPos lastTree = null;
    @Unique BlockPos thisTree = null;

    @Override
    public boolean canAssignCitizens()
    {
        if(PathingConfig.LUMBERJACK_WORK_WHEN_UNCONSTRUCTED.get()){
            return true;
        }
        return getBuildingLevel() > 0 && isBuilt();
    }

    @Unique
    public BlockPos getLastTree(){
        return this.lastTree;
    }

    @Unique
    public void thisTreeToLast(){
        if (thisTree != null){
            lastTree = thisTree;
            thisTree = null;
        }
    }

    @Unique
    public BlockPos getThisTree(){
        return this.thisTree;
    }

    @Unique
    public void setThisTree(BlockPos treePos){
        thisTree = treePos;
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN"),remap = false)
    private void deserializeNBTAddition (CompoundTag compound, CallbackInfo cir){
        if(compound.contains("last_tree", Tag.TAG_COMPOUND)){
            CompoundTag treeTag = compound.getCompound("last_tree");
            int x = treeTag.getInt("x");
            int y = treeTag.getInt("y");
            int z = treeTag.getInt("z");
            lastTree = new BlockPos(x, y, z);
        }
        if(compound.contains("this_tree", Tag.TAG_COMPOUND)){
            CompoundTag treeTag = compound.getCompound("this_tree");
            int x = treeTag.getInt("x");
            int y = treeTag.getInt("y");
            int z = treeTag.getInt("z");
            thisTree = new BlockPos(x, y, z);
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",at=@At("RETURN"),remap = false,cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        if (lastTree != null) {
            CompoundTag treeTag = new CompoundTag();
            treeTag.putInt("x", lastTree.getX());
            treeTag.putInt("y", lastTree.getY());
            treeTag.putInt("z", lastTree.getZ());
            tag.put("last_tree", treeTag);
        }
        if (thisTree != null){
            CompoundTag treeTag = new CompoundTag();
            treeTag.putInt("x", thisTree.getX());
            treeTag.putInt("y", thisTree.getY());
            treeTag.putInt("z", thisTree.getZ());
            tag.put("this_tree", treeTag);
        }
        cir.setReturnValue(tag);
    }
}
