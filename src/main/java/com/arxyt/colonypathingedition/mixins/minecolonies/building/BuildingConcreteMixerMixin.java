package com.arxyt.colonypathingedition.mixins.minecolonies.building;

import com.arxyt.colonypathingedition.api.workersetting.BuildingConcreteMixerExtra;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingConcreteMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = BuildingConcreteMixer.class, remap = false)
public abstract class BuildingConcreteMixerMixin implements BuildingConcreteMixerExtra {
    private static final String CONCRETE_MIXER_SIMULATED_BLOCKS = "simulated_blocks";
    private static final String CONCRETE_MIXER_SIMULATED_POS = "simulated_block_pos";
    private static final String CONCRETE_MIXER_SIMULATED_COUNT = "simulated_count";

    @Shadow(remap = false) @Final private Map<Integer, List<BlockPos>> waterPos;

    @Unique Map<BlockPos, Integer> simulatedBlocks = new HashMap<>();

    @Unique public void addSimulatedBlock(BlockPos pos, int num){
        simulatedBlocks.put(pos,num);
    }

    @Unique public int getSimulatedTimes(BlockPos pos){
        return simulatedBlocks.getOrDefault(pos, 1);
    }

    @Unique public void deleteSimulateBlock(BlockPos pos){
        simulatedBlocks.remove(pos);
    }

    @Unique
    private boolean isValidWaterPos(BlockPos pos)
    {
        for (List<BlockPos> list : waterPos.values())
        {
            if (list.contains(pos))
            {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    private void deserializeNBTAddition (CompoundTag compound, CallbackInfo cir){
        simulatedBlocks.clear();

        final ListTag simMapList = compound.getList(CONCRETE_MIXER_SIMULATED_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < simMapList.size(); i++)
        {
            final CompoundTag simCompound = simMapList.getCompound(i);
            final BlockPos pos = NbtUtils.readBlockPos(simCompound.getCompound(CONCRETE_MIXER_SIMULATED_POS));
            final int count = simCompound.getInt(CONCRETE_MIXER_SIMULATED_COUNT);
            simulatedBlocks.put(pos, count);
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"), remap = false, cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir){
        final CompoundTag compound = cir.getReturnValue();
        @NotNull final ListTag simMap = new ListTag();
        for (@NotNull final Map.Entry<BlockPos, Integer> entry : simulatedBlocks.entrySet())
        {
            final CompoundTag simCompound = new CompoundTag();
            // 🔑 这里做过滤：只把合适的方块位置加入 map
            if (isValidWaterPos(entry.getKey()))  // 你可以换成自己的判定条件
            {
                simCompound.put(CONCRETE_MIXER_SIMULATED_POS, NbtUtils.writeBlockPos(entry.getKey()));
                simCompound.putInt(CONCRETE_MIXER_SIMULATED_COUNT, entry.getValue());
                simMap.add(simCompound);
            }
        }
        compound.put(CONCRETE_MIXER_SIMULATED_BLOCKS, simMap);

        cir.setReturnValue(compound);
    }
}
