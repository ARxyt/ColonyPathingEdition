package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.modules.IMinimumStockModuleView;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import com.ldtteam.structurize.client.gui.WindowSelectRes;
import com.minecolonies.core.network.messages.server.colony.building.AddMinimumStockToBuildingModuleMessage;
import com.minecolonies.core.network.messages.server.colony.building.RemoveMinimumStockFromBuildingModuleMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.WindowConstants.*;
import static com.minecolonies.api.util.constant.WindowConstants.QUANTITY_LABEL;
import static com.minecolonies.api.util.constant.WindowConstants.RESOURCE_ICON;

@OnlyIn(Dist.CLIENT)
public class WindowPreciseMinimumStock extends AbstractModuleWindow<IMinimumStockModuleView> {
    /**
     * The resource string.
     */
    private static final String RESOURCE_STRING = ":gui/layouthuts/layoutminimumstock.xml";

    /**
     * Limit reached label.
     */
    private static final String LABEL_LIMIT_REACHED = "com.minecolonies.coremod.gui.warehouse.limitreached";

    /**
     * Resource scrolling list.
     */
    private final ScrollingList resourceList;

    /**
     * The matching module view to the window.
     */
    private final IMinimumStockModuleView moduleView;

    /**
     * Constructor for the minimum stock window view.
     *
     * @param moduleView the module view.
     */
    public WindowPreciseMinimumStock(
            final IMinimumStockModuleView moduleView)
    {
        super(moduleView, new ResourceLocation(ColonyPathingEdition.MODID + RESOURCE_STRING));

        resourceList = this.window.findPaneOfTypeByID("resourcesstock", ScrollingList.class);
        this.moduleView = moduleView;

        registerButton(STOCK_ADD, this::addStock);
        if (moduleView.hasReachedLimit())
        {
            final ButtonImage button = findPaneOfTypeByID(STOCK_ADD, ButtonImage.class);
            button.setText(Component.translatable(LABEL_LIMIT_REACHED));
            button.setImage(new ResourceLocation(Constants.MOD_ID, "textures/gui/builderhut/builder_button_medium_disabled.png"), false);
        }

        registerButton(STOCK_REMOVE, this::removeStock);
    }

    /**
     * Remove the stock.
     *
     * @param button the button.
     */
    private void removeStock(final Button button)
    {
        final int row = resourceList.getListElementIndexByPane(button);
        final Tuple<ItemStorage, Integer> tuple = moduleView.getStock().get(row);
        moduleView.getStock().remove(row);
        Network.getNetwork().sendToServer(new RemoveMinimumStockFromBuildingModuleMessage(buildingView, tuple.getA().getItemStack(), moduleView.getProducer().getRuntimeID()));
        updateStockList();
    }

    /**
     * Add the stock.
     */
    private void addStock()
    {
        if (!moduleView.hasReachedLimit())
        {
            new WindowSelectRes(this,
                    Component.empty(),
                    null,
                    IColonyManager.getInstance().getCompatibilityManager().getListOfAllItems(),
                    (stack, qty) -> Network.getNetwork().sendToServer(new AddMinimumStockToBuildingModuleMessage(buildingView, stack, qty)),
                    true,
                    Component.translatable("com.minecolonies.coremod.gui.scan.select.stack")).open();
        }
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        updateStockList();
    }

    /**
     * Updates the resource list in the GUI with the info we need.
     */
    private void updateStockList()
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
                return moduleView.getStock().size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStack resource = moduleView.getStock().get(index).getA().getItemStack().copy();
                resource.setCount(1);

                rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class).setText(resource.getHoverName());
                rowPane.findPaneOfTypeByID(QUANTITY_LABEL, Text.class).setText(Component.literal(String.valueOf(moduleView.getStock().get(index).getB())));
                rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class).setItem(resource);
            }
        });
    }
}
