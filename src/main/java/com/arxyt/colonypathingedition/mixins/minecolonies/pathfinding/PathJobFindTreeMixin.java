package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobFindTree;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PathJobFindTree.class, remap = false)
public class PathJobFindTreeMixin {
    @Shadow(remap = false) private AABB restrictionBox;
    @Inject(
            at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Ljava/util/List;ILcom/minecolonies/api/colony/IColony;Lnet/minecraft/world/entity/Mob;)V",
            remap = false)
    void expandYRestrict(CallbackInfo ci){
        if(restrictionBox != null){
            restrictionBox.expandTowards(0,3,0);
            restrictionBox.expandTowards(0,-3,0);
        }
    }
}
