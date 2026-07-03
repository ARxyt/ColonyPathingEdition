package com.arxyt.colonypathingedition.api.workersetting;

import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

public interface BuildingCookExtra {
    void preorderTable(int customerId);
    void reached(int customerId);
    int getCustomerCount();
    List<Integer> getOrders();
    List<Integer> getCustomers(int maxCount);
    void releaseProcessingCustomer(int customerId, boolean requeue);
    void deleteCustomer(int customerId);
    void tryRegisterCustomer(int citizenId);
    boolean checkCustomerRegistry(int citizenId);
    int checkSize();
    boolean getPlayCanServe(UUID playerID);
    void setPlayerServed(UUID playerID);
    void setPlayerServing(Queue<Player> playerList);
    void removePlayerServing(UUID playerID);
    void removePlayerListServing(Queue<Player> playerList);
}
