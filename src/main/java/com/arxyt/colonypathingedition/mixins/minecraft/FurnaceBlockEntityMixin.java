package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.FurnaceBlockEntityExtras;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
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

    @Unique private int colonyPathingEdition$workerID = -1;
    @Unique private int colonyPathingEdition$pickerID = -1;
    @Unique private int colonyPathingEdition$fuelerID = -1;
    @Unique private int colonyPathingEdition$protectTime = 0;

    // 增加进度
    @Unique
    public int colonyPathingEdition$addProgress(int adder){
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
    public void colonyPathingEdition$addLitTime(int adder){
        if(litTime == 0){
            return;
        }
        litTime += adder;
        if(litDuration < litTime){
            litTime = litDuration;
        }
    }

    @Unique
    public void colonyPathingEdition$tickProtect(){
        if(colonyPathingEdition$protectTime > 0){
            colonyPathingEdition$protectTime--;
        }
    }

    @Unique
    public void colonyPathingEdition$setPickup(AbstractFurnaceBlockEntity pBlockEntity){
        if(cookingTotalTime == cookingProgress + 1 && pBlockEntity.getItem(Constants.SMELTABLE_SLOT).getCount() == 1){
            colonyPathingEdition$setFurnacePicker(colonyPathingEdition$workerID);
        }
    }

    @Unique
    public int colonyPathingEdition$getFurnaceWorker() {
        return colonyPathingEdition$workerID;
    }

    @Unique
    public void colonyPathingEdition$setFurnaceWorker(int workerID) {
        this.colonyPathingEdition$workerID = workerID;
        if(workerID < 0){
            colonyPathingEdition$protectTime = 0;
        }
        else{
            colonyPathingEdition$protectTime = 60; //保护三秒
        }
    }

    @Unique
    public int colonyPathingEdition$getFurnacePicker(){
        return colonyPathingEdition$pickerID;
    }

    @Unique
    public void colonyPathingEdition$setFurnacePicker(int pickerID){
        this.colonyPathingEdition$pickerID = pickerID;
        if(pickerID < 0){
            colonyPathingEdition$protectTime = 0;
        }
        else{
            colonyPathingEdition$protectTime = 40; //保护两秒
        }
    }

    /**
     * @return citizen Civilian ID
     */
    @Unique
    public int colonyPathingEdition$getFurnaceFueler() {
        return colonyPathingEdition$fuelerID;
    }

    /**
     * @param fuelerID: Civilian ID
     */
    @Unique
    public void colonyPathingEdition$setFurnaceFueler(int fuelerID) {
        this.colonyPathingEdition$fuelerID = fuelerID;
        if(fuelerID >= 0){
            colonyPathingEdition$protectTime = 40; //保护两秒
        }
    }

    @Unique
    public void colonyPathingEdition$setProtectTime(int protectTime){
        this.colonyPathingEdition$protectTime = protectTime;
    }

    @Unique
    public boolean colonyPathingEdition$atProtectTime() {
        return colonyPathingEdition$protectTime > 0;
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("WorkerID", colonyPathingEdition$workerID);
        tag.putInt("PickerID", colonyPathingEdition$pickerID);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("WorkerID")) {
            colonyPathingEdition$workerID = tag.getInt("WorkerID");
        }
        else{
            colonyPathingEdition$workerID = -1;
        }
        if (tag.contains("PickerID")) {
            colonyPathingEdition$pickerID = tag.getInt("PickerID");
        }
        else{
            colonyPathingEdition$pickerID = -1;
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void afterServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtras)pBlockEntity).colonyPathingEdition$tickProtect();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void beforeServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtras)pBlockEntity).colonyPathingEdition$setPickup(pBlockEntity);
    }
}
