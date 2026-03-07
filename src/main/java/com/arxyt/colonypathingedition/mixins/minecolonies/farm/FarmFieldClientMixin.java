package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.colony.buildingextensions.AbstractBuildingExtension;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.Level.TICKS_PER_DAY;


@Mixin(value = FarmField.class, remap = false)
public abstract class FarmFieldClientMixin extends AbstractBuildingExtension {
    @Shadow(remap = false)  private ItemStack seed;

    @Unique
    private FarmField asFarmField(){
        return ((FarmField)(Object)this);
    }

    public FarmFieldClientMixin(final BuildingExtensionRegistries.BuildingExtensionEntry fieldType, final BlockPos position)
    {
        super(fieldType, position);
    }

    @Inject(method = "getSeed",at = @At("HEAD"),remap = false ,cancellable = true)
    public void getSeed(CallbackInfoReturnable<ItemStack> cir)
    {
        FarmFieldExtra farmField = (FarmFieldExtra) asFarmField();

        if(farmField.isSeasonalSeedsEmpty() && !seed.isEmpty()){
            farmField.setSeasonSeed(1,seed);
        }
        Level world = Minecraft.getInstance().level;
        if(world != null && world.getDayTime() != 0){
            final int nowDay = (int)(world.getDayTime() / TICKS_PER_DAY);
            if(nowDay != farmField.getDate()) {
                farmField.advanceDay(nowDay);
            }
        }
        seed = farmField.getSeasonSeed(farmField.getCurrentSeason()).copy();
        seed.setCount(1);
        if(seed.isEmpty()){
            asFarmField().setBuilding(null);
        }
        cir.setReturnValue(seed);
    }
}
