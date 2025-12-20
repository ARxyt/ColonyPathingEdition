package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.colony.module.TavernRecruitModuleView;
import com.arxyt.colonypathingedition.core.message.TavernRecruitMessage;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;

public class TavernRecruitModuleWindow extends AbstractModuleWindow {

    private static final String RESOURCE_STRING = ":gui/layouthuts/layouttavernrecruit.xml";

    private final ScrollingList visitors;

    private final TavernRecruitModuleView moduleView;

    public TavernRecruitModuleWindow(final IBuildingView building, final TavernRecruitModuleView moduleView) {
        super(building, ColonyPathingEdition.MODID + RESOURCE_STRING);
        this.moduleView = moduleView;
        visitors = this.window.findPaneOfTypeByID("visitors", ScrollingList.class);
        this.updateList();
    }

    @Override
    public void onOpened() {
        super.onOpened();
        updateList();
    }

    public void updateList() {
        visitors.enable();
        visitors.show();
        visitors.setDataProvider(new ScrollingList.DataProvider() {
            @Override
            public int getElementCount() {
                return TavernRecruitModuleWindow.this.moduleView.getVisitorData().size();
            }

            @Override
            public void updateElement(int index, Pane rowPane) {
                rowPane.findPaneOfTypeByID("name", Text.class)
                    .setText(Component.literal(TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).name()));
                rowPane.findPaneOfTypeByID("athletics", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).athleticsLevel()))
                );
                rowPane.findPaneOfTypeByID("dexterity", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).dexterityLevel()))
                );
                rowPane.findPaneOfTypeByID("strength", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).strengthLevel()))
                );
                rowPane.findPaneOfTypeByID("agility", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).agilityLevel()))
                );
                rowPane.findPaneOfTypeByID("stamina", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).staminaLevel()))
                );
                rowPane.findPaneOfTypeByID("mana", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).manaLevel()))
                );
                rowPane.findPaneOfTypeByID("adaptability", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).adaptabilityLevel()))
                );
                rowPane.findPaneOfTypeByID("focus", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).focusLevel()))
                );
                rowPane.findPaneOfTypeByID("creativity", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).creativityLevel()))
                );
                rowPane.findPaneOfTypeByID("knowledge", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).knowledgeLevel()))
                );
                rowPane.findPaneOfTypeByID("intelligence", Text.class).setText(
                    Component.literal(String.valueOf(
                        TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).intelligenceLevel()))
                );
                rowPane.findPaneOfTypeByID("recruitCost", ItemIcon.class).setItem(
                    TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).recruitCost());
                rowPane.findPaneOfTypeByID("recruitBtn", Button.class).setHandler(button -> {
                    int visitorId = TavernRecruitModuleWindow.this.moduleView.getVisitorData().get(index).id();
                    Network.getNetwork().sendToServer(new TavernRecruitMessage(TavernRecruitModuleWindow.this.buildingView, visitorId));
                    updateList();
                });
            }
        });
    }
}
