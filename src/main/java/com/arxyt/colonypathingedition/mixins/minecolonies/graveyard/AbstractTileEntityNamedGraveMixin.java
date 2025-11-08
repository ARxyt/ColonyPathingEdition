package com.arxyt.colonypathingedition.mixins.minecolonies.graveyard;

import com.arxyt.colonypathingedition.core.easycolony.extension.IGraveDataExtension;
import com.minecolonies.api.colony.GraveData;
import com.minecolonies.api.colony.IGraveData;
import com.minecolonies.api.tileentities.AbstractTileEntityNamedGrave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractTileEntityNamedGrave.class)
public abstract class AbstractTileEntityNamedGraveMixin extends BlockEntity implements IGraveDataExtension {

    @Unique
    private static final String TAG_GRAVE_DATA = "gravedata";

    public AbstractTileEntityNamedGraveMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Unique
    @Nullable
    private IGraveData graveData;

    @Override
    @Nullable
    public IGraveData getGraveData() {
        return graveData;
    }

    @Override
    public void setGraveData(IGraveData graveData) {
        this.graveData = graveData;
        setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider, CallbackInfo ci) {
        if (graveData != null) {
            compound.put(TAG_GRAVE_DATA, graveData.write());
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider provider, CallbackInfo ci) {
        if (compound.contains(TAG_GRAVE_DATA)) {
            graveData = new GraveData();
            graveData.read(compound.getCompound(TAG_GRAVE_DATA));
        } else {
            graveData = null;
        }
    }

}
