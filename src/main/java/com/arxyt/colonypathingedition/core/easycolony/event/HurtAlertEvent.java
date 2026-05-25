package com.arxyt.colonypathingedition.core.easycolony.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 此功能为从简易殖民地迁移而来。
 * This feature has been migrated from EasyColony.
 * @author sxtkl
 * @since 2025/11/8
 */
@Mod.EventBusSubscriber(modid = ColonyPathingEdition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HurtAlertEvent {

    @SubscribeEvent
    public static void onLivingHurt(final LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!PathingConfig.HURT_ALERT.get()) return;
        if (event.getSource().getEntity() instanceof EntityCitizen) return;
        if (!(event.getEntity() instanceof EntityCitizen citizen)) return;

        Entity src = event.getSource().getEntity();
        // 判断是否受到的伤害来源为可标记实体
        if (!(src instanceof LivingEntity livingEntity)) return;
        // 判断是否为卫兵
        if (citizen.getCitizenJobHandler().getColonyJob() != null && citizen.getCitizenJobHandler().getColonyJob().isGuard())
            return;

        // 发出通告，说自己被锤了
        MutableComponent message = Component.translatable(
                AdditionalContants.HURT_ALERT,
                src.getDisplayName(),
                (int) citizen.getX(),
                (int) citizen.getY(),
                (int) citizen.getZ()
        );
        message = message.withStyle(ChatFormatting.GOLD);
        // 为攻击市民的生物加入荧光效果，高亮显示其位置
        livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 15));
        IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return;
        final IJob<?> job = citizen.getCitizenJobHandler().getColonyJob();
        if (job != null) {
            MessageUtils.format(job.getJobRegistryEntry().getTranslationKey())
                    .append(Component.literal(" "))
                    .append(citizen.getCustomName())
                    .append(Component.literal(" ("))
                    .append(Integer.toString((int) (citizen.getHealth() - event.getAmount())))
                    .append(Component.literal(" ♥): "))
                    .append(message)
                    .sendTo(colony.getImportantMessageEntityPlayers());
            return;
        }
        MessageUtils.format(citizen.getCustomName())
                .append(Component.literal(" ("))
                .append(Integer.toString((int) (citizen.getHealth() - event.getAmount())))
                .append(Component.literal(" ♥): "))
                .append(message)
                .sendTo(colony.getImportantMessageEntityPlayers());
        //MessageUtils.forCitizen(citizen, message).withPriority(MessageUtils.MessagePriority.IMPORTANT).sendTo(colony.getImportantMessageEntityPlayers());
    }

}
