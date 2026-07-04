package com.arxyt.colonypathingedition.mixins.structurize;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import com.arxyt.colonypathingedition.core.message.CropRotationSeasonCountMessage;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.client.gui.AbstractWindowSkeleton;
import com.ldtteam.structurize.client.gui.WindowSelectRes;
import com.ldtteam.structurize.client.gui.util.InputFilters;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.core.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.stringtemplate.v4.ST;

import java.util.List;
import java.util.function.BiConsumer;

import static com.ldtteam.structurize.client.gui.WindowSelectRes.COUNT;

@Mixin(value = WindowSelectRes.class, remap = false)
public abstract class WindowSelectResMixin extends AbstractWindowSkeleton {

    public WindowSelectResMixin(final ResourceLocation xml) {
        super(xml);
    }

    @Redirect(method = "updateResources", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean updateResources$contains(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }

    @Redirect(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lcom/ldtteam/blockui/views/BOWindow;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/function/BiConsumer;ZLnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Lcom/ldtteam/blockui/controls/TextField;setFilter(Lcom/ldtteam/blockui/controls/TextField$Filter;)V"), remap = false)
    public void resetFilter(TextField instance, TextField.Filter f){
        instance.setFilter(InputFilters.ONLY_NUMBERS);
        instance.setHandler(this::onTypeIn);
    }

    private void onTypeIn (TextField input){
        final String value = input.getText().trim();
        int count;
        try {
            count = Integer.parseInt(value);
            if (count > 9999) count = count % 10000;
            if (count < 0) count = 0;
        }
        catch (NumberFormatException e) {
            count = 0;
        }
        input.setText(String.valueOf(count));
    }


    @Redirect(method = "getCount", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I"), remap = false)
    private int resetCount(String s)
    {
        int count = Integer.parseInt(findPaneOfTypeByID(COUNT, TextField.class).getText());
        return count == 0 ? 1 : count;
    }

}
