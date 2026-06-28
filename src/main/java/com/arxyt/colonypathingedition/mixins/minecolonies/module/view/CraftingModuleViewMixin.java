package com.arxyt.colonypathingedition.mixins.minecolonies.module.view;

import com.arxyt.colonypathingedition.api.CraftingModuleViewExtra;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.registry.CraftingType;
import com.minecolonies.core.colony.buildings.moduleviews.CraftingModuleView;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CraftingModuleView.class)
public abstract class CraftingModuleViewMixin extends AbstractBuildingModuleView implements CraftingModuleViewExtra {
    @Shadow(remap = false) private JobEntry jobEntry;
    @Shadow(remap = false) private Set<CraftingType> recipeTypeSet;
    @Shadow(remap = false) @Final protected List<IRecipeStorage> recipes;
    @Shadow(remap = false) @Final protected List<IRecipeStorage> disabledRecipes;
    @Shadow(remap = false) private int maxRecipes;
    @Shadow(remap = false) private String id;
    @Shadow(remap = false) private boolean isVisible;
    @Shadow(remap = false) private int activeRecipes;

    final protected List<IRecipeStorage> preTaughtRecipes = new ArrayList<>();
    final protected List<IRecipeStorage> disabledPreTaughtRecipes = new ArrayList<>();

    @Override
    public void deserialize(@NotNull FriendlyByteBuf buf)
    {
        if (buf.readBoolean())
        {
            this.jobEntry = buf.readRegistryIdSafe(JobEntry.class);
        }
        else
        {
            this.jobEntry = null;
        }

        recipeTypeSet.clear();
        final int size = buf.readVarInt();
        for (int i = 0; i < size; ++i)
        {
            final CraftingType type = buf.readRegistryIdUnsafe(MinecoloniesAPIProxy.getInstance().getCraftingTypeRegistry());
            if (type != null)
            {
                recipeTypeSet.add(type);
            }
        }

        if (buf.readBoolean())
        {
            recipes.clear();
            disabledRecipes.clear();
            preTaughtRecipes.clear();
            disabledPreTaughtRecipes.clear();

            final int recipesSize = buf.readInt();
            for (int i = 0; i < recipesSize; i++)
            {
                final IRecipeStorage storage = StandardFactoryController.getInstance().deserialize(buf.readNbt());
                if (storage != null)
                {
                    recipes.add(storage);
                }
            }

            final int disabledRecipeSize = buf.readInt();
            for (int i = 0; i < disabledRecipeSize; i++)
            {
                final IRecipeStorage storage = StandardFactoryController.getInstance().deserialize(buf.readNbt());
                if (storage != null)
                {
                    disabledRecipes.add(storage);
                }
            }

            final int recipesPreTaughtSize = buf.readInt();
            for (int i = 0; i < recipesPreTaughtSize; i++)
            {
                final IRecipeStorage storage = StandardFactoryController.getInstance().deserialize(buf.readNbt());
                if (storage != null)
                {
                    preTaughtRecipes.add(storage);
                }
            }

            final int disabledRecipePreTaughtSize = buf.readInt();
            for (int i = 0; i < disabledRecipePreTaughtSize; i++)
            {
                final IRecipeStorage storage = StandardFactoryController.getInstance().deserialize(buf.readNbt());
                if (storage != null)
                {
                    preTaughtRecipes.add(storage);
                    disabledRecipes.add(storage);
                    disabledPreTaughtRecipes.add(storage);
                }
            }
        }

        this.activeRecipes = buf.readVarInt();
        this.maxRecipes = buf.readInt();
        this.id = buf.readUtf(32767);
        this.isVisible = buf.readBoolean();
    }

    public int getActiveRecipesQuick() {
        return recipes.size() - disabledRecipes.size() - getActivePreTaughtRecipes();
    }

    public int getActivePreTaughtRecipes() {
        return preTaughtRecipes.size() - disabledPreTaughtRecipes.size();
    }

    @Inject(method = "toggle", at = @At("HEAD"), cancellable = true, remap = false)
    private void remasteredToggle(int row, CallbackInfo ci) {
        final IRecipeStorage storage = recipes.get(row);
        if (disabledRecipes.contains(storage)) disabledRecipes.remove(storage);
        else disabledRecipes.add(storage);
        if (disabledPreTaughtRecipes.contains(storage)) disabledPreTaughtRecipes.remove(storage);
        else if(preTaughtRecipes.contains(storage)) disabledPreTaughtRecipes.add(storage);
        ci.cancel();
    }
}
