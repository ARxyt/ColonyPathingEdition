package com.arxyt.colonypathingedition.core.util;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;

public class ToolUtils {
    public static double applyMending(@NotNull final AbstractEntityCitizen citizen, final double xp) {
        if (xp <= 0) {
            return 0;
        }
        Stack<ItemStack> toMend = new Stack<>();

        double localXp = xp;
        for (final EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            final ItemStack tool;
            switch (equipmentSlot) {
                case FEET :
                case HEAD :
                case LEGS :
                case CHEST: {
                    tool = citizen.getInventoryCitizen().getArmorInSlot(equipmentSlot);
                    break;
                }
                case MAINHAND: {
                    tool = citizen.getItemInHand(InteractionHand.MAIN_HAND);
                    break;
                }
                case OFFHAND: {
                    tool = citizen.getItemInHand(InteractionHand.OFF_HAND);
                    break;
                }
                default:
                    continue;
            }
            if (!ItemStackUtils.isEmpty(tool) && tool.isDamaged() && tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream().anyMatch(holder -> holder.is(Enchantments.MENDING))) {
                toMend.add(tool);
            }
        }
        if(toMend.isEmpty()) {
            return localXp;
        }

        double averageXp = localXp / toMend.size();

        // average mending
        for (final ItemStack tool : toMend) {
            final double dmgHealed = Math.min(averageXp * 2, tool.getDamageValue());
            localXp -= dmgHealed / 2;
            tool.setDamageValue(tool.getDamageValue() - (int) Math.ceil(dmgHealed));
            if (localXp <= 0) {
                return 0;
            }
        }

        // remain mending
        for (final ItemStack tool : toMend) {
            final double dmgHealed = Math.min(localXp * 2, tool.getDamageValue());
            localXp -= dmgHealed / 2;
            tool.setDamageValue(tool.getDamageValue() - (int) Math.ceil(dmgHealed));
            if (localXp <= 0) {
                return 0;
            }
        }

        return localXp;
    }
}
