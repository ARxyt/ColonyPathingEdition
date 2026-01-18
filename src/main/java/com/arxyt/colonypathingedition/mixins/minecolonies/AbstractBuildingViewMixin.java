package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.views.AbstractBuildingView;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(value = AbstractBuildingView.class, remap = false)
public abstract class AbstractBuildingViewMixin {

    @Shadow(remap = false) private int buildingLevel;
    @Shadow(remap = false) private int buildingMaxLevel;
    @Shadow(remap = false) private int buildingDmPrio;
    @Shadow(remap = false) private int workOrderLevel;
    @Shadow(remap = false) private String pack;
    @Shadow(remap = false) private String path;
    @Shadow(remap = false) private @NotNull BlockPos parent;
    @Shadow(remap = false) private String customName;
    @Shadow(remap = false) private int rotation;
    @Shadow(remap = false) private boolean isBuildingMirrored;
    @Shadow(remap = false) private int claimRadius;
    @Shadow(remap = false) private ImmutableCollection<IToken<?>> resolvers;
    @Shadow(remap = false) private IToken<?> requesterId;
    @Shadow(remap = false) private List<BlockPos> containerlist;
    @Shadow(remap = false) private boolean isDeconstructed;
    @Shadow(remap = false) private boolean isAssignmentAllowed;
    @Shadow(remap = false) protected Int2ObjectLinkedOpenHashMap<IBuildingModuleView> moduleViews;

    @Shadow(remap = false) protected abstract void loadRequestSystemFromNBT(CompoundTag compound);

    @Unique private Object2ObjectOpenHashMap<String, IBuildingModuleView> moduleViewsByKey = null;

    /**
     * @author ARxyt
     * @reason Try to add a checker to deserialize.
     */
    @Overwrite(remap = false)
    public void deserialize(@NotNull final FriendlyByteBuf buf)
    {
        buildingLevel = buf.readInt();
        buildingMaxLevel = buf.readInt();
        buildingDmPrio = buf.readInt();
        workOrderLevel = buf.readInt();
        pack = buf.readUtf(32767);
        path = buf.readUtf(32767);
        parent = buf.readBlockPos();
        customName = buf.readUtf(32767);

        rotation = buf.readInt();
        isBuildingMirrored = buf.readBoolean();
        claimRadius = buf.readInt();

        final List<IToken<?>> list = new ArrayList<>();
        final int resolverSize = buf.readInt();
        for (int i = 0; i < resolverSize; i++)
        {
            final CompoundTag compound = buf.readNbt();
            if (compound != null)
            {
                list.add(StandardFactoryController.getInstance().deserialize(compound));
            }
        }

        resolvers = ImmutableList.copyOf(list);
        final CompoundTag compound = buf.readNbt();
        if (compound != null)
        {
            requesterId = StandardFactoryController.getInstance().deserialize(compound);
        }
        containerlist.clear();
        final int racks = buf.readInt();
        for (int i = 0; i < racks; i++)
        {
            containerlist.add(buf.readBlockPos());
        }
        loadRequestSystemFromNBT(buf.readNbt());
        isDeconstructed = buf.readBoolean();
        isAssignmentAllowed = buf.readBoolean();

        if(moduleViewsByKey != null) {
            moduleViewsByKey.clear();
        }
        else {
            moduleViewsByKey = new Object2ObjectOpenHashMap<>();
        }
        for (Int2ObjectMap.Entry<IBuildingModuleView> entry
                : moduleViews.int2ObjectEntrySet())
        {
            IBuildingModuleView view = entry.getValue();
            moduleViewsByKey.put(view.getProducer().key, view);
            System.out.println("<init>:" + customName + " Has module " + view.getProducer().key + ":" + entry.getIntKey());
        }

        for (int i = 0, size = buf.readInt(); i < size; i++)
        {
            boolean moduleRechecked = false;
            int id = buf.readInt();
            String key = buf.readUtf();
            IBuildingModuleView moduleView = moduleViews.get(id);

            if (moduleView == null || !Objects.equals(moduleView.getProducer().key, key))
            {
                moduleView = moduleViewsByKey.get(key);
                moduleRechecked = true;
            }

            if (moduleView == null)
            {
                Log.getLogger().error("Problem during sync: {} missing module view, key={}, id={}",customName , key, id);
                return;
            }
            else{
                System.out.println("<init>:" + customName + " Get module " + moduleView.getProducer().key + ":" + id);
                if(moduleRechecked) {
                    System.out.println("Module runID now is" + moduleView.getProducer().getRuntimeID());
                }
            }

            moduleView.deserialize(buf);
        }
    }
}
