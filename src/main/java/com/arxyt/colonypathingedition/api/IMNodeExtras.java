package com.arxyt.colonypathingedition.api;

public interface IMNodeExtras
{
    /**
     * @return 如果节点在农田上返回 true
     */
    boolean getOnFarmland();

    /**
     * 标记节点在农田上
     */
    void setOnFarmland();

    /**
     * @return 如果节点在半砖（slab）上返回 true
     */
    boolean getOnSlab();

    /**
     * 标记节点在半砖上
     */
    void setOnSlab();

    /**
     * @return 如果节点在探测铁轨上返回 true
     */
    boolean isStation();

    /**
     * 标记节点在探测铁轨上
     */
    void setStation();

    /**
     * @return 节点是绕路节点返回 true
     */
    boolean isCallbackNode();

    /**
     * 标记节点是绕路节点
     */
    void setCallbackNode();
}
