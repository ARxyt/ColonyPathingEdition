package com.arxyt.colonypathingedition.api.workersetting;

import org.spongepowered.asm.mixin.Unique;

public interface BuildingPickupExtra {
    int shouldPickup (final int ickUpPriority, final int qty);
    boolean newCreatePickupRequest(int pickUpPriority);
}
