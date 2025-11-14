package com.arxyt.colonypathingedition.mixins.minecolonies.linkage;

import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.minecolonies.core.client.gui.WindowSelectRes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WindowSelectRes.class, remap = false)
public abstract class WindowSelectResMixin {

    @Redirect(method = "updateResources", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean updateResources$contains(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }

}
