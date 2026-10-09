package com.arxyt.colonypathingedition.api.accessor;

public interface AbstractMinecartAccessor {
    int getLSteps();
    boolean getFlipped();
    double getLx();
    double getLy();
    double getLz();
    double getLxr();
    double getLyr();

    void lStepMinus();
    void filpReverse();
    void setOnRails(boolean onRails);
}
