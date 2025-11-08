package com.arxyt.colonypathingedition.mixins.structurize;

import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.ldtteam.structurize.client.gui.WindowReplaceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WindowReplaceBlock.class)
public abstract class WindowReplaceBlockMixin {

    @Redirect(method = "lambda$onUpdate$5", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean onUpdate$contains(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }

}
