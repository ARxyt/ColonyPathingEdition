package com.arxyt.colonypathingedition.api;


public interface PatientExtras {
    /**
     * @return 已经在处理中
     */
    int getEmployed();

    /**
     * 设置其是否在处理中
     */
    void setEmployed(int doctor);

}
