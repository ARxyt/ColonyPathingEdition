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

    @Unique private int workerID = -1;
    @Unique private int pickerID = -1;
    @Unique private int protectTime = 0;

    // 增加进度
    @Unique
    public int addProgress(int adder){
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
    public void addLitTime(int adder){
        if(litTime == 0){
            return;
        }
        litTime += adder;
        if(litDuration < litTime){
            litTime = litDuration;
        }
    }

    @Unique
    public void tickProtect(){
        if(protectTime > 0){
            protectTime --;
        }
    }

    @Unique
    public void setPickup(AbstractFurnaceBlockEntity pBlockEntity){
        if(cookingTotalTime == cookingProgress + 1 && pBlockEntity.getItem(Constants.SMELTABLE_SLOT).getCount() == 1){
            setFurnacePicker(workerID);
        }
    }

    @Unique
    public int getFurnaceWorker() {
        return workerID;
    }

    @Unique
    public void setFurnaceWorker(int workerID) {
        this.workerID = workerID;
        if(workerID < 0){
            protectTime = 0;
        }
        else{
            protectTime = 60; //保护三秒
        }
    }

    @Unique
    public int getFurnacePicker(){
        return pickerID;
    }

    @Unique
    public void setFurnacePicker(int pickerID){
        this.pickerID = pickerID;
        if(pickerID < 0){
            protectTime = 0;
        }
        else{
            protectTime = 40; //保护两秒
        }
    }

    @Unique
    public void setProtectTime(int protectTime){
        this.protectTime = protectTime;
    }

    @Unique
    public boolean atProtectTime() {
        return protectTime > 0;
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void onSave(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        tag.putInt("WorkerID", workerID);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void onLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains("WorkerID")) {
            workerID = tag.getInt("WorkerID");
        }
        else{
            workerID = -1;
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void afterServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtras)pBlockEntity).tickProtect();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void beforeServerTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtras)pBlockEntity).setPickup(pBlockEntity);
    }
}
