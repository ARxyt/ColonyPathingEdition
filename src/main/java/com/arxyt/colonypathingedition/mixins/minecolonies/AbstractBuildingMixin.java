package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.workersetting.BuildingPickupExtra;
import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.google.common.collect.ImmutableCollection;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Pickup;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The purpose of this Mixin is to modify the pickUp mechanism.
 * 1. Locally store pickUpDay to prevent all hut clearance timers from resetting
 *    every time the save is loaded.
 * 2. After attempting to issue a pickUp process, do not require the next pickUp
 *    request to solely resolve the process blockage caused by the "attempted pickUp".
 */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class AbstractBuildingMixin extends AbstractBuildingContainer implements BuildingPickupExtra {

    @Unique public int minDayPickup = -1;
    @Unique public int cumulatedItems = 0;

    @Shadow(remap = false) private String customName;
    @Shadow(remap = false) protected List<IBuildingModule> modules;

    @Shadow(remap = false) public abstract boolean hasModule(BuildingEntry.ModuleProducer<?, ?> producer);
    @Shadow(remap = false) protected abstract int getCurrentWorkOrderLevel();
    @Shadow(remap = false) protected abstract void writeRequestSystemToNBT(CompoundTag compound);

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

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",at = @At("RETURN"),remap = false)
    private void deserializeNBTAddition (CompoundTag compound, CallbackInfo cir) {
        if(compound.contains("pick_up_day")) {
            minDayPickup = compound.getInt("pick_up_day");
        }
        if(compound.contains("cumulated_items")) {
            minDayPickup = compound.getInt("cumulated_items");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",at = @At("RETURN"),remap = false, cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("pick_up_day", minDayPickup);
        tag.putInt("cumulated_items", cumulatedItems);
        cir.setReturnValue(tag);
    }

    @Unique
    public int shouldPickup (final int pickUpPriority, final int qty) {
        if(getOpenRequestsByRequestableType().getOrDefault(TypeConstants.PICKUP, Collections.emptyList()) != null) {
            return 15;
        }
        cumulatedItems += qty;
        int daysToPickup = 10 - pickUpPriority;

        if(cumulatedItems >= 256 + 64 * daysToPickup ) {
            cumulatedItems = 0;
            minDayPickup = -1;
            return 15;
        }
        if(minDayPickup == -1){
            minDayPickup = colony.getDay() + daysToPickup;
        }
        else if (minDayPickup > colony.getDay() + daysToPickup)
        {
            minDayPickup = colony.getDay() + daysToPickup;
        }

        if (colony.getDay() < minDayPickup)
        {
            return 0;
        }
        cumulatedItems = 0;
        minDayPickup = -1;
        return pickUpPriority;
    }

    @Unique
    public boolean newCreatePickupRequest(int pickUpPriority)
    {
        final List<IToken<?>> reqs = new ArrayList<>(getOpenRequestsByRequestableType().getOrDefault(TypeConstants.PICKUP, Collections.emptyList()));
        if (!reqs.isEmpty())
        {
            for (final IToken<?> req : reqs)
            {
                final IRequest<?> request = colony.getRequestManager().getRequestForToken(req);
                if (request != null && request.getState() == RequestState.IN_PROGRESS)
                {
                    final IRequestResolver<?> resolver = colony.getRequestManager().getResolverForRequest(req);
                    if (resolver instanceof IPlayerRequestResolver || resolver instanceof IRetryingRequestResolver)
                    {
                        colony.getRequestManager().reassignRequest(req, Collections.emptyList());
                    }
                }
            }
            return false;
        }

        createRequest(new Pickup(pickUpPriority), true);
        return true;
    }

    /**
     * @author ARxyt
     * @reason Use key rather than runtime ID to get module.
     */
    @Overwrite(remap = false)
    public void serializeToView(@NotNull final FriendlyByteBuf buf, final boolean fullSync)
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

        buf.writeInt(getRotation());
        buf.writeBoolean(isMirrored());
        buf.writeInt(getClaimRadius(getBuildingLevel()));

        final CompoundTag requestSystemCompound = new CompoundTag();
        writeRequestSystemToNBT(requestSystemCompound);

        final ImmutableCollection<IRequestResolver<?>> resolvers = getResolvers();
        buf.writeInt(resolvers.size());
        for (final IRequestResolver<?> resolver : resolvers)
        {
            buf.writeNbt(StandardFactoryController.getInstance().serialize(resolver.getId()));
        }
        buf.writeNbt(StandardFactoryController.getInstance().serialize(getId()));
        buf.writeInt(containerList.size());
        for (BlockPos blockPos : containerList)
        {
            buf.writeBlockPos(blockPos);
        }
        buf.writeNbt(requestSystemCompound);

        buf.writeBoolean(isDeconstructed());
        buf.writeBoolean(canAssignCitizens());

        final List<IBuildingModule> syncedModules = new ArrayList<>();
        for (final IBuildingModule module : modules)
        {
            if (module.getProducer().hasView())
            {
                syncedModules.add(module);
            }
        }

        buf.writeInt(syncedModules.size());
        for (final IBuildingModule module : syncedModules)
        {
            buf.writeInt(module.getProducer().getRuntimeID());
            buf.writeUtf(module.getProducer().key);
            module.serializeToView(buf, fullSync);
        }
    }
}
