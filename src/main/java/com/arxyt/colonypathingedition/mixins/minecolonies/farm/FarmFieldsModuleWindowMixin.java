package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Image;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import com.minecolonies.core.client.gui.modules.FarmFieldsModuleWindow;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.moduleviews.FieldsModuleView;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.minecolonies.api.util.constant.TranslationConstants.FIELD_STATUS;
import static com.minecolonies.api.util.constant.translation.GuiTranslationConstants.FIELD_LIST_LABEL_DISTANCE;

@Mixin(value = FarmFieldsModuleWindow.class, remap = false)
public abstract class FarmFieldsModuleWindowMixin extends AbstractModuleWindow {
    @Shadow(remap = false) @Final private static String HUT_FIELDS_RESOURCE_SUFFIX;
    @Shadow(remap = false) private ScrollingList fieldList;
    @Shadow(remap = false) @Final private static String LIST_FIELDS;
    @Shadow(remap = false) @Final private FieldsModuleView moduleView;
    @Shadow(remap = false) @Final private static String TAG_ICON;
    @Shadow(remap = false) @Final private static String TAG_STAGE_ICON;
    @Shadow(remap = false) @Final private static String TAG_STAGE_TEXT;
    @Shadow(remap = false) @Final private static String TAG_DISTANCE;
    @Shadow(remap = false) @Final private static String TAG_BUTTON_ASSIGN;

    @Shadow(remap = false) protected abstract void setAssignButtonTexture(ButtonImage button, boolean isOn);
    @Shadow(remap = false) protected abstract void updateUI();

    public FarmFieldsModuleWindowMixin(final IBuildingView building)
    {
        super(building, Constants.MOD_ID + HUT_FIELDS_RESOURCE_SUFFIX);
    }

    /**
     * @author ARxyt
     * @reason Shut down caused by this code, remastered.
     */
    @Overwrite(remap = false)
    public void onOpened()
    {
        super.onOpened();
        fieldList = findPaneOfTypeByID(LIST_FIELDS, ScrollingList.class);
        fieldList.setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return moduleView.getFields().size();
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final IBuildingExtension field = moduleView.getFields().get(index);
                if (field instanceof FarmField farmField && !farmField.getSeed().isEmpty())
                {
                    rowPane.findPaneOfTypeByID(TAG_ICON, ItemIcon.class).setItem(farmField.getSeed());
                    rowPane.findPaneOfTypeByID(TAG_STAGE_TEXT, Text.class).setText(Component.translatable(FIELD_STATUS));
                    rowPane.findPaneOfTypeByID(TAG_STAGE_ICON, Image.class).setImage(farmField.getFieldStage().getNextStage().getStageIcon(), true);
                }
                else{
                    rowPane.findPaneOfTypeByID(TAG_STAGE_ICON, Image.class).setImage(FarmField.Stage.EMPTY.getStageIcon(), true);
                }

                final String distance = Integer.toString(field.getSqDistance(buildingView));
                final BlockPosUtil.DirectionResult direction = BlockPosUtil.calcDirection(buildingView.getPosition(), field.getPosition());

                final Component directionText = switch (direction) {
                    case UP, DOWN -> direction.getLongText();
                    default -> Component.translatable(FIELD_LIST_LABEL_DISTANCE, Component.literal(distance + "m"), direction.getShortText());
                };

                rowPane.findPaneOfTypeByID(TAG_DISTANCE, Text.class).setText(directionText);

                final ButtonImage assignButton = rowPane.findPaneOfTypeByID(TAG_BUTTON_ASSIGN, ButtonImage.class);
                assignButton.setEnabled(moduleView.assignFieldManually());
                assignButton.show();
                assignButton.setHoverPane(null);

                if (field.isTaken())
                {
                    setAssignButtonTexture(assignButton, true);
                }
                else
                {
                    // Field may be claimed
                    setAssignButtonTexture(assignButton, false);

                    if (!moduleView.canAssignField(field))
                    {
                        assignButton.disable();

                        MutableComponent warningTooltip = moduleView.getFieldWarningTooltip(field);
                        if (warningTooltip != null && moduleView.assignFieldManually())
                        {
                            PaneBuilders.tooltipBuilder()
                                    .append(warningTooltip.withStyle(ChatFormatting.RED))
                                    .hoverPane(assignButton)
                                    .build();
                        }
                    }
                }
            }
        });

        updateUI();
    }
}
