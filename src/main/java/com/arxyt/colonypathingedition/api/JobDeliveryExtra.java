package com.arxyt.colonypathingedition.api;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;

public interface JobDeliveryExtra {
    void setOngoingDeliveries(int target);
    IRequest<IDeliverymanRequestable> pickMoreDeliveryTask();
    boolean checkDeliveryFinished();
    IRequest<IDeliverymanRequestable> getCurrentTaskToDeliver();
}
