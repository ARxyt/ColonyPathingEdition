package com.arxyt.colonypathingedition.core.util;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class ToolUtils {
    public static int INVALID_INDEX = -2;
    public static int INVENTORY_FULL = -1;

    /**
     * Checks if the {@link IItemHandler} contains the following equipmentTypes with the given level range.
     *
     * @param itemHandler  The {@link IItemHandler} to scan.
     * @param equipmentTypes     The list equipmentType of the equipment to find.
     * @param minimalLevel The minimal level to find.
     * @param maximumLevel The maximum level to find.
     * @return Equipment with the given EquipmentType was found in the given {@link IItemHandler}.
     */
    public static Map<EquipmentTypeEntry, Integer> findEquipments(
            @NotNull final IItemHandler itemHandler,
            @NotNull final List<EquipmentTypeEntry> equipmentTypes,
            final int minimalLevel,
            final int maximumLevel)
    {
        return findEquipmentsWithExceptSlots(itemHandler, equipmentTypes, minimalLevel, maximumLevel, new HashMap<>());
    }

    /**
     * Checks if the {@link IItemHandler} contains the following equipmentTypes with the given level range and exception.
     *
     * @param itemHandler  The {@link IItemHandler} to scan.
     * @param equipmentTypes     The list equipmentType of the equipment to find.
     * @param minimalLevel The minimal level to find.
     * @param maximumLevel The maximum level to find.
     * @param exceptSlots The slots except for the finding.
     * @return Equipment with the given EquipmentType was found in the given except given slots {@link IItemHandler}.
     */
    public static Map<EquipmentTypeEntry, Integer> findEquipmentsWithExceptSlots(
            @NotNull final IItemHandler itemHandler,
            @NotNull final List<EquipmentTypeEntry> equipmentTypes,
            final int minimalLevel,
            final int maximumLevel,
            @NotNull final Map<EquipmentTypeEntry, Integer> exceptSlots)
    {
        if (equipmentTypes.isEmpty()) {
            return new HashMap<>();
        }

        @NotNull final BiFunction<ItemStack, EquipmentTypeEntry, Boolean> worthySlotPredicate =
                (ItemStack stack, EquipmentTypeEntry equipmentType) ->
                        ItemStackUtils.hasEquipmentLevel(stack, equipmentType, minimalLevel, maximumLevel);

        final List<EquipmentTypeEntry> remainTypes = new ArrayList<>(equipmentTypes);
        final Map<EquipmentTypeEntry, Integer> foundTools = new HashMap<>();

        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (remainTypes.isEmpty()) {
                break;
            }

            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // 用迭代器，边遍历边移除已匹配的类型
            final Iterator<EquipmentTypeEntry> iterator = remainTypes.iterator();
            while (iterator.hasNext())
            {
                EquipmentTypeEntry equipmentType = iterator.next();
                Integer exceptSlot = exceptSlots.get(equipmentType);
                if (exceptSlot != null && exceptSlot == slot) {
                    continue;
                }
                if (worthySlotPredicate.apply(stack, equipmentType))
                {
                    foundTools.put(equipmentType, slot);
                    iterator.remove(); // 该类型已找到，不再参与后续槽位匹配
                }
            }
        }

        return foundTools;
    }

    public static Map<EquipmentTypeEntry, Integer> checkEquipmentsInItemHandler(
            final IItemHandler sourceHandler,
            final Set<EquipmentTypeEntry> typeNeedCheck,
            final BiFunction<ItemStack, EquipmentTypeEntry, Boolean> toolPredicate)
    {
        Map<EquipmentTypeEntry, Integer> foundTools = new HashMap<>();
        if (sourceHandler == null || typeNeedCheck.isEmpty()) {
            return foundTools;
        }

        for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
            if (foundTools.size() == typeNeedCheck.size()) {
                break;
            }

            ItemStack sourceStack = sourceHandler.getStackInSlot(slot);
            if (sourceStack.isEmpty()) {
                continue;
            }

            for (EquipmentTypeEntry tool : typeNeedCheck) {
                if (!foundTools.containsKey(tool) && toolPredicate.apply(sourceStack, tool)) {
                    foundTools.put(tool, slot);
                    break;
                }
            }
        }
        return foundTools;
    }

    /**
     * Method to transfer an ItemStacks from the given source {@link IItemHandler} to the given target {@link IItemHandler}.
     *
     * @param sourceHandler The {@link IItemHandler} that works as Source.
     * @param slot          The exact slot to transfer.
     * @param predicate     the predicate for the stack.
     * @param targetHandler The {@link IItemHandler} that works as Target.
     * @return -2 for INVALID_INDEX, -1 for INVENTORY_FULL, else for exact slot.
     */
    public static int transferItemStackOfExactSlotIntoEmptySlotInNextItemHandler(
            @Nullable final IItemHandler sourceHandler,
            final int slot,
            final Predicate<ItemStack> predicate,
            @NotNull final IItemHandler targetHandler)
    {
        if(sourceHandler == null) {
            return INVALID_INDEX;
        }
        final int slotSize = sourceHandler.getSlots();
        if(slot < 0 || slot >= slotSize || !predicate.test(sourceHandler.getStackInSlot(slot))) {
            return INVALID_INDEX;
        }

        final int targetSlot = getFirstOpenSlotFromItemHandler(targetHandler);
        if(targetSlot >= 0){
            ItemStack sourceStack = sourceHandler.extractItem(slot, Integer.MAX_VALUE, false);
            targetHandler.insertItem(targetSlot, sourceStack, false);
        }
        return targetSlot;
    }

    public static int getFirstOpenSlotFromItemHandler(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            return INVENTORY_FULL;
        }

        for (int i = 0, slots = itemHandler.getSlots(); i < slots; i++)
        {
            final ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty())
            {
                return i;
            }
        }

        return INVENTORY_FULL;
    }

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
            if (isMendingDamagedTool(tool)) {
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

    public static boolean isMendingDamagedTool(ItemStack tool) {
        if (ItemStackUtils.isEmpty(tool)) return false;
        if (!tool.isDamaged()) return false;
        if (!tool.isEnchanted()) return false;
        return EnchantmentHelper.getEnchantments(tool).containsKey(Enchantments.MENDING);
    }
}
