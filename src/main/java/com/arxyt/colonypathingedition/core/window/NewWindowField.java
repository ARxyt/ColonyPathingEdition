package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.message.FarmFieldResizeMessage;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.tileentities.TileEntityScarecrow;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.api.util.constant.translation.GuiTranslationConstants.FIELD_GUI_ASSIGNED_FARMER;
import static com.minecolonies.api.util.constant.translation.GuiTranslationConstants.FIELD_GUI_NO_ASSIGNED_FARMER;
import static com.minecolonies.core.colony.buildingextensions.FarmField.MAX_RANGE;

// This is a temporary fix dealing with conflicts to tweaks addon
@OnlyIn(Dist.CLIENT)
public class NewWindowField extends AbstractWindowSkeleton {
    private static final String FARM_FIELD_IS_NULL = "com.arxyt.colonypathingedition.core.field_is_null";

    /**
     * The prefix ID of the directional buttons.
     */
    private static final String DIRECTIONAL_BUTTON_ID_PREFIX = "dir-resize-";

    /**
     * The ID of the center icon of the directional buttons.
     */
    private static final String DIRECTIONAL_BUTTON_CENTER_ICON_ID = "dir-center";

    /**
     * The ID of the select seed button.
     */
    private static final String SELECT_SEED_BUTTON_ID = "select-seed";

    /**
     * The ID for the current seed text.
     */
    private static final String CURRENT_SEED_TEXT_ID = "current-seed";

    /**
     * The ID for the current farmer text.
     */
    private static final String CURRENT_FARMER_TEXT_ID = "current-farmer";

    /**
     * The tile entity of the scarecrow.
     */
    @NotNull
    private final TileEntityScarecrow tileEntityScarecrow;

    /**
     * The farm field instance.
     */
    @Nullable
    private FarmField farmField;

    /**
     * Create the field GUI.
     *
     * @param tileEntityScarecrow the scarecrow tile entity.
     */
    public NewWindowField(@NotNull TileEntityScarecrow tileEntityScarecrow)
    {
        super(ResourceLocation.fromNamespaceAndPath(ColonyPathingEdition.MODID, "gui/windowfield.xml"));
        this.tileEntityScarecrow = tileEntityScarecrow;

        registerButton(SELECT_SEED_BUTTON_ID, this::selectSeed);
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            registerButton(DIRECTIONAL_BUTTON_ID_PREFIX + dir.getName() + "-up", this::onDirectionalButtonClick);
            registerButton(DIRECTIONAL_BUTTON_ID_PREFIX + dir.getName() + "-down", this::onDirectionalButtonClick);
        }

        updateAll();
    }

    /**
     * Button handler for selecting a seed.
     */
    private void selectSeed()
    {
        if (this.farmField == null) {
            MessageUtils.format(FARM_FIELD_IS_NULL)
                    .withPriority(MessageUtils.MessagePriority.DANGER)
                    .sendTo(Minecraft.getInstance().player);
            return;
        }
        WindowCropRotation customWindow = new WindowCropRotation(tileEntityScarecrow, farmField,this);
        customWindow.open();
    }

    /**
     * Button handler for clicking on any of the directional buttons.
     *
     * @param button which button was clicked.
     */
    private void onDirectionalButtonClick(Button button)
    {
        if (!button.isEnabled())
        {
            return;
        }

        String directionName = button.getID().replace(DIRECTIONAL_BUTTON_ID_PREFIX, "");
        boolean up_down = directionName.matches(".*down"); // false -> up . true-> down;

        Optional<Direction> direction = Direction.Plane.HORIZONTAL.stream().filter(f -> directionName.matches(f.getName()+".*")).findFirst();

        if (direction.isEmpty())
        {
            return;
        }

        final int currentValue = tileEntityScarecrow.getFieldSize()[direction.get().get2DDataValue()];

        int newRadius = ((currentValue + (up_down? -2 : 0)) % MAX_RANGE) + 1;
        newRadius = newRadius <= 0 ? newRadius + MAX_RANGE : newRadius;
        tileEntityScarecrow.setFieldSize(direction.get(), newRadius);
        button.setText(Component.literal(up_down? "-" : "+"));

        if (tileEntityScarecrow.getCurrentColony() instanceof IColonyView colonyView) {
            new FarmFieldResizeMessage(colonyView, newRadius, direction.get(), tileEntityScarecrow.getBlockPos()).sendToServer();
        }
    }

    private void updateAll()
    {
        updateFarmField();
        updateElementStates();
        updateOwner();
        updateSeed();
        updateButtons();
    }

    /**
     * Keep attempting to fetch the currently loaded farm field, if not present already.
     */
    private void updateFarmField()
    {
        if (farmField != null)
        {
            return;
        }

        IColonyView colonyView = getCurrentColony();
        if (colonyView == null)
        {
            return;
        }

        final @NotNull List<IBuildingExtension> fields = colonyView.getClientBuildingManager()
                .getBuildingExtensions(otherField -> otherField.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && otherField.getPosition()
                        .equals(tileEntityScarecrow.getBlockPos()));
        if (!fields.isEmpty() && fields.getFirst() instanceof FarmField farmFieldFound)
        {
            farmField = farmFieldFound;
        }
    }

    /**
     * Updates the states of certain additional elements, determining whether they should be enabled/visible.
     */
    private void updateElementStates()
    {
        IColonyView colonyView = getCurrentColony();

        findPaneOfTypeByID(CURRENT_FARMER_TEXT_ID, Text.class).setVisible(colonyView != null);
        findPaneOfTypeByID(SELECT_SEED_BUTTON_ID, ButtonImage.class).setVisible(colonyView != null);
        findPaneOfTypeByID(CURRENT_SEED_TEXT_ID, ItemIcon.class).setVisible(colonyView != null);
        findPaneOfTypeByID(DIRECTIONAL_BUTTON_CENTER_ICON_ID, ItemIcon.class).setVisible(colonyView != null);
    }

    /**
     * Update the label which farmer owns the field, if any.
     */
    private void updateOwner()
    {
        findPaneOfTypeByID(CURRENT_FARMER_TEXT_ID, Text.class).setText(Component.translatable(FIELD_GUI_NO_ASSIGNED_FARMER));

        IColonyView colonyView = getCurrentColony();
        if (colonyView == null || farmField == null || !farmField.isTaken())
        {
            return;
        }

        final IBuildingView building = colonyView.getClientBuildingManager().getBuilding(farmField.getBuildingId());
        if (building == null)
        {
            return;
        }

        final Integer citizenId = building.getAllAssignedCitizens().stream().findFirst().orElse(null);
        if (citizenId == null)
        {
            return;
        }

        ICitizen citizen = colonyView.getCitizen(citizenId);
        if (citizen == null)
        {
            return;
        }

        findPaneOfTypeByID(CURRENT_FARMER_TEXT_ID, Text.class).setText(Component.translatable(FIELD_GUI_ASSIGNED_FARMER, citizen.getName()));
    }

    /**
     * Updates the seed icon next to the selection button.
     */
    private void updateSeed()
    {
        if (farmField != null)
        {
            findPaneOfTypeByID(CURRENT_SEED_TEXT_ID, ItemIcon.class).setItem(farmField.getSeed());
        }
    }

    /**
     * Updates the directional buttons.
     */
    private void updateButtons()
    {
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            Text text = findPaneOfTypeByID(DIRECTIONAL_BUTTON_ID_PREFIX + dir.getName(), Text.class);
            text.setText(Component.literal(Integer.toString(tileEntityScarecrow.getFieldSize()[dir.get2DDataValue()])));

            ButtonImage button1 = findPaneOfTypeByID(DIRECTIONAL_BUTTON_ID_PREFIX + dir.getName() +"-up", ButtonImage.class);
            ButtonImage button2 = findPaneOfTypeByID(DIRECTIONAL_BUTTON_ID_PREFIX + dir.getName() +"-down", ButtonImage.class);

            PaneBuilders.tooltipBuilder()
                    .hoverPane(button1)
                    .append(Component.translatable(PARTIAL_BLOCK_HUT_FIELD_DIRECTION_ABSOLUTE + dir.getSerializedName()))
                    .appendNL(Component.translatable(getDirectionalTranslationKey(dir)).setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY)))
                    .build();
            PaneBuilders.tooltipBuilder()
                    .hoverPane(button2)
                    .append(Component.translatable(PARTIAL_BLOCK_HUT_FIELD_DIRECTION_ABSOLUTE + dir.getSerializedName()))
                    .appendNL(Component.translatable(getDirectionalTranslationKey(dir)).setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY)))
                    .build();
        }
    }

    /**
     * Get the current colony, if any, from the tile entity.
     *
     * @return the colony view, if exists.
     */
    @Nullable
    private IColonyView getCurrentColony()
    {
        if (tileEntityScarecrow.getCurrentColony() instanceof IColonyView colonyView)
        {
            return colonyView;
        }
        return null;
    }

    /**
     * Get translation keys for the different directional buttons.
     *
     * @param direction the direction.
     * @return the translation key.
     */
    private String getDirectionalTranslationKey(Direction direction)
    {
        assert Minecraft.getInstance().player != null;
        Direction[] looks = Direction.orderedByNearest(Minecraft.getInstance().player);
        Direction facing = looks[0].getAxis() == Direction.Axis.Y ? looks[1] : looks[0];

        return switch (facing.getOpposite().get2DDataValue() - direction.get2DDataValue())
        {
            case 1, -3 -> BLOCK_HUT_FIELD_DIRECTION_RELATIVE_TO_RIGHT;
            case 2, -2 -> BLOCK_HUT_FIELD_DIRECTION_RELATIVE_OPPOSITE;
            case 3, -1 -> BLOCK_HUT_FIELD_DIRECTION_RELATIVE_TO_LEFT;
            default -> BLOCK_HUT_FIELD_DIRECTION_RELATIVE_NEAREST;
        };
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        updateAll();
    }
}
