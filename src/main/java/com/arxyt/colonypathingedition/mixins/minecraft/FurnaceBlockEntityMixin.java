package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.extras.FurnaceBlockEntityExtra;
import com.minecolonies.api.util.constant.Constants;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin implements FurnaceBlockEntityExtra {
    @Final @Shadow private Object2IntOpenHashMap<ResourceLocation> recipesUsed;
    @Shadow int litTime;
    @Shadow int cookingProgress;
    @Shadow int cookingTotalTime;
    @Shadow int litDuration;

    @Shadow public abstract List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec);

    @Unique private int pathFindEdition$workerID = -1;
    @Unique private int pathFindEdition$pickerID = -1;
    @Unique private int pathFindEdition$fuelerID = -1;
    @Unique private int pathFindEdition$protectTime = 0;

    // 增加进度
    @Unique
    public int pathFindEdition$addProgress(int adder){
        cookingProgress += adder;
        if(cookingProgress >= cookingTotalTime){
            int left = cookingProgress - cookingTotalTime;
            cookingProgress =  cookingTotalTime - 1;
            litTime ++;
            return left;
        }
        return 0;
    }

    // 增加燃料点燃时间
    @Unique
    public void pathFindEdition$addLitTime(int adder){
        if(litTime == 0){
            return;
        }
        litTime += adder;
        if(litDuration < litTime){
            litTime = litDuration;
        }
    }

    @Unique
    public void pathFindEdition$tickProtect(){
        if(pathFindEdition$protectTime > 0){
            pathFindEdition$protectTime--;
        }
    }

    @Unique
    public void pathFindEdition$setPickup(AbstractFurnaceBlockEntity pBlockEntity){
        if(cookingTotalTime == cookingProgress + 1 && pBlockEntity.getItem(Constants.SMELTABLE_SLOT).getCount() == 1){
            pathFindEdition$setFurnacePicker(pathFindEdition$workerID);
        }
    }

    @Unique
    public int pathFindEdition$getFurnaceWorker() {
        return pathFindEdition$workerID;
    }

    @Unique
    public void pathFindEdition$setFurnaceWorker(int workerID) {
        this.pathFindEdition$workerID = workerID;
        if(workerID < 0){
            pathFindEdition$protectTime = 0;
        }
        else{
            pathFindEdition$protectTime = 60; //保护三秒
        }
    }

    @Unique
    public int pathFindEdition$getFurnacePicker(){
        return pathFindEdition$pickerID;
    }

    @Unique
    public void pathFindEdition$setFurnacePicker(int pickerID){
        this.pathFindEdition$pickerID = pickerID;
        if(pickerID < 0){
            pathFindEdition$protectTime = 0;
        }
        else{
            pathFindEdition$protectTime = 40; //保护两秒
        }
    }

    /**
     * @return citizen Civilian ID
     */
    @Unique
    public int pathFindEdition$getFurnaceFueler() {
        return pathFindEdition$fuelerID;
    }

    /**
     * @param fuelerID: Civilian ID
     */
    @Unique
    public void pathFindEdition$setFurnaceFueler(int fuelerID) {
        this.pathFindEdition$fuelerID = fuelerID;
        if(fuelerID >= 0){
            pathFindEdition$protectTime = 40; //保护两秒
        }
    }

    @Unique
    public void pathFindEdition$setProtectTime(int protectTime){
        this.pathFindEdition$protectTime = protectTime;
    }

    @Unique
    public boolean pathFindEdition$atProtectTime() {
        return pathFindEdition$protectTime > 0;
    }

    @Unique
    public void pathFindEdition$clearUsedRecipesAndPopExperience(ServerLevel world, Entity worker) {
        if(world == null) return;
        this.getRecipesToAwardAndPopExperience(world, worker.position());
        this.recipesUsed.clear();
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void onSave(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        tag.putInt("WorkerID", pathFindEdition$workerID);
        tag.putInt("PickerID", pathFindEdition$pickerID);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void onLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains("WorkerID")) {
            pathFindEdition$workerID = tag.getInt("WorkerID");
        }
        else{
            pathFindEdition$workerID = -1;
        }
        if (tag.contains("PickerID")) {
            pathFindEdition$pickerID = tag.getInt("PickerID");
        }
        else{
            pathFindEdition$pickerID = -1;
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void afterServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtra)pBlockEntity).pathFindEdition$tickProtect();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void beforeServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtra)pBlockEntity).pathFindEdition$setPickup(pBlockEntity);
    }
}
