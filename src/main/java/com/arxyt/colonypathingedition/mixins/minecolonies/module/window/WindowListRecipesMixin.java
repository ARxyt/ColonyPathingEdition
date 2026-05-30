package com.arxyt.colonypathingedition.mixins.minecolonies.module.window;

import com.arxyt.colonypathingedition.api.CraftingModuleViewExtra;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Gradient;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import com.minecolonies.core.client.gui.modules.building.WindowListRecipes;
import com.minecolonies.core.colony.buildings.moduleviews.CraftingModuleView;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static com.minecolonies.api.util.constant.WindowConstants.*;
import static com.minecolonies.api.util.constant.WindowConstants.BUTTON_TOGGLE;


@Mixin(WindowListRecipes.class)
public abstract class WindowListRecipesMixin extends AbstractModuleWindow<CraftingModuleView> {
    @Final @Shadow(remap = false) private ScrollingList recipeList;
    @Final @Shadow(remap = false) private Text recipeStatus;
    @Final @Shadow(remap = false) private static String RECIPE_LIST;
    @Shadow(remap = false) private int lifeCount;
    @Shadow(remap = false) private static final String OUTPUT_ICON = "output";
    @Shadow(remap = false) private static final String RESOURCE = "res%d";

    @Shadow(remap = false) protected abstract ItemStack getStackWithCount(final ItemStorage storage);

    public WindowListRecipesMixin(final CraftingModuleView module)
    {
        super(module, new ResourceLocation(Constants.MOD_ID, "gui/layouthuts/layoutlistrecipes.xml"));
    }

    @Override
    public void onOpened()
    {
        recipeList.enable();
        recipeList.show();

        //Creates a dataProvider for the homeless recipeList.
        recipeList.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return moduleView.getRecipes().size();
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                @NotNull final IRecipeStorage recipe = moduleView.getRecipes().get(index);
                final ItemIcon icon = rowPane.findPaneOfTypeByID(OUTPUT_ICON, ItemIcon.class);
                List<ItemStack> displayStacks = recipe.getRecipeType().getOutputDisplayStacks();
                icon.setItem(displayStacks.get((lifeCount / LIFE_COUNT_DIVIDER) % (displayStacks.size())));

                boolean isBuiltIn = false;
                final Button removeButton = rowPane.findPaneOfTypeByID(BUTTON_REMOVE, Button.class);
                if (removeButton != null)
                {
                    if (moduleView.isRecipeAlterationAllowed())
                    {
                        removeButton.on();
                        if (recipe.getRecipeSource() != null && !Screen.hasControlDown())
                        {
                            isBuiltIn = true;
                            removeButton.disable();
                            PaneBuilders.tooltipBuilder()
                                    .append(Component.translatable("com.minecolonies.coremod.gui.workerhuts.removebuiltin",
                                            Component.translatable("key.keyboard.left.control")))
                                    .hoverPane(removeButton)
                                    .build();
                        }
                        else
                        {
                            removeButton.setHoverPane(null);
                        }
                    }
                    else
                    {
                        removeButton.off();
                    }
                }

                final Text intermediate = rowPane.findPaneOfTypeByID("intermediate", Text.class);
                intermediate.setVisible(false);
                if (recipe.getRequiredTool() != ModEquipmentTypes.none.get())
                {
                    intermediate.setText(recipe.getRequiredTool().getDisplayName());
                    intermediate.setVisible(true);
                }
                else if(recipe.getIntermediate() != Blocks.AIR)
                {
                    intermediate.setText(recipe.getIntermediate().getName());
                    //intermediate.setVisible(true);
                }

                if (moduleView.isDisabled(recipe))
                {
                    rowPane.findPaneOfTypeByID("gradient", Gradient.class).setVisible(true);
                    rowPane.findPaneOfTypeByID(BUTTON_TOGGLE, Button.class).setText(Component.translatable("com.minecolonies.coremod.gui.recipe.enable"));
                    rowPane.findPaneOfTypeByID(BUTTON_TOGGLE, Button.class).setVisible(isBuiltIn || moduleView.getActiveRecipes() - ((CraftingModuleViewExtra)moduleView).getActivePreTaughtRecipes() < moduleView.getMaxRecipes());
                }
                else
                {
                    rowPane.findPaneOfTypeByID("gradient", Gradient.class).setVisible(false);
                    rowPane.findPaneOfTypeByID(BUTTON_TOGGLE, Button.class).setText(Component.translatable("com.minecolonies.coremod.gui.recipe.disable"));
                    rowPane.findPaneOfTypeByID(BUTTON_TOGGLE, Button.class).setVisible(true);
                }

                boolean skipThird = recipe.getInput().size() == 4;
                int j = 0;
                for (int i = 0; i < Math.min(9, recipe.getInput().size()); i++)
                {
                    if(skipThird && i == 2){
                        j++;
                    }
                    rowPane.findPaneOfTypeByID(String.format(RESOURCE, ++j), ItemIcon.class).setItem(getStackWithCount(recipe.getInput().get(i)));
                }
            }
        });
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (!Screen.hasShiftDown())
        {
            lifeCount++;
        }
        recipeStatus.setText(Component.translatable(TranslationConstants.RECIPE_STATUS, moduleView.getActiveRecipes() - ((CraftingModuleViewExtra)moduleView).getActivePreTaughtRecipes() + " (+" + ((CraftingModuleViewExtra)moduleView).getActivePreTaughtRecipes() + ")", moduleView.getMaxRecipes()));
        window.findPaneOfTypeByID(RECIPE_LIST, ScrollingList.class).refreshElementPanes();
    }
}
