package com.arxyt.colonypathingedition.core.ai.actions.netherworker;

import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.arxyt.colonypathingedition.core.util.ToolUtils;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Objects;

import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ActionType.MINING;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.INVALID;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.SUCCESS;
import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.ADVENTURE_DELAY;
import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_BREAK_SPEED;

public class NetherWorkerMiningAction extends AdventureActionHandler.Action{
    private static final int NO_TOOL = -10;

    private final Level world;

    private final AbstractEntityCitizen worker;
    private final AbstractJob<?, ?> job;
    private final int primarySkillLevel;
    private final IBuilding building;
    private final ItemStack currStack;
    private final int toolSlot;

    public NetherWorkerMiningAction(Level world, AbstractEntityCitizen worker, AbstractJob<?, ?> job, ItemStack currStack, int toolSlot){
        super(MINING);
        this.world = world;
        this.worker = worker;
        this.primarySkillLevel = worker.getCitizenData().getCitizenSkillHandler().getLevel(((WorkerBuildingModule) job.getWorkModule()).getPrimarySkill());
        this.job = job;
        this.building = job.getWorkBuilding();
        this.currStack = currStack;
        this.toolSlot = toolSlot;
    }

    @Override
    protected AdventureActionHandler.ResultType tick() {
        if (currStack.getItem() instanceof BlockItem bi)
        {
            final Block block = bi.getBlock();
            ItemStack tool = toolSlot < 0 ? ItemStack.EMPTY : worker.getInventoryCitizen().getStackInSlot(toolSlot);
            if (toolSlot == NO_TOOL || tool.getItem() instanceof TieredItem)
            {
                if(toolSlot != NO_TOOL) {
                    worker.setItemSlot(EquipmentSlot.MAINHAND, tool);
                }
                else {
                    worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }

                final BlockState state = block.defaultBlockState();
                int xpGain = 0;
                for (int i = 0; i < currStack.getCount(); i++)
                {
                    LootParams context = getDropLoot(state);
                    LootTable loot = Objects.requireNonNull(world.getServer()).reloadableRegistries().getLootTable(block.getLootTable());
                    rewards.addAll(loot.getRandomItems(context));
                    xpGain = block.getExpDrop(state, world, worker.blockPosition(), null, null, tool);
                }
                rewards.removeIf(ItemStack::isEmpty);
                worker.getCitizenExperienceHandler().addExperience(ToolUtils.applyMending(worker, xpGain));
                actionDelay = ADVENTURE_DELAY + getBlockMiningTime(state) * currStack.getCount();
                return SUCCESS;
            }
            cannotHandleAlert(block);
        }
        return INVALID;
    }

    private LootParams getDropLoot(BlockState blockState) {
        FakePlayer fakePlayer = getFakePlayer();
        LootParams.Builder builder = (new LootParams.Builder((ServerLevel) this.world))
                .withParameter(LootContextParams.ORIGIN, worker.position())
                .withParameter(LootContextParams.THIS_ENTITY, fakePlayer)
                .withParameter(LootContextParams.BLOCK_STATE, blockState)
                .withParameter(LootContextParams.TOOL, worker.getMainHandItem())
                .withLuck(1 + primarySkillLevel / 25.0F);
        return builder.create(LootContextParamSets.BLOCK);
    }

    protected FakePlayer getFakePlayer()
    {
        IPermissions permissions = building.getColony().getPermissions();
        return FakePlayerFactory.get((ServerLevel) world, new GameProfile(permissions.getOwner(), permissions.getOwnerName()));
    }

    public int getBlockMiningTime(BlockState state)
    {
        worker.getMainHandItem();
        ItemStack stack = worker.getMainHandItem();
        float destroySpeed = stack.getItem().getDestroySpeed(worker.getMainHandItem(), state);

        Holder<Enchantment> efficiencyHolder = world.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        int eff = stack.getEnchantmentLevel(efficiencyHolder);
        if (eff > 0 ) {
            destroySpeed += eff * eff + 1;
        }

        MobEffectInstance haste = worker.getEffect(MobEffects.DIG_SPEED);
        if (haste != null) {
            int level = haste.getAmplifier() + 1;
            destroySpeed *= 1.0F + 0.2F * level;
        }

        MobEffectInstance fatigue = worker.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) {
            int level = fatigue.getAmplifier() + 1;
            destroySpeed *= Math.max(0.05F, 1.0F - 0.2F * level);
        }

        final double multiplier = 1 + worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_BREAK_SPEED);

        return ((int) (30 / (1 + Math.pow(primarySkillLevel, 2.0) / 100)
                * (double) state.getDestroySpeed(world, worker.blockPosition()) / (double) (destroySpeed)
                / multiplier));
    }

    void cannotHandleAlert(Block block)
    {
        final IColony colony = job.getColony();
        MutableComponent message = Component.translatable(
                AdditionalContants.HANDLE_ALERT
        );

        MessageUtils.format("[")
                .append(colony.getName())
                .append("] ")
                .append(job.getJobRegistryEntry().getTranslationKey())
                .append(Component.literal(" "))
                .append(worker.getCustomName())
                .append(Component.literal(": "))
                .append(message)
                .append("[")
                .append(block.getName().withStyle(ChatFormatting.GOLD))
                .append("]")
                .sendTo(colony.getImportantMessageEntityPlayers());
    }
}
