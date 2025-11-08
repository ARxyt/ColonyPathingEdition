package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.AbstractMinecartAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin implements AbstractMinecartAccessor {
    @Shadow private boolean onRails;
    @Shadow private int lerpSteps;
    @Shadow private boolean flipped;

    @Accessor("lerpSteps")
    public abstract int getLSteps();
    @Accessor("flipped")
    public abstract boolean getFlipped();
    @Accessor("lerpX")
    public abstract double getLx();
    @Accessor("lerpY")
    public abstract double getLy();
    @Accessor("lerpZ")
    public abstract double getLz();
    @Accessor("lerpXRot")
    public abstract double getLxr();
    @Accessor("lerpYRot")
    public abstract double getLyr();

    @Unique public void lStepMinus(){
        this.lerpSteps--;
    }

    @Unique public void filpReverse(){
        this.flipped = !this.flipped;
    }

    @Unique public void setOnRails(boolean onRails){
        this.onRails = onRails;
    }
}
