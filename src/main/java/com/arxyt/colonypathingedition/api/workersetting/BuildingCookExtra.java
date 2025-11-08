package com.arxyt.colonypathingedition.api.workersetting;

import java.util.List;

public interface BuildingCookExtra {
    List<Integer> getCustomers(int maxCount);
    void releaseProcessingCustomer(int customerId, boolean requeue);
    void deleteCustomer(int customerId);
    void tryRegisterCustomer(int citizenId);
    boolean checkCustomerRegistry(int citizenId);
    int checkSize();
}
