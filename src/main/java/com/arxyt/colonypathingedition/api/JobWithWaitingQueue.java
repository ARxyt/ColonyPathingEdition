package com.arxyt.colonypathingedition.api;

public interface JobWithWaitingQueue {
    void setWaitingForJob(boolean isWaiting);
    boolean isWaiting();
}
