package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.api.workersetting.BuildingCookExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(value = BuildingCook.class, remap = false)
public abstract class BuildingCookMixin extends AbstractBuilding implements BuildingCookExtra {
    private final Queue<Integer> customerQueue = new ConcurrentLinkedQueue<>();
    private final Set<Integer> processingCustomers = ConcurrentHashMap.newKeySet();
    private final HashSet<Integer> customersOnTheWay = new HashSet<>();
    private final HashMap<UUID,Integer> playerFed = new HashMap<>();
    private final HashMap<UUID,Integer> playerServing = new HashMap<>();

    public BuildingCookMixin(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    // 预定餐桌
    public void preorderTable(int customerId){
        customersOnTheWay.add(customerId);
        processingCustomers.remove(customerId);
        customerQueue.remove(customerId);
    }

    // 客人到店
    public void reached(int customerId){
        customersOnTheWay.remove(customerId);
    }

    // 查询用餐人数
    public int getCustomerCount(){
        return customersOnTheWay.size() + this.checkSize();
    }

    // 查询预定人员
    public List<Integer> getOrders(){
        return customersOnTheWay.stream().toList();
    }

    // 分片获取 Customers
    public List<Integer> getCustomers(int maxCount) {
        List<Integer> assigned = new ArrayList<>();
        while (assigned.size() < maxCount && !customerQueue.isEmpty()) {
            Integer customerId = customerQueue.poll();
            if (customerId != null && processingCustomers.add(customerId)) {
                assigned.add(customerId);
            }
        }
        return assigned;
    }

    // 释放 Customer
    public void releaseProcessingCustomer(int customerId, boolean requeue) {
        processingCustomers.remove(customerId);
        if (requeue) {
            customerQueue.offer(customerId);
        }
    }

    // 完全删除 Customer
    public void deleteCustomer(int customerId) {
        processingCustomers.remove(customerId);
        customerQueue.remove(customerId);
    }

    // 顾客注册
    public void tryRegisterCustomer(int citizenId) {
        reached(citizenId);
        if (!customerQueue.contains(citizenId) && !processingCustomers.contains(citizenId)) {
            customerQueue.offer(citizenId);
        }
    }

    // 顾客监测
    public boolean checkCustomerRegistry(int citizenId){
        return customerQueue.contains(citizenId) || processingCustomers.contains(citizenId);
    }

    // 获取当前顾客数量
    public int checkSize(){
        return customerQueue.size() + processingCustomers.size();
    }

    // 获取当前可提供食物的玩家
    public boolean getPlayCanServe(UUID playerID) {
        return !playerServing.containsKey(playerID) && !playerFed.containsKey(playerID);
    }

    // 增加玩家服务冷却
    public void setPlayerServed(UUID playerID) {
        playerServing.remove(playerID);
        playerFed.put(playerID, 5); // 5 -> 2500 tick, about 2 minutes.
    }

    // 添加正在接收服务的玩家
    public void setPlayerServing(Queue<Player> playerList) {
        for(Player player : playerList) {
            playerServing.put(player.getUUID(), 5);
        }
    }

    // 删除正在接收服务的玩家
    public void removePlayerServing(UUID playerID) {
        playerServing.remove(playerID);
    }
    public void removePlayerListServing(Queue<Player> playerList) {
        for(Player player : playerList) {
            playerServing.remove(player.getUUID());
        }
    }

    @Override
    public void onColonyTick(final IColony colony) {
        super.onColonyTick(colony);
        for(UUID playerID : playerFed.keySet()) {
            int cooldown = playerFed.get(playerID);
            if(--cooldown <= 0){
                playerFed.remove(playerID);
            }
            else{
                playerFed.replace(playerID, cooldown);
            }
        }
        for(UUID playerID : playerServing.keySet()) {
            int cooldown = playerServing.get(playerID);
            if(--cooldown <= 0){
                playerServing.remove(playerID);
            }
            else{
                playerServing.replace(playerID, cooldown);
            }
        }
    }
}
