package com.arxyt.colonypathingedition.mixins.minecolonies.researches;

import com.minecolonies.api.research.AbstractResearchProvider;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.generation.defaults.DefaultResearchProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.List;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.PRECISE_FARMING;

@Mixin(value = DefaultResearchProvider.class, remap = false)
public class DefaultResearchProviderMixin {
    @Shadow(remap = false) @Final
    private static ResourceLocation TECH;

    @Inject(method = "getTechnologyResearch", at = @At("TAIL"), remap = false)
    private void addNewResearches(Collection<AbstractResearchProvider.Research> r, CallbackInfoReturnable<Collection<AbstractResearchProvider.Research>> cir) {

        new AbstractResearchProvider.Research(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "technology/colonypathedition_precise_farming"), TECH)
                .addEffect(PRECISE_FARMING, 1)
                .addToList(r);
        new AbstractResearchProvider.Research(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "technology/colonypathedition_precise_farming2"), TECH)
                .addToList(r);
        new AbstractResearchProvider.Research(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "technology/colonypathedition_precise_farming3"), TECH)
                .addToList(r);
    }

    @Inject(method = "getResearchEffectCollection", at = @At("TAIL"), remap = false, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addNewEffects(CallbackInfoReturnable<Collection<AbstractResearchProvider.ResearchEffect>> cir, List<AbstractResearchProvider.ResearchEffect> effects) {
        effects.add(new AbstractResearchProvider.ResearchEffect(PRECISE_FARMING).setLevels(new double[] {1, 2, 4}));
    }
}
