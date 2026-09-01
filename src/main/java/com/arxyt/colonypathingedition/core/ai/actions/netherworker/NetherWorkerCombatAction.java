package com.arxyt.colonypathingedition.core.ai.actions.netherworker;

import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import com.arxyt.colonypathingedition.core.util.SwitchUtils;
import com.arxyt.colonypathingedition.core.util.ToolUtils;
import com.arxyt.colonypathingedition.mixins.minecraft.DamageSourcesAccessor;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.compatibility.tinkers.TinkersToolHelper;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.DamageSourceKeys;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ActionType.*;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.*;
import static com.minecolonies.api.util.constant.CitizenConstants.AVERAGE_SATURATION;
import static com.minecolonies.api.util.constant.CitizenConstants.LOW_SATURATION;
import static com.minecolonies.api.util.constant.GuardConstants.BASE_PHYSICAL_DAMAGE;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_DAMAGE;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_ENTITY_TYPE;
import static com.minecolonies.api.util.constant.StatisticsConstants.MINER_DEATHS;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.NETHERMINER_MENU;

public class NetherWorkerCombatAction extends AdventureActionHandler.Action {
    private final Level world;
    private final boolean extraRound;

    private final AbstractEntityCitizen worker;
    private final int primarySkillLevel;
    private final int secondarySkillLevel;
    private final DamageSource damageSource;
    private final IBuilding building;
    private final List<ItemStack> netherEdible;

    private final LivingEntity mob;
    private float mobDamage = 5.0F;
    private float mobHealth;
    private EntityType<?> mobType = EntityType.ZOMBIE;

    public NetherWorkerCombatAction(Level world, AbstractEntityCitizen worker, AbstractJob<?, ?> job, CompoundTag tag, boolean extraRound) {
        super(COMBAT);
        this.world = world;
        this.extraRound = extraRound;

        this.worker = worker;
        this.primarySkillLevel = worker.getCitizenData().getCitizenSkillHandler().getLevel(((WorkerBuildingModule) job.getWorkModule()).getPrimarySkill());
        this.secondarySkillLevel = worker.getCitizenData().getCitizenSkillHandler().getLevel(((WorkerBuildingModule) job.getWorkModule()).getSecondarySkill());
        this.building = job.getWorkBuilding();
        this.netherEdible = IColonyManager.getInstance()
                        .getCompatibilityManager()
                        .getEdibles(building.getBuildingLevel() - 1)
                        .stream()
                        .map(ItemStorage::getItemStack)
                        .collect(Collectors.toList());
        final Set<ItemStorage> allowedItems = building.getModule(NETHERMINER_MENU).getMenu();
        netherEdible.removeIf(item -> !allowedItems.contains(new ItemStorage(item)));

        if(tag.contains(TAG_DAMAGE)) mobDamage = tag.getFloat(TAG_DAMAGE);

        if (tag.contains(TAG_ENTITY_TYPE))
        {
            mobType = EntityType.byString(tag.getString(TAG_ENTITY_TYPE)).orElse(EntityType.ZOMBIE);
        }
        this.damageSource = ((DamageSourcesAccessor)world.damageSources()).invokerSource(DamageSourceKeys.NETHER);
        this.mob = (LivingEntity) mobType.create(world);
        if(this.mob == null) {
            this.currentActionType = NONE;
            return;
        }
        handleSpecialMobType();
        this.mobHealth = this.mob.getHealth();
    }

    private void handleSpecialMobType() {
        if(mob instanceof MagmaCube magmaCube) {
            magmaCube.setSize(2, true);
        }
    }

    @Override
    protected AdventureActionHandler.ResultType tick() {
        if(this.currentActionType == NONE) {
            return INVALID;
        }

        // Set holding sword.
        worker.setItemSlot(EquipmentSlot.MAINHAND, findTool(ModEquipmentTypes.sword.get()));

        // Success
        if(mobHealth < 0 ) {
            // Generate loot for this mob, special LootParams needed.
            LootParams context = this.getDropLoot();
            LootTable loot = Objects.requireNonNull(world.getServer()).getLootData().getLootTable(mob.getLootTable());
            rewards = new ObjectArrayList<>();
            for (int i = 0; i <= secondarySkillLevel / 30; i++){
                rewards.addAll(loot.getRandomItems(context));
            }
            rewards.removeIf(ItemStack::isEmpty);
            worker.getCitizenExperienceHandler().addExperience(ToolUtils.applyMending(worker, mob.getExperienceReward() * 2));
            final ExpeditionLogModule expeditionLogModule = building.getModule(ExpeditionLogModule.class);
            if(expeditionLogModule != null) {
                final ExpeditionLog expeditionLog = expeditionLogModule.getLog();
                expeditionLog.addMob(mobType);
            }
            return SUCCESS;
        }

        // Combat
        if(!worker.isDeadOrDying()) {
            worker.hurtTime = 0;
            worker.invulnerableTime = 0;
            float damageToDo = BASE_PHYSICAL_DAMAGE;

            // Calculate if the sword still exists, how much damage will be done to the mob
            final ItemStack sword = worker.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!sword.isEmpty())
            {
                if (sword.getItem() instanceof SwordItem)
                {
                    damageToDo += ((SwordItem) sword.getItem()).getDamage();
                }
                else
                {
                    damageToDo += (float) TinkersToolHelper.getDamage(sword);
                }
                damageToDo += (float) (EnchantmentHelper.getDamageBonus(sword, mob.getMobType()) / 2.5);
                sword.hurtAndBreak(1, worker, e -> {});
            }

            // Hit the mob
            mobHealth -= damageToDo;

            // Get hit by the mob
            if (!worker.hurt(damageSource, mobDamage))
            {
                //Shouldn't get here, but if we do we can force the damage.
                float incomingDamage = worker.calculateDamageAfterAbsorbs(damageSource, mobDamage);
                worker.setHealth(worker.getHealth() - incomingDamage);
            }
        }

        // Dead?
        if(worker.isDeadOrDying()) {
            final ExpeditionLogModule expeditionLogModule = building.getModule(ExpeditionLogModule.class);
            if(expeditionLogModule != null) {
                final ExpeditionLog expeditionLog = expeditionLogModule.getLog();
                expeditionLog.setKilled();
            }
            StatsUtil.trackStat(building, MINER_DEATHS, 1);
            // Stop processing loot table data, as the worker died before finishing the trip.
            InventoryUtils.clearItemHandler(worker.getItemHandlerCitizen());
            return FAIL;
        }

        // Heal
        final float saturationFactor = 0.25f;
        if(worker.getCitizenData().getSaturation() > LOW_SATURATION) {
            float healAmount = (float) Math.min((worker.getCitizenData().getSaturation() - LOW_SATURATION + 1) / saturationFactor, worker.getMaxHealth() - worker.getHealth());
            worker.heal(healAmount);
            worker.getCitizenData().decreaseSaturation(healAmount * saturationFactor);
        }
        else {
            float healAmount = (float) Math.min((worker.getCitizenData().getSaturation() >= 1 ? 1 : worker.getCitizenData().getSaturation()) / saturationFactor, worker.getMaxHealth() - worker.getHealth());
            worker.heal(healAmount);
            worker.getCitizenData().decreaseSaturation(healAmount * saturationFactor);
        }

        // Eat food
        if (worker.getCitizenData().getSaturation() < AVERAGE_SATURATION)
        {
            final IDeliverable edible = new StackList(netherEdible, "Edible Food", 1);
            final int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(worker, edible::matches);
            if (slot > -1)
            {
                final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
                SwitchUtils.consumeFoodSwitcher(stack, worker, null);
            }
        }

        // Try to escape
        if (extraRound && InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) < 8) {
            return ESCAPE;
        }
        else if (worker.getCitizenData().getSaturation() < LOW_SATURATION || worker.getHealth() < worker.getMaxHealth() * 0.2)
        {
            if (worker.getRandom().nextFloat() < secondarySkillLevel / 200.0F) return ESCAPE;
        }

        return IN_PROGRESS;
    }

    private ItemStack findItem(@NotNull final Predicate<ItemStack> predicate)
    {
        int slotOfStack = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getItemHandlerCitizen(), predicate);
        return slotOfStack < 0 ? ItemStack.EMPTY : worker.getInventoryCitizen().getStackInSlot(slotOfStack);
    }

    private ItemStack findTool(@NotNull final EquipmentTypeEntry tool)
    {
        return findItem(stack -> ItemStackUtils.hasEquipmentLevel(stack, tool, 0, building.getMaxEquipmentLevel()));
    }

    private LootParams getDropLoot() {
        FakePlayer fakePlayer = getFakePlayer();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, worker.getMainHandItem());
        LootParams.Builder builder = (new LootParams.Builder((ServerLevel) this.world))
                .withParameter(LootContextParams.ORIGIN, worker.position())
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.DAMAGE_SOURCE, world.damageSources().playerAttack(fakePlayer))
                .withParameter(LootContextParams.KILLER_ENTITY, fakePlayer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                .withParameter(LootContextParams.TOOL, worker.getMainHandItem())
                .withLuck(1 + primarySkillLevel / 25.0F);
        return builder.create(LootContextParamSets.ENTITY);
    }

    protected FakePlayer getFakePlayer()
    {
        IPermissions permissions = building.getColony().getPermissions();
        return FakePlayerFactory.get((ServerLevel) world, new GameProfile(permissions.getOwner(), permissions.getOwnerName()));
    }
}
