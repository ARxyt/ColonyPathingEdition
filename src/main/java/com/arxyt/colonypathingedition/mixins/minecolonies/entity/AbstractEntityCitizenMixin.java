package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenJobHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractEntityCitizen.class, remap = false)
public abstract class AbstractEntityCitizenMixin extends AbstractCivilianEntity {

    @Shadow(remap = false) public abstract ICitizenJobHandler getCitizenJobHandler();

    public AbstractEntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        //市民职业信息
        if(getCitizenJobHandler() != null && getCitizenJobHandler().getColonyJob() != null) {
            tag.putString("citizenJob", getCitizenJobHandler().getColonyJob().getModel().getPath());
        }
        else{
            tag.putString("citizenJob", "unemployed");
        }
    }
}
