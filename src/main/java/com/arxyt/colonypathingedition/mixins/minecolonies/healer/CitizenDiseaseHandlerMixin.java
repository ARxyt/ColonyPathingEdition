package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.arxyt.colonypathingedition.api.ICitizenDiseaseHandlerExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.datalistener.DiseasesListener;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.SICK_TIME;
import static com.minecolonies.api.util.constant.CitizenConstants.*;

@Mixin(value = CitizenDiseaseHandler.class, remap = false)
public abstract class CitizenDiseaseHandlerMixin implements ICitizenDiseaseHandlerExtra {
    @Final @Shadow(remap = false) private ICitizenData citizenData;
    @Shadow(remap = false) private Disease disease;
    @Shadow(remap = false) private int immunityTicks;

    @Shadow(remap = false) protected abstract boolean canBecomeSick();
    @Shadow(remap = false) public abstract boolean isSick();

    @Unique private int sickTime;

    @Unique public int getSickTime(){
        return this.sickTime;
    }
    @Unique public void setSickTime(int sickTime){
        this.sickTime = sickTime;
    }

    /**
     * @author ARxyt
     * @reason 加入生病逻辑判断
     */
    @Overwrite(remap = false)
    public void write(final CompoundTag compound)
    {
        CompoundTag diseaseTag = new CompoundTag();
        if (disease != null)
        {
            diseaseTag.putString(TAG_DISEASE_ID, disease.id().toString());
            diseaseTag.putInt(SICK_TIME, sickTime);
        }
        else{
            diseaseTag.putInt(SICK_TIME, 0);
        }
        diseaseTag.putInt(TAG_IMMUNITY, immunityTicks);
        compound.put(TAG_DISEASE, diseaseTag);
    }

    /**
     * @author ARxyt
     * @reason 加入生病逻辑判断
     */
    @Overwrite(remap = false)
    public void read(final CompoundTag compound)
    {
        if (!compound.contains(TAG_DISEASE, Tag.TAG_COMPOUND))
        {
            return;
        }

        CompoundTag diseaseTag = compound.getCompound(TAG_DISEASE);
        if (diseaseTag.contains(TAG_DISEASE_ID))
        {
            this.disease = DiseasesListener.getDisease(ResourceLocation.parse(diseaseTag.getString(TAG_DISEASE_ID)));
        }

        this.immunityTicks = diseaseTag.getInt(TAG_IMMUNITY);
        this.sickTime = diseaseTag.getInt(SICK_TIME);
    }

    /**
     * @author ARxyt
     * @reason 加入生病逻辑判断
     */
    @Overwrite(remap = false)
    public boolean setDisease(final @Nullable Disease disease)
    {
        if (canBecomeSick())
        {
            this.disease = disease;
            this.sickTime = 0;
            return true;
        }
        return false;
    }

    /** 将生病时间统计放在疾病监测事件中 */
    @Inject(method = "update", at= @At("RETURN") ,remap = false)
    public void afterUpdate(final int tickRate, CallbackInfo cir){
        if ( isSick() ){
            if(this.sickTime < 0){
                this.sickTime = 0;
            }
            int lastTime = this.sickTime - PathingConfig.HEAL_START.get() * 20;
            this.sickTime += tickRate;
            if(citizenData.getEntity().isPresent()) {
                if (lastTime >= 0 && citizenData.getEntity().get().getRandom().nextInt( PathingConfig.HEAL_DURATION.get() * 20 - lastTime) < tickRate) {
                    cure();
                }
            }
        }
    }

    /**
     * @author ARxyt
     * @reason 重写一下hurt判定
     */
    @Overwrite(remap = false)
    public boolean isHurt()
    {
        Optional<AbstractEntityCitizen> e = citizenData.getEntity();
        if (e.isEmpty()) return false;
        AbstractEntityCitizen citizen = e.get();
        return citizen.getHealth() < Math.min(Math.max(PathingConfig.MAX_PERCENTAGE_HP_FOR_CURE.get() * citizen.getMaxHealth(), PathingConfig.MAX_HP_FOR_CURE.get()),Math.min(100,citizen.getMaxHealth()));
    }
}
