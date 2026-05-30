package com.arxyt.colonypathingedition.mixins.minecolonies.module;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.modules.*;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.registry.CraftingType;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import com.minecolonies.core.colony.crafting.CustomRecipe;
import com.minecolonies.core.colony.crafting.CustomRecipeManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(AbstractCraftingBuildingModule.class)
public abstract class AbstractCraftingBuildingModuleMixin extends AbstractBuildingModule implements ICraftingBuildingModule, IPersistentModule, ICreatesResolversModule, IHasRequiredItemsModule, ITickingModule {

    @Shadow(remap = false) @Final protected JobEntry jobEntry;
    @Shadow(remap = false) private boolean recipesDirty;
    @Shadow(remap = false) @Final protected List<IToken<?>> recipes;
    @Shadow(remap = false) protected abstract boolean isRecipeCompatibleWithCraftingModule(IToken<?> token);
    @Shadow(remap = false) protected abstract boolean isPreTaughtRecipe(IRecipeStorage storage, Map<ResourceLocation, CustomRecipe> crafterRecipes);
    @Shadow(remap = false) @Final protected List<IToken<?>> disabledRecipes;
    @Shadow(remap = false) protected abstract int getMaxRecipes();

    @Override
    public void serializeToView(@NotNull final RegistryFriendlyByteBuf buf, final boolean fullSync)
    {
        if (jobEntry != null)
        {
            buf.writeBoolean(true);
            buf.writeById(IMinecoloniesAPI.getInstance().getJobRegistry()::getIdOrThrow, jobEntry);
        }
        else
        {
            buf.writeBoolean(false);
        }

        final Set<CraftingType> craftingTypes = this.getSupportedCraftingTypes();
        buf.writeVarInt(craftingTypes.size());
        for (final CraftingType type : craftingTypes)
        {
            buf.writeById(MinecoloniesAPIProxy.getInstance().getCraftingTypeRegistry()::getIdOrThrow, type);
        }

        buf.writeBoolean(recipesDirty || fullSync);
        if (recipesDirty || fullSync)
        {
            final List<IRecipeStorage> storages = new ArrayList<>();
            final List<IRecipeStorage> activePreTaughtStorages = new ArrayList<>();
            final List<IRecipeStorage> disabledStorages = new ArrayList<>();
            final List<IRecipeStorage> disabledPreTaughtStorages = new ArrayList<>();
            final Map<ResourceLocation, CustomRecipe> crafterRecipes = CustomRecipeManager.getInstance().getAllRecipes().getOrDefault(getCustomRecipeKey(), Collections.emptyMap());
            for (final IToken<?> token : new ArrayList<>(recipes))
            {
                final IRecipeStorage storage = IColonyManager.getInstance().getRecipeManager().getRecipes().get(token);

                boolean isPreTaught = false;
                if (storage == null || ((isPreTaught = storage.getRecipeSource() != null) && !crafterRecipes.containsKey(storage.getRecipeSource())) || (
                        !isRecipeCompatibleWithCraftingModule(token) && !isPreTaughtRecipe(storage, crafterRecipes)))
                {
                    removeRecipe(token);
                }
                else
                {
                    storages.add(storage);
                    if (disabledRecipes.contains(token))
                    {
                        if(isPreTaught) disabledPreTaughtStorages.add(storage);
                        else disabledStorages.add(storage);
                    }
                    else{
                        if(isPreTaught) activePreTaughtStorages.add(storage);
                    }
                }
            }

            buf.writeInt(storages.size());
            for (final IRecipeStorage storage : storages)
            {
                StandardFactoryController.getInstance().serialize(buf, storage);
            }

            buf.writeInt(disabledStorages.size());
            for (final IRecipeStorage storage : disabledStorages)
            {
                StandardFactoryController.getInstance().serialize(buf, storage);
            }

            buf.writeInt(activePreTaughtStorages.size());
            for (final IRecipeStorage storage : activePreTaughtStorages)
            {
                StandardFactoryController.getInstance().serialize(buf, storage);
            }

            buf.writeInt(disabledPreTaughtStorages.size());
            for (final IRecipeStorage storage : disabledPreTaughtStorages)
            {
                StandardFactoryController.getInstance().serialize(buf, storage);
            }
        }

        recipesDirty = false;

        buf.writeInt(getMaxRecipes());
        buf.writeUtf(getId());
        buf.writeBoolean(isVisible());
    }

    @Inject(method = "hasSpaceForMoreRecipes", at = @At("HEAD"), remap = false , cancellable = true)
    private void hasSpaceForMoreRecipes(CallbackInfoReturnable<Boolean> cir)
    {
        int activeManualRecipe = 0;
        for (final IToken<?> token : new ArrayList<>(recipes)) {
            if (disabledRecipes.contains(token)) continue;
            final IRecipeStorage storage = IColonyManager.getInstance().getRecipeManager().getRecipes().get(token);
            if (storage != null && storage.getRecipeSource() == null) activeManualRecipe++;
        }
        cir.setReturnValue(getMaxRecipes() > activeManualRecipe);
    }
}
