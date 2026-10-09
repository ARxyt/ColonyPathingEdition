package com.arxyt.colonypathingedition.api.extras;


public interface PatientExtra {
    /**
     * @return 已经在处理中
     */
    int getEmployed();

    /**
     * 设置其是否在处理中
     */
    void setEmployed(int doctor);

}
