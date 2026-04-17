package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.FurnaceBlockEntityExtras;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin implements FurnaceBlockEntityExtras {
    @Shadow int litTime;
    @Shadow int cookingProgress;
    @Shadow int cookingTotalTime;
    @Shadow int litDuration;

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
        ((FurnaceBlockEntityExtras)pBlockEntity).pathFindEdition$tickProtect();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void beforeServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtras)pBlockEntity).pathFindEdition$setPickup(pBlockEntity);
    }
}
