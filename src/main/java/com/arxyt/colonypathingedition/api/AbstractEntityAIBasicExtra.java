package com.arxyt.colonypathingedition.api;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.requestsystem.request.IRequest;

public interface AbstractEntityAIBasicExtra {
     ImmutableList<IRequest<?>> getRequestCannotBeDone();
}
