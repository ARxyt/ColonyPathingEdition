package com.arxyt.colonypathingedition.mixins.minecolonies.plantation;

import com.ldtteam.structurize.blocks.interfaces.IAnchorBlock;
import com.minecolonies.api.blocks.AbstractBlockMinecoloniesHorizontal;
import com.minecolonies.api.blocks.interfaces.IBuildingBrowsableBlock;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.entity.ai.workers.util.IBuilderUndestroyable;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.blocks.BlockPlantationField;
import com.minecolonies.core.client.gui.WindowPlantationField;
import com.minecolonies.core.colony.buildingextensions.PlantationField;
import com.minecolonies.core.tileentities.TileEntityPlantationField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(BlockPlantationField.class)
public abstract class BlockPlantationFieldMixin extends AbstractBlockMinecoloniesHorizontal<BlockPlantationField> implements IBuilderUndestroyable, IAnchorBlock, IBuildingBrowsableBlock, EntityBlock {

    public BlockPlantationFieldMixin(final Properties properties)
    {
        super(properties);
    }

    /**
     * @author ARxyt
     * @reason Sync plantation
     */
    @Overwrite(remap = false)
    public @NotNull ItemInteractionResult useItemOn(
            final @NotNull ItemStack stack,
            final @NotNull BlockState state,
            final Level worldIn,
            final @NotNull BlockPos pos,
            final @NotNull Player player,
            final @NotNull InteractionHand hand,
            final @NotNull BlockHitResult ray)
    {
        // If this is the client side, open the plantation field GUI
        if (worldIn.isClientSide)
        {
            if (hand == InteractionHand.OFF_HAND)
            {
                return ItemInteractionResult.FAIL;
            }

            final BlockEntity tileEntity = worldIn.getBlockEntity(pos);
            if (tileEntity instanceof TileEntityPlantationField plantationField)
            {
                new WindowPlantationField(plantationField).open();
                return ItemInteractionResult.SUCCESS;
            }

            return ItemInteractionResult.FAIL;
        }

        final BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityPlantationField tileEntityPlantationField)
        {
            final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(worldIn, pos);
            if (colony != null)
            {
                List<IBuildingExtension> extensions = colony.getServerBuildingManager().getBuildingExtensions(extension ->  extension.getPosition().equals(pos));
                for (IBuildingExtension plantationField : extensions) {
                    if (!tileEntityPlantationField.getPlantationFieldTypes().contains(plantationField.getBuildingExtensionType())) {
                        colony.getServerBuildingManager().removeBuildingExtension(extension -> extension.getBuildingExtensionType() == plantationField.getBuildingExtensionType() && extension.getPosition().equals(pos));
                    }
                }
                for (BuildingExtensionRegistries.BuildingExtensionEntry plantationFieldType : tileEntityPlantationField.getPlantationFieldTypes())
                {
                    if(extensions.stream().anyMatch(extension -> extension.getBuildingExtensionType() == plantationFieldType)) {
                        continue;
                    }
                    final PlantationField plantationField = PlantationField.create(plantationFieldType, pos);

                    final List<BlockPos> workingPositions = tileEntityPlantationField.getWorkingPositions(plantationField.getModule().getWorkTag());
                    if (workingPositions.isEmpty())
                    {
                        Log.getLogger()
                                .warn("Plantation field blueprint at path {} does not have ANY tagged working positions for the tag '{}', please report this to devs!",
                                        tileEntityPlantationField.getBlueprintPath(),
                                        plantationField.getModule().getWorkTag());
                    }

                    final List<BlockPos> validPositions = plantationField.getModule().getValidWorkingPositions(worldIn, workingPositions);
                    if (!validPositions.isEmpty())
                    {
                        plantationField.setWorkingPositions(validPositions);
                        colony.getServerBuildingManager().addBuildingExtension(plantationField);
                        colony.getServerBuildingManager().addLeisureSite(pos);
                    }
                    else
                    {
                        Log.getLogger()
                                .warn("Plantation field blueprint at path {} does not have ANY VALID tagged working positions for the tag '{}', please report this to devs!",
                                        tileEntityPlantationField.getBlueprintPath(),
                                        plantationField.getModule().getWorkTag());
                    }
                }
            }
        }
        // This must succeed in Remote to stop more right click interactions like placing blocks
        return ItemInteractionResult.SUCCESS;
    }
}
