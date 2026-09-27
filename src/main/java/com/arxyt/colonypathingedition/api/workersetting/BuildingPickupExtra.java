package com.arxyt.colonypathingedition.api.workersetting;

public interface BuildingPickupExtra {
    int shouldPickup (final int ickUpPriority, final int qty);
    boolean newCreatePickupRequest(int pickUpPriority);
}
