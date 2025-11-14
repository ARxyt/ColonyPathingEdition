package com.arxyt.colonypathingedition.mixins.minecolonies.herder;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingRabbitHutch;
import com.minecolonies.core.colony.jobs.JobRabbitHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkRabbitHerder;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityAIWorkRabbitHerder.class, remap = false)
public abstract class EntityAIWorkRabbitHerderMixin extends AbstractEntityAIHerder<JobRabbitHerder, BuildingRabbitHutch> {

    public EntityAIWorkRabbitHerderMixin(@NotNull JobRabbitHerder job) {
        super(job);
        throw new RuntimeException("EntityAIWorkChickenHerderMixin 类不应被实例化！");
    }

    @Inject(method = "butcherAnimal", at = @At("HEAD"), remap = false, cancellable = true)
    private void notSpecialButcherAnimal(Animal animal, CallbackInfo ci){
        super.butcherAnimal(animal);
        ci.cancel();
    }
}
