package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.colony.buildings.modules.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

@Mixin(value = BuildingModules.class, remap = false)
public class BuildingModulesMixin
{
    /**
     * 可以用这个函数在注册时修改小屋的各种参数设置，这里仅用来修改上限人数。
     */
    @ModifyArg(
            method = "<clinit>",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/colony/buildings/registry/BuildingEntry$ModuleProducer;<init>" +
                            "(Ljava/lang/String;Ljava/util/function/Supplier;Ljava/util/function/Supplier;)V"
            ),
            index = 1
    )
    private static Supplier<IBuildingModule> modifyProducerArg(String key , Supplier<IBuildingModule> moduleSupplier , Supplier<?> viewSupplier)
    {
        return switch (key) {
            case "cook_craft" -> () -> new NoPrivateCrafterWorkerModule(
                    ModJobs.cook.get(),
                    Skill.Adaptability,
                    Skill.Knowledge,
                    true,
                    (b) -> PathingConfig.RESTAURANT_EXTRA_WORKER.get() ? Math.max(1, (b.getBuildingLevel() + 1) / 2) : 1
            );
            case "chef_work" -> () -> new CraftingWorkerBuildingModule(
                    ModJobs.chef.get(),
                    Skill.Creativity,
                    Skill.Knowledge,
                    true,
                    (b) -> PathingConfig.KITCHEN_EXTRA_WORKER.get() ? Math.max(1, (b.getBuildingLevel() + 1) / 2) : 1,
                    Skill.Knowledge,
                    Skill.Creativity
            );
            case "healer_work" -> () -> new HospitalAssignmentModule(
                    ModJobs.healer.get(),
                    Skill.Mana,
                    Skill.Knowledge,
                    true,
                    (b) -> PathingConfig.HOSPITAL_EXTRA_WORKER.get() ? Math.max(1, (b.getBuildingLevel() + 1) / 2) : 1
            );
            case "stonesmelter_work" -> () -> new CraftingWorkerBuildingModule(ModJobs.stoneSmeltery.get(),
                    Skill.Athletics,
                    Skill.Dexterity,
                    true,
                    (b) -> PathingConfig.STONE_SMELTERY_EXTRA_WORKER.get() ? Math.max(1, (b.getBuildingLevel() + 1) / 2) : 1,
                    Skill.Dexterity,
                    Skill.Athletics
            );
            case "courier_work" -> () -> new DeliverymanAssignmentModule(ModJobs.delivery.get(),
                    Skill.Agility,
                    Skill.Adaptability,
                    true,
                    (b) -> PathingConfig.DELIVERY_EXTRA_WORKER.get() ? Math.max(1, (b.getBuildingLevel() + 1) / 2) : 1
            );
            default ->
                // leave unchanged
                    moduleSupplier;
        };
    }
}
