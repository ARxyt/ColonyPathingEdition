package com.arxyt.colonypathingedition.mixins.minecolonies.linkage;

import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.minecolonies.core.client.gui.WindowPostBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WindowPostBox.class)
public abstract class WindowPostBoxMixin {

    @Redirect(method = "lambda$updateResources$1", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean updateResources$contain(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }
}
