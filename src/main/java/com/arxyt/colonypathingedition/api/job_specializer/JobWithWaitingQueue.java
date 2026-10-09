package com.arxyt.colonypathingedition.api.job_specializer;

public interface JobWithWaitingQueue {
    void setWaitingForJob(boolean isWaiting);
    boolean isWaiting();
}
