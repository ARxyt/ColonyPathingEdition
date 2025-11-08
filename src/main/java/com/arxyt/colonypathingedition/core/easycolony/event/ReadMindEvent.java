package com.arxyt.colonypathingedition.core.easycolony.event;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.minecolonies.api.entity.ai.ITickingStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.READ_MIND_ITEM;

/**
 * 此功能为从简易殖民地迁移而来。
 * This feature has been migrated from EasyColony.
 * @author sxtkl
 * @since 2025/11/8
 */
public class ReadMindEvent {

    @SubscribeEvent
    public void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        if (!PathingConfig.ALLOW_READ_MIND.get()) return;
        if (event.getItemStack().getItem() != BuiltInRegistries.ITEM.get(ResourceLocation.parse(READ_MIND_ITEM.get()))) return;
        if (!(event.getTarget() instanceof EntityCitizen citizen)) return;
        if (event.getEntity().isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getSide().isClient()) return;
        IState stat;
        ITickingStateAI workAI = citizen.getCitizenJobHandler().getWorkAI();
        if (workAI == null) {
            stat = citizen.getCitizenAI().getState();
        } else {
            stat = workAI.getState();
        }
        Component msg = Component.translatable(AdditionalContants.READ_MIND)
                .append(stat.toString());
        event.getEntity().sendSystemMessage(msg);
    }
}
