package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModuleView;
import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.arxyt.colonypathingedition.core.message.AlterBlackListMenuItemMessage;
import com.arxyt.colonypathingedition.core.message.SyncBlackListMenuItemMessage;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.TranslationConstants.PARTIAL_BLOCK_HUT_FIELD_DIRECTION_ABSOLUTE;
import static com.minecolonies.api.util.constant.WindowConstants.*;
import static org.jline.utils.AttributedStyle.WHITE;

@OnlyIn(Dist.CLIENT)
public class FoodBlackListMenuModuleWindow extends AbstractModuleWindow<FoodBlackListMenuModuleView> {

    public static final String SYNC_BLACK_LIST = "sync_black_list";
    public static final String SYNC_TOOLTIP = "com.arxyt.colonypathingedition.core.black_list.sync_tooltip";
    /**
     * Resource scrolling list.
     */
    private final ScrollingList menuList;

    /**
     * Resource scrolling list.
     */
    protected final ScrollingList resourceList;

    /**
     * The filter for the resource list.
     */
    private String filter = "";

    /**
     * Grouped list that can be further filtered.
     */
    protected List<ItemStorage> groupedItemList;

    /**
     * Grouped list after applying the current temporary filter.
     */
    protected final List<ItemStorage> currentDisplayedList = new ArrayList<>();

    /**
     * Update delay.
     */
    private int tick;

    /**
     * The currently selected menu.
     */
    private List<ItemStorage> menu;

    /**
     * Constructor for the minimum stock window view.
     *
     * @param moduleView the module view.
     */
    public FoodBlackListMenuModuleWindow(final FoodBlackListMenuModuleView moduleView)
    {
        super(moduleView, new ResourceLocation(ColonyPathingEdition.MODID, "gui/layouthuts/layoutfoodblacklist.xml"));

        menuList = this.window.findPaneOfTypeByID("resourcesstock", ScrollingList.class);

        registerButton(BUTTON_SWITCH, this::switchClicked);
        registerButton(STOCK_REMOVE, this::removeStock);
        registerButton(SYNC_BLACK_LIST, this::syncBlackList);

        resourceList = window.findPaneOfTypeByID(LIST_RESOURCES, ScrollingList.class);

        groupedItemList = new ArrayList<>(IColonyManager.getInstance().getCompatibilityManager().getEdibles(0));

        window.findPaneOfTypeByID(INPUT_FILTER, TextField.class).setHandler(input -> {
            final String newFilter = input.getText();
            if (!newFilter.equals(filter))
            {
                filter = newFilter;
                this.tick = 10;
            }
        });
    }

    /**
     * Remove the stock.
     *
     * @param button the button.
     */
    private void removeStock(final Button button)
    {
        final int row = menuList.getListElementIndexByPane(button);
        final ItemStorage storage = menu.get(row);
        moduleView.getMenu().remove(storage);
        Network.getNetwork().sendToServer(AlterBlackListMenuItemMessage.removeMenuItem(buildingView, storage.getItemStack()));
        updateStockList();
    }

    /**
     * Sync the stock.
     *
     * @param button the button.
     */
    private void syncBlackList(final Button button)
    {
        Network.getNetwork().sendToServer(new SyncBlackListMenuItemMessage(buildingView));
        updateStockList();
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        updateStockList();
        updateResources();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (tick > 0 && --tick == 0)
        {
            updateResources();
        }
    }

    /**
     * Fired when assign has been clicked in the field list.
     *
     * @param button clicked button.
     */
    private void switchClicked(@NotNull final Button button)
    {
        final int row = resourceList.getListElementIndexByPane(button);
        final ItemStorage storage = currentDisplayedList.get(row);

        Network.getNetwork().sendToServer(AlterBlackListMenuItemMessage.addMenuItem(buildingView, storage.getItemStack()));
        moduleView.getMenu().add(storage);
        updateStockList();

        resourceList.refreshElementPanes();
    }


    /**
     * Updates the resource list in the GUI with the info we need.
     */
    private void updateStockList()
    {
        menu = new ArrayList<>(moduleView.getMenu());
        applySorting(menu);

        if (menu.isEmpty())
        {
            findPaneByID("warning").show();
        }
        else {
            findPaneByID("warning").hide();
        }

        Pane sync_button = findPaneByID(SYNC_BLACK_LIST);

        if(sync_button != null) {
            sync_button.show();
            PaneBuilders.tooltipBuilder()
                    .hoverPane(sync_button)
                    .append(Component.translatable(SYNC_TOOLTIP))
                    .build();
        }

        menuList.enable();
        menuList.show();

        //Creates a dataProvider for the unemployed resourceList.
        menuList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return menu.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStack resource = menu.get(index).getItemStack().copy();

                rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class).setText(resource.getHoverName());
                rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class).setItem(resource);

                final Gradient gradient = rowPane.findPaneOfTypeByID("gradient", Gradient.class);
                if (resource.getItem() instanceof IMinecoloniesFoodItem foodItem)
                {
                    if (foodItem.getTier() == 3)
                    {
                        gradient.setGradientStart(255, 215, 0, 255);
                        gradient.setGradientEnd(255, 215, 0, 255);
                    }
                    else if (foodItem.getTier() == 2)
                    {
                        gradient.setGradientStart(211, 211, 211, 255);
                        gradient.setGradientEnd(211, 211, 211, 255);
                    }
                    else if (foodItem.getTier() == 1)
                    {
                        gradient.setGradientStart(205, 127, 50, 255);
                        gradient.setGradientEnd(205, 127, 50, 255);
                    }
                }
                else
                {
                    gradient.setGradientStart(0, 0, 0, 0);
                    gradient.setGradientEnd(0, 0, 0, 0);
                }
            }
        });
    }

    /**
     * Update the item list.
     */
    private void updateResources()
    {
        final Predicate<ItemStack> filterPredicate = stack -> filter.isEmpty()
                || LinkageManager.match(stack.getDescriptionId().toLowerCase(Locale.US),filter.toLowerCase(Locale.US))
                || LinkageManager.match(stack.getHoverName().getString().toLowerCase(Locale.US),filter.toLowerCase(Locale.US));
        currentDisplayedList.clear();
        for (final ItemStorage storage : groupedItemList)
        {
            if (filterPredicate.test(storage.getItemStack()))
            {
                currentDisplayedList.add(storage);
            }
        }

        applySorting(currentDisplayedList);

        updateResourceList();
    }

    /**
     * Apply sorting to display list based on the scores.
     * @param displayedList list to apply sorting to.
     */
    protected void applySorting(final List<ItemStorage> displayedList)
    {
        displayedList.sort((o1, o2) -> {
            int score = o1.getItem() instanceof IMinecoloniesFoodItem foodItem ? foodItem.getTier()* -100 : -o1.getItemStack().getFoodProperties(null).getNutrition();
            int score2 = o2.getItem() instanceof IMinecoloniesFoodItem foodItem2 ? foodItem2.getTier()* -100 : -o2.getItemStack().getFoodProperties(null).getNutrition();

            final int scoreComparison = Integer.compare(score, score2);
            if (scoreComparison != 0)
            {
                return scoreComparison;
            }

            return o1.getItemStack().getDisplayName().getString().toLowerCase(Locale.US).compareTo(o2.getItemStack().getDisplayName().getString().toLowerCase(Locale.US));
        });
    }

    /**
     * Updates the resource list in the GUI with the info we need.
     */
    protected void updateResourceList()
    {
        resourceList.enable();
        resourceList.show();

        //Creates a dataProvider for the unemployed resourceList.
        resourceList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return currentDisplayedList.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStack resource = currentDisplayedList.get(index).getItemStack();
                final Text resourceLabel = rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class);
                resourceLabel.setText(resource.getItem().getName(resource).plainCopy());
                resourceLabel.setColors(WHITE);
                final ItemIcon itemIcon = rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class);
                itemIcon.setItem(resource);
                final boolean isInMenu  = moduleView.getMenu().contains(new ItemStorage(resource));
                final Button switchButton = rowPane.findPaneOfTypeByID(BUTTON_SWITCH, Button.class);
                final Gradient gradient = rowPane.findPaneOfTypeByID("gradient", Gradient.class);
                if (resource.getItem() instanceof IMinecoloniesFoodItem foodItem)
                {
                    if (foodItem.getTier() == 3)
                    {
                        gradient.setGradientStart(255, 215, 0, 255);
                        gradient.setGradientEnd(255, 215, 0, 255);
                    }
                    else if (foodItem.getTier() == 2)
                    {
                        gradient.setGradientStart(211, 211, 211, 255);
                        gradient.setGradientEnd(211, 211, 211, 255);
                    }
                    else if (foodItem.getTier() == 1)
                    {
                        gradient.setGradientStart(205, 127, 50, 255);
                        gradient.setGradientEnd(205, 127, 50, 255);
                    }
                }
                else
                {
                    gradient.setGradientStart(0, 0, 0, 0);
                    gradient.setGradientEnd(0, 0, 0, 0);
                }

                if (isInMenu)
                {
                    switchButton.disable();
                }
                else
                {
                    switchButton.enable();
                }
            }
        });
    }
}
