package com.arxyt.colonypathingedition.mixins.minecolonies.linkage;

import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.minecolonies.core.client.gui.modules.ItemListModuleWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemListModuleWindow.class, remap = false)
public abstract class ItemListModuleWindowMixin {

    @Redirect(method = "lambda$updateResources$3", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean updateResources$filterPredicate(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }

}
