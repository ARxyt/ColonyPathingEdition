package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.api.workersetting.BuildingCookExtra;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(BuildingCook.class)
public class BuildingCookForCustomerMixin implements BuildingCookExtra {
    private final Queue<Integer> customerQueue = new ConcurrentLinkedQueue<>();
    private final Set<Integer> processingCustomers = ConcurrentHashMap.newKeySet();

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
}
