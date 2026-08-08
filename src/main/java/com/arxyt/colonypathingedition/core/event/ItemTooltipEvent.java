package com.arxyt.colonypathingedition.core.event;

import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.client.gui.containers.WindowCitizenInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;

import static com.minecolonies.api.research.util.ResearchConstants.SATURATION;

@OnlyIn(Dist.CLIENT)
public class ItemTooltipEvent {

    @SubscribeEvent
    public static void onItemTooltipEvent(final net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        final Entity eventEntity = event.getEntity();
        if(eventEntity == null) return;
        IColony colony = IMinecoloniesAPI.getInstance().getColonyManager().getIColony(event.getEntity().level(), event.getEntity().blockPosition());
        if (colony == null) return;
        final ItemStack stack = event.getItemStack();
        if (WindowCitizenInventory.activeCitizenInventory != null && ItemStackUtils.ISFOOD.test(stack))
        {
            if (!NewFoodUtils.EDIBLE.test(stack))
            {
                event.getToolTip().add(Component.translatable("com.minecolonies.coremod.item.tooltip.wrongfood").withStyle(ChatFormatting.RED));
                return;
            }

            double foodValue = NewFoodUtils.getFoodValue(stack, stack.get(DataComponents.FOOD), colony.getResearchManager().getResearchEffects().getEffectStrength(SATURATION));
            final int foodTier = Math.min(Math.max((int)(foodValue / 3), 3), 1);

            final ICitizenDataView citizenData = (ICitizenDataView) WindowCitizenInventory.activeCitizenInventory.getCitizenData();
            final IColonyView colonyView = citizenData.getColony();

            final int homeBuildingLevel =
                    colonyView.getClientBuildingManager().getBuilding(citizenData.getHomeBuilding()) == null ? 0 : colonyView.getClientBuildingManager().getBuilding(citizenData.getHomeBuilding()).getBuildingLevel();
            if (NewFoodUtils.canEatLevel(event.getItemStack(), homeBuildingLevel))
            {
                event.getToolTip().add(Component.translatable(TranslationConstants.TIER_TOOLTIP + foodTier).withStyle(ChatFormatting.GRAY));
            }
            else
            {
                event.getToolTip().add(Component.translatable("com.minecolonies.coremod.item.tooltip.needbetterfood").withStyle(ChatFormatting.RED));
            }
        }
    }
}
