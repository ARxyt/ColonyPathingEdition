package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.ExpeditionLogExtra;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = ExpeditionLog.class, remap = false)
public class ExpeditionLogMixin implements ExpeditionLogExtra {
    @Shadow(remap = false) private Map<ItemStorage, ItemStorage> loot = new HashMap<>();

    @Unique
    public boolean removeLoot(@NotNull final ItemStack stack)
    {
        ItemStorage storage = new ItemStorage(stack);
        ItemStorage inStorage = this.loot.get(storage);
        if(inStorage == null || inStorage.getAmount() < storage.getAmount()) {
            return false;
        }
        inStorage.setAmount(inStorage.getAmount() - storage.getAmount());
        if (inStorage.isEmpty()) {
            this.loot.remove(storage);
        }
        return true;
    }
}
