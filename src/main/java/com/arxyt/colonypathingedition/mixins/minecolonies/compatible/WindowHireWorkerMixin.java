package com.arxyt.colonypathingedition.mixins.minecolonies.compatible;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.minecolonies.api.colony.buildings.modules.IAssignmentModuleView;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.WindowHireWorker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static com.minecolonies.core.client.gui.modules.building.WindowBuilderResModule.BLACK;

@Mixin(value = WindowHireWorker.class, remap = false)
public abstract class WindowHireWorkerMixin extends AbstractWindowSkeleton{
    //TODO:别忘了回头测试过关了把这个mixin加回去

    @Shadow(remap = false) @Final protected List<IAssignmentModuleView> moduleViews;

    @Shadow(remap = false) protected abstract void jobClicked(@NotNull Button button);

    @Shadow
    protected IAssignmentModuleView selectedModule;

    public WindowHireWorkerMixin()
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/windowhireworker.xml"));
    }

    /**
     * @author ARxyt
     * @reason A compatible test, so use a simpler way to edit.
     */
    @Overwrite(remap = false)
    public void setupJobButtons()
    {
        int xOffset = 15;
        for (final IAssignmentModuleView hireModule : moduleViews)
        {
            final JobEntry entry = hireModule.getJobEntry();

            if(entry == null){
                continue;
            }

            final ButtonImage jobButton = new ButtonImage();
            jobButton.setImage(new ResourceLocation("minecolonies:textures/gui/builderhut/builder_button_medium.png"), false);
            jobButton.setPosition(xOffset, 30);
            if (!hireModule.getAssignedCitizens().isEmpty())
            {
                jobButton.setText(Component.translatable(entry.getTranslationKey()).append(Component.literal(" " + hireModule.getAssignedCitizens().size())));
            }
            else
            {
                jobButton.setText(Component.translatable(entry.getTranslationKey()));
            }
            jobButton.setID(hireModule.getJobEntry().getKey().toString());
            jobButton.setHandler(this::jobClicked);
            jobButton.setSize(86, 17);
            jobButton.setTextSize(86, 17);

            this.addChild(jobButton);
            PaneBuilders.tooltipBuilder().hoverPane(jobButton).build().setText(Component.translatable(entry.getKey().toString() + ".job.desc"));
            if (entry.equals(selectedModule.getJobEntry()))
            {
                jobButton.disable();
            }
            else
            {
                jobButton.enable();
            }
            jobButton.setColors(BLACK);
            jobButton.setTextAlignment(Alignment.MIDDLE);

            xOffset += 90;
        }
    }
}
