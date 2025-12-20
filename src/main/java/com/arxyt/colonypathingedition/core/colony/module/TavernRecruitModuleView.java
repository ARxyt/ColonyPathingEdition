package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.core.window.TavernRecruitModuleWindow;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TavernRecruitModuleView extends AbstractBuildingModuleView implements IBuildingModuleView {
    private final List<VisitorData> visitorData = new ArrayList<>();

    @Override
    public void deserialize(@NotNull FriendlyByteBuf buf) {
        visitorData.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            VisitorData data = new VisitorData(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                buf.readItem()
            );
            visitorData.add(data);
        }
        visitorData.sort((o1, o2) -> o2.intelligenceLevel - o1.intelligenceLevel);
    }

    @Override
    public BOWindow getWindow() {
        return new TavernRecruitModuleWindow(buildingView, this);
    }

    @Override
    public String getDesc() {
        return "com.arxyt.colonypathingedition.core.tavern_recruit";
    }

    public List<VisitorData> getVisitorData() {
        return visitorData;
    }

    public record VisitorData(
        int id,
        int athleticsLevel,
        int dexterityLevel,
        int strengthLevel,
        int agilityLevel,
        int staminaLevel,
        int manaLevel,
        int adaptabilityLevel,
        int focusLevel,
        int creativityLevel,
        int knowledgeLevel,
        int intelligenceLevel,
        String name,
        ItemStack recruitCost
    ) {}
}
