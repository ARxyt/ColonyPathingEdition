package com.arxyt.colonypathingedition.core.easycolony.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.minecolonies.api.entity.ai.ITickingStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.READ_MIND_ITEM;
import static com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState.WORKING;

/**
 * 此功能为从简易殖民地迁移而来。
 * This feature has been migrated from EasyColony.
 * @author sxtkl
 * @since 2025/11/8
 */
@Mod.EventBusSubscriber(modid = ColonyPathingEdition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReadMindEvent {

    @SubscribeEvent
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        if (!PathingConfig.ALLOW_READ_MIND.get()) return;
        if (event.getItemStack().getItem() != ForgeRegistries.ITEMS.getValue(new ResourceLocation(READ_MIND_ITEM.get()))) return;
        if (!(event.getTarget() instanceof EntityCitizen citizen)) return;
        if (event.getEntity().isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getSide().isClient()) return;
        IState stat = citizen.getCitizenAI().getState();
        ITickingStateAI workAI = citizen.getCitizenJobHandler().getWorkAI();
        if (stat == WORKING && workAI != null) {
            stat = workAI.getState();
        }
        Component msg = Component.translatable(AdditionalContants.READ_MIND)
                .append(Component.translatable(AdditionalContants.READ_MIND_STATE + "." + stat.toString()).withStyle(ChatFormatting.GREEN));
        event.getEntity().sendSystemMessage(msg);
    }
}
