package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.accessor.AbstractMinecartAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin implements AbstractMinecartAccessor {
    @Shadow private boolean onRails;
    @Shadow private int lSteps;
    @Shadow private boolean flipped;

    @Accessor("lSteps")
    public abstract int getLSteps();
    @Accessor("flipped")
    public abstract boolean getFlipped();
    @Accessor("lx")
    public abstract double getLx();
    @Accessor("ly")
    public abstract double getLy();
    @Accessor("lz")
    public abstract double getLz();
    @Accessor("lxr")
    public abstract double getLxr();
    @Accessor("lyr")
    public abstract double getLyr();

    @Unique public void lStepMinus(){
        this.lSteps--;
    }

    @Unique public void filpReverse(){
        this.flipped = !this.flipped;
    }

    @Unique public void setOnRails(boolean onRails){
        this.onRails = onRails;
    }
}
