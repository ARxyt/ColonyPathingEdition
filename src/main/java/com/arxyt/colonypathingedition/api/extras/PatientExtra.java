package com.arxyt.colonypathingedition.api.extras;


public interface PatientExtra {
    /**
     * If the patient are treating by healer
     * @return healer Civilian ID
     */
    int getEmployed();

    /**
     * Set healer treating patient.
     * @param doctor: healer Civilian ID
     */
    void setEmployed(int doctor);

}
