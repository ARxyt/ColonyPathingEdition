package com.arxyt.colonypathingedition.core.ai.actions.netherworker;

import com.arxyt.colonypathingedition.api.ExpeditionLogExtra;
import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ActionType.TRADE;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.INVALID;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.SUCCESS;

public class NetherWorkerPiglinTradeAction extends AdventureActionHandler.Action {
    private final Level world;

    private final AbstractEntityCitizen worker;
    private final int primarySkillLevel;
    private final JobNetherWorker job;
    private final IBuilding building;

    private ResourceLocation tradeResource = null;
    private ItemStack cost = ItemStack.EMPTY;
    private ItemStack optionalCost = ItemStack.EMPTY;

    public NetherWorkerPiglinTradeAction(Level world, AbstractEntityCitizen worker, JobNetherWorker job, CompoundTag tag){
        super(TRADE);
        this.world = world;
        this.worker = worker;
        this.primarySkillLevel = worker.getCitizenData().getCitizenSkillHandler().getLevel(((WorkerBuildingModule) job.getWorkModule()).getPrimarySkill());
        this.job = job;
        this.building = job.getWorkBuilding();
        if(tag.contains("tradeLoot")) {
            this.tradeResource = new ResourceLocation(tag.getString("tradeLoot"));
        }
        if(tag.contains("cost")) {
            ResourceLocation itemResourceLocation = new ResourceLocation(tag.getString("cost"));
            Item costItem = ForgeRegistries.ITEMS.getValue(itemResourceLocation);
            if(costItem != null) {
                int costCount = 1;
                if(tag.contains("costCount")) {
                    costCount = tag.getInt("costCount");
                    costCount = costCount <= 0 ? 1 : costCount;
                }
                this.cost = new ItemStack(costItem, costCount);
            }
        }
        if(tag.contains("optionalCost")) {
            ResourceLocation itemResourceLocation = new ResourceLocation(tag.getString("optionalCost"));
            Item costItem = ForgeRegistries.ITEMS.getValue(itemResourceLocation);
            if(costItem != null) {
                int costCount = 1;
                if(tag.contains("optionalCostCount")) {
                    costCount = tag.getInt("optionalCostCount");
                    costCount = costCount <= 0 ? 1 : costCount;
                }
                this.optionalCost = new ItemStack(costItem, costCount);
            }
        }
    }

    @Override
    protected AdventureActionHandler.ResultType tick() {
        if(tradeResource == null) {
            return INVALID;
        }
        final LootTable loot = Objects.requireNonNull(world.getServer()).getLootData().getLootTable(tradeResource);
        if(loot.equals(LootTable.EMPTY)) {
            return INVALID;
        }
        boolean success = true;
        if(!cost.isEmpty()) {
            success = consumeTradeCost();
        }
        if(success) {
            final LootParams context = getDropLoot();
            rewards.addAll(loot.getRandomItems(context));
            rewards.removeIf(ItemStack::isEmpty);
            return SUCCESS;
        }
        return INVALID;
    }

    private boolean consumeTradeCost() {
        boolean success;
        boolean optionalSuccess = false;
        final ExpeditionLogModule expeditionLogModule = building.getModule(ExpeditionLogModule.class);
        if(expeditionLogModule != null && expeditionLogModule.getLog() instanceof ExpeditionLogExtra expeditionLog) {
            success = expeditionLog.removeLoot(cost.copy());
            if (!success && !optionalCost.isEmpty()){
                optionalSuccess = expeditionLog.removeLoot(optionalCost.copy());
            }
        }
        else {
            Map<Item, Integer> map = job.getProcessedResults().stream().filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.groupingBy(ItemStack::getItem, Collectors.summingInt(ItemStack::getCount)));
            success = map.containsKey(cost.getItem()) && map.get(cost.getItem()) >= cost.getCount();
            optionalSuccess = !optionalCost.isEmpty() && map.containsKey(optionalCost.getItem()) && map.get(optionalCost.getItem()) >= optionalCost.getCount();
        }
        if (success) {
            List<ItemStack> stackList = job.getProcessedResults().stream().toList();
            int totalDeleted = 0;
            for (ItemStack stack : stackList) {
                if(stack.getItem().equals(cost.getItem())) {
                    totalDeleted += stack.getCount();
                    job.getProcessedResults().remove(stack);
                }
                if(totalDeleted == cost.getCount()) {
                    break;
                }
                if(totalDeleted >= cost.getCount()) {
                    cost.setCount(totalDeleted - cost.getCount());
                    job.getProcessedResults().add(cost);
                    break;
                }
            }
            return true;
        }
        else if (optionalSuccess) {
            List<ItemStack> stackList = job.getProcessedResults().stream().toList();
            int totalDeleted = 0;
            for (ItemStack stack : stackList) {
                if(stack.getItem().equals(optionalCost.getItem())) {
                    totalDeleted += stack.getCount();
                    job.getProcessedResults().remove(stack);
                }
                if(totalDeleted == optionalCost.getCount()) {
                    break;
                }
                if(totalDeleted >= optionalCost.getCount()) {
                    optionalCost.setCount(totalDeleted - optionalCost.getCount());
                    job.getProcessedResults().add(optionalCost);
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private LootParams getDropLoot() {
        LivingEntity piglin = EntityType.PIGLIN.create(world);
        if(piglin == null) {
            piglin = worker;
        }
        LootParams.Builder builder = (new LootParams.Builder((ServerLevel) this.world))
                .withParameter(LootContextParams.THIS_ENTITY, piglin)
                .withParameter(LootContextParams.ORIGIN, worker.position())
                .withLuck(1 + primarySkillLevel / 25.0F);
        return builder.create(LootContextParamSets.PIGLIN_BARTER);
    }

}
