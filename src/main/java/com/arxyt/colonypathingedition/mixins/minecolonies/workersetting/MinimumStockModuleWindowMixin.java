package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.buildings.modules.IMinimumStockModuleView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.modules.MinimumStockModuleWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;

import static com.minecolonies.api.util.constant.WindowConstants.*;

@Mixin(MinimumStockModuleWindow.class)
public class MinimumStockModuleWindowMixin {

    @Final @Shadow(remap = false) private ScrollingList resourceList;
    @Final @Shadow(remap = false) private IMinimumStockModuleView moduleView;

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/client/gui/AbstractModuleWindow;<init>(Lcom/minecolonies/api/colony/buildings/views/IBuildingView;Ljava/lang/String;)V"
            ),
            index = 1,
            remap = false
    )
    private static String modifySuperArg(String original) {
        return original.replace(Constants.MOD_ID, AdditionalContants.MOD_ID);
    }

    /**
     * @author ARxyt
     * @reason Inside method mixin.
     */
    @Overwrite(remap = false)
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
                final ItemStack resource = Objects.requireNonNull(moduleView.getStock().get(index).getA()).getItemStack().copy();
                if(PathingConfig.MINIMUM_STOCK_PRECISE.get()) {
                    resource.setCount(1);
                }
                else{
                    resource.setCount(resource.getMaxStackSize());
                }
                rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class).setText(resource.getHoverName());
                rowPane.findPaneOfTypeByID(QUANTITY_LABEL, Text.class).setText(Component.literal(String.valueOf(moduleView.getStock().get(index).getB())));
                rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class).setItem(resource);
            }
        });
    }
}
