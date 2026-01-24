package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.google.common.collect.ImmutableCollection;
import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 此Mixin的目的是修改pickUp机制。
 * 1.本地存储 pickUpDay, 阻止每次进入存档即重置所有小屋清仓计时。
 * 2.当尝试发出 pick up 进程后，不要求下一次 pick up 请求仅来清理 “尝试 pick up“ 的进程堵塞问题。
 */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class AbstractBuildingMixin extends AbstractBuildingContainer {

    @Shadow(remap = false) public int pickUpDay;
    @Shadow(remap = false) protected List<IBuildingModule> modules;
    @Shadow(remap = false) private String customName;

    @Shadow(remap = false) protected abstract int getCurrentWorkOrderLevel();
    @Shadow(remap = false) protected abstract void writeRequestSystemToNBT(HolderLookup.@NotNull Provider provider, CompoundTag compound);

    protected AbstractBuildingMixin(@NotNull final IColony colony, final BlockPos pos) {
        super(pos, colony);
        throw new RuntimeException("AbstractBuildingMixin 类不应被实例化！");
    }

    @Override
    public boolean canEat(final ItemStack stack)
    {
        FoodBlackListMenuModule module = getModule(FoodBlackListMenuModule.class);
        if(module != null) {
            if (module.getBlackList().contains(new ItemStorage(stack))) return false;
        }
        else{
            if (PathingConfig.food_black_list.contains(new ItemStorage(stack))) return false;
        }
        return super.canEat(stack);
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V",at = @At("RETURN"),remap = false)
    private void deserializeNBTAddition (HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        if(compound.contains("pick_up_day")) {
            pickUpDay = compound.getInt("pick_up_day");
        }
    }

    @Inject(method = "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",at = @At("RETURN"),remap = false,cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("pick_up_day", pickUpDay);
        cir.setReturnValue(tag);
    }

    @Inject(method = "createPickupRequest",at = @At("RETURN"),remap = false)
    private void resetPickUpDay (final int pickUpPrio, CallbackInfoReturnable<Boolean> cir) {
        if(pickUpDay == -1){
            int daysToPickup = 10 - pickUpPrio;
            pickUpDay = colony.getDay() + daysToPickup;
        }
    }

    /**
     * @author ARxyt
     * @reason Use key rather than runtime ID to get module.
     */
    @Overwrite(remap = false)
    public void serializeToView(@NotNull final RegistryFriendlyByteBuf buf, final boolean fullSync)
    {
        buf.writeUtf(this.getBuildingType().getRegistryName().toString());
        buf.writeInt(getBuildingLevel());
        buf.writeInt(getMaxBuildingLevel());
        buf.writeInt(getPickUpPriority());
        buf.writeInt(getCurrentWorkOrderLevel());
        buf.writeUtf(getStructurePack());
        buf.writeUtf(getBlueprintPath());
        buf.writeBlockPos(getParent());
        buf.writeUtf(this.customName);

        if (getRotationMirror() == null)
        {
            Log.getLogger().error(String.format("Building %s is supposed to have rotation mirror!", this.getBuildingType().getRegistryName().toString()));
            buf.writeByte(RotationMirror.NONE.ordinal());
        }
        else
        {
            buf.writeByte(getRotationMirror().ordinal());
        }
        buf.writeInt(getClaimRadius(getBuildingLevel()));

        final CompoundTag requestSystemCompound = new CompoundTag();
        writeRequestSystemToNBT(buf.registryAccess(), requestSystemCompound);

        final ImmutableCollection<IRequestResolver<?>> resolvers = getResolvers();
        buf.writeInt(resolvers.size());
        for (final IRequestResolver<?> resolver : resolvers)
        {
            buf.writeNbt(StandardFactoryController.getInstance().serializeTag(buf.registryAccess(), resolver.getId()));
        }
        buf.writeNbt(StandardFactoryController.getInstance().serializeTag(buf.registryAccess(), getId()));
        buf.writeInt(containerList.size());
        for (BlockPos blockPos : containerList)
        {
            buf.writeBlockPos(blockPos);
        }
        buf.writeNbt(requestSystemCompound);

        buf.writeBoolean(isDeconstructed());
        buf.writeBoolean(canAssignCitizens());

        final List<IBuildingModule> syncedModules = new ArrayList<>();
        for(final IBuildingModule module:modules)
        {
            if (module.getProducer().hasView())
            {
                syncedModules.add(module);
            }
        }

        buf.writeInt(syncedModules.size());
        for (final IBuildingModule module: syncedModules)
        {
            buf.writeInt(module.getProducer().getRuntimeID());
            buf.writeUtf(module.getProducer().key);
            module.serializeToView(buf, fullSync);
        }
    }
}
