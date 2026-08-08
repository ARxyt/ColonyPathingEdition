package com.arxyt.colonypathingedition.core.manager;

import net.minecraftforge.fml.ModList;

/**
 * 联动管理器，单例模式，后面可能考虑替换掉easycolony.LinkageManager。
 *
 * @author sxtkl
 * @since 2025/12/12
 */
public class LinkageManager {

    private static LinkageManager instance;

    private boolean eclipticSeasonsLoaded = false;
    private boolean compatibilityAddonLoaded = false;

    /**
     * 判断是否加载了节气mod。
     *
     * @return 是否加载
     * @author sxtkl
     * @since 2025/12/12
     */
    public boolean isEclipticSeasonsLoaded() {
        return eclipticSeasonsLoaded;
    }

    /**
     * 判断是否加载了 Compatibility addon。
     *
     * @return 是否加载
     * @author sxtkl
     * @since 2025/12/12
     */
    public boolean isCompatibilityAddonLoaded() {
        return compatibilityAddonLoaded;
    }

    /**
     * 加载模组，判断所有联动内容。
     *
     * @param modList 模组列表
     * @author sxtkl
     * @since 2025/12/12
     */
    public void loadMods(ModList modList) {
        if (modList.isLoaded("eclipticseasons")) {
            this.eclipticSeasonsLoaded = true;
        }
        if (modList.isLoaded("minecolonies_compatibility")) {
            this.compatibilityAddonLoaded = true;
        }
    }

    /**
     * 获得联动管理器实例。
     *
     * @return 联动管理器单例实例
     * @author sxtkl
     * @since 2025/12/12
     */
    public static LinkageManager getInstance() {
        if (instance == null) {
            instance = new LinkageManager();
        }
        return instance;
    }
}
