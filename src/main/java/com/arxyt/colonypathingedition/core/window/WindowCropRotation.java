package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.manager.LinkageManager;
import com.arxyt.colonypathingedition.core.message.*;
import com.arxyt.colonypathingedition.core.data.tag.ModTag;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.tileentities.AbstractTileEntityScarecrow;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.WindowSelectRes;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.items.ItemCrop;
import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class WindowCropRotation extends AbstractWindowSkeleton {
    /**
     * Link to the xml file of the window.
     */
    private static final String WINDOW_RESOURCE = ":gui/windowcroprotation.xml";

    private static final String ROTATION_CLOSE_ID = "rotation-close";
    private static final String SYNC_ID = "sync";

    private static final String SET_SEASONS_COUNT_ID = "season-count";
    private static final String NOW_SEASONS_LABEL_ID = "current-season";
    private static final String NOW_DAYS_LABEL_ID = "current-day";

    private static final String ADD_SEEDS_BUTTON_ID_PREFIX = "add-seeds-";
    private static final String SET_DAYS_LABEL_ID_PREFIX = "set-days-";
    private static final String CURRENT_SEED_TEXT_ID_PREFIX = "current-seed-";

    /**
     * The tile entity of the scarecrow.
     */
    @NotNull
    private final AbstractTileEntityScarecrow tileEntityScarecrow;

    /**
     * The farm field instance.
     */
    private final @NotNull FarmField farmField;

    // 储存每个轮作阶段的种子
    private final Map<Integer, ItemStack> rotationSeeds = new HashMap<>();
    // 储存每个轮作阶段的长度
    private final Map<Integer, Integer> seasonLength = new HashMap<>();
    // 储存当前轮作阶段长度
    private int currentSeason = 1;
    private int currentDay = 1;
    private int season = 1;

    /**
     * Create the field GUI.
     *
     * @param tileEntityScarecrow the scarecrow tile entity.
     */
    public WindowCropRotation(@NotNull AbstractTileEntityScarecrow tileEntityScarecrow, @NotNull FarmField farmField, @Nullable final BOWindow parent) {
        super(parent, new ResourceLocation(ColonyPathingEdition.MODID + WINDOW_RESOURCE));
        this.tileEntityScarecrow = tileEntityScarecrow;
        this.farmField = farmField;
        accessFieldData();
        registerButton(ROTATION_CLOSE_ID, button -> this.close());
        registerButton(SYNC_ID, this::syncEclipticSeasons);

        for(int i = 1; i <= 4; i++) {
            String str = String.valueOf(i);
            final int index = i;
            registerButton(ADD_SEEDS_BUTTON_ID_PREFIX + str, button -> addSeed(index));
        }
        // 监听季节数量输入变化
        findPaneOfTypeByID(SET_SEASONS_COUNT_ID, TextField.class).setHandler(this::setSeasonCount);

        // 监听当前季节输入变化
        findPaneOfTypeByID(NOW_SEASONS_LABEL_ID, TextField.class).setHandler(this::setCurrentSeason);

        // 监听当前天数输入变化
        findPaneOfTypeByID(NOW_DAYS_LABEL_ID, TextField.class).setHandler(this::setCurrentDay);

        // 根据是否有节气模组，决定同步按钮状态
        findPaneOfTypeByID(SYNC_ID, Button.class).setEnabled(LinkageManager.getInstance().isEclipticSeasonsLoaded());

        // 初始化状态
        updateAll();
    }

    /**
     * 同步节气mod。
     *
     * @author sxtkl
     * @since 2025/12/15
     */
    private void syncEclipticSeasons(Button button) {
        // 设置季节为4个
        season = 4;
        SolarTerm nowTerm = EclipticUtil.INSTANCE.getSolarTerm(this.mc.level);
        // 获取每个节气的持续时间
        int lastingTime = EclipticUtil.INSTANCE.getLastingDaysOfEachTerm(this.mc.level);
        // 计算每个季节的持续时间
        int seasonLastingTime = lastingTime * 6;
        // 获取当前季节的枚举顺序
        int nowSeasonIndex = nowTerm.getSeason().ordinal();
        // 获取当前季节的日期
        int timeInTerm = EclipticUtil.INSTANCE.getTimeInTerm(this.mc.level);
        int nowDate = (timeInTerm + lastingTime * nowTerm.ordinal()) % seasonLastingTime + 1;
        currentSeason = nowSeasonIndex + 1;
        currentDay = nowDate;
        seasonLength.forEach((s, integer) -> seasonLength.put(s, seasonLastingTime));
        IColonyView colonyView = getCurrentColony();
        if (colonyView != null) {
            FarmFieldExtra extra = (FarmFieldExtra) farmField;
            extra.setCurrentSeason(currentSeason);
            Network.getNetwork().sendToServer(new CropRotationCurrentSeasonMessage(colonyView, farmField.getPosition(), currentSeason));
            extra.setSeasonCount(season);
            Network.getNetwork().sendToServer(new CropRotationSeasonCountMessage(colonyView, farmField.getPosition(), season));
            extra.setCurrentDay(currentDay);
            Network.getNetwork().sendToServer(new CropRotationCurrentDayMessage(colonyView, farmField.getPosition(), currentDay));
            seasonLength.forEach((season, duration) -> {
                extra.setSeasonDuration(season, duration);
                Network.getNetwork().sendToServer(new CropRotationLengthUpdateMessage(colonyView, farmField.getPosition(), season, duration));
            });
        }
        updateAll();
    }

    private void setSeasonCount(TextField input){
        final String value = input.getText().trim();
        try {
            int count = Integer.parseInt(value);
            if (count < 1) count = 1;
            if (count > 10) count = count % 10;
            if (count > 4) count = 4;
            season = count;
        }
        catch (NumberFormatException e) {
            season = 1;
        }
        IColonyView colonyView = getCurrentColony();
        if (colonyView != null)
        {
            ((FarmFieldExtra)farmField).setSeasonCount(season);
            Network.getNetwork().sendToServer(new CropRotationSeasonCountMessage(colonyView, farmField.getPosition(), season));
        }
    }

    private void setCurrentSeason(TextField input){
        final String value = input.getText().trim();
        try {
            int count = Integer.parseInt(value);
            if (count < 1) count = 1;
            if (count > 10) count = count % 10;
            if (count > season) count = season;
            currentSeason = count;
        } catch (NumberFormatException e) {
            currentSeason = 1;
        }
        IColonyView colonyView = getCurrentColony();
        if (colonyView != null)
        {
            ((FarmFieldExtra)farmField).setCurrentSeason(currentSeason);
            Network.getNetwork().sendToServer(new CropRotationCurrentSeasonMessage(colonyView, farmField.getPosition(), currentSeason));
        }
    }

    private void setCurrentDay(TextField input){
        final String value = input.getText().trim();
        try {
            int count = Integer.parseInt(value);
            if (seasonLength.get(currentSeason) == null || count < 1) count = 1;
            else if (count > seasonLength.get(currentSeason)) count = seasonLength.get(currentSeason);
            currentDay = count;
        } catch (NumberFormatException e) {
            // FIXME)) 这里的逻辑似乎有些问题，需要理清一个月到底是不是从0开始算
            currentDay = 0;
        }
        IColonyView colonyView = getCurrentColony();
        if (colonyView != null)
        {
            ((FarmFieldExtra)farmField).setCurrentDay(currentDay);
            Network.getNetwork().sendToServer(new CropRotationCurrentDayMessage(colonyView, farmField.getPosition(), currentDay));
        }
    }

    private void setSeasonLength(final int thisSeason, TextField input){
        final String value = input.getText().trim();
        try {
            int count = Integer.parseInt(value);
            if (count < 1) count = 1;
            if (count > 9999) count = count % 10000;
            seasonLength.put(thisSeason, count);

        } catch (NumberFormatException e) {
            seasonLength.put(thisSeason, 0);
        }
        IColonyView colonyView = getCurrentColony();
        if (colonyView != null)
        {
            final int length = seasonLength.getOrDefault(thisSeason,0);
            ((FarmFieldExtra)farmField).setSeasonDuration(thisSeason, length);
            Network.getNetwork().sendToServer(new CropRotationLengthUpdateMessage(colonyView, farmField.getPosition(), thisSeason, length));
        }
    }

    /**
     * 点击“添加种子”时打开选择界面
     */
    private void addSeed(final int slot)
    {
        new WindowSelectRes(
                this,
                stack -> stack.is(Tags.Items.SEEDS)
                        || stack.is(ModTag.ADDITIONAL_SEEDS)
                        || SpecialSeedManager.isSpecialSeed(stack.getItem())
                        || (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock)
                        || (stack.getItem() instanceof ItemCrop itemCrop && itemCrop.canBePlantedIn(Minecraft.getInstance().level.getBiome(tileEntityScarecrow.getBlockPos()))),
                (stack, qty) -> setSeed(slot,stack),
                false).open();
    }

    /**
     * 设置选中的种子
     */
    private void setSeed(int slot, ItemStack stack)
    {
        rotationSeeds.put(slot, stack);
        updateSeedDisplay(slot);

        // 更新 GUI 图标
        ItemIcon icon = findPaneOfTypeByID(CURRENT_SEED_TEXT_ID_PREFIX + slot, ItemIcon.class);
        if (icon != null) {
            icon.setItem(stack);
        }

        IColonyView colonyView = getCurrentColony();
        if (colonyView != null) {
            ((FarmFieldExtra) farmField).setSeasonSeed(slot, stack);
            Network.getNetwork().sendToServer(new CropRotationSeedUpdateMessage(colonyView, farmField.getPosition(), slot, stack));
        }
    }

    private void accessFieldData()
    {
        FarmFieldExtra farmField = ((FarmFieldExtra)this.farmField);

        // 从 FarmField 取出所有数据
        this.season = farmField.getSeasonCount();
        this.currentSeason = farmField.getCurrentSeason();
        this.currentDay = farmField.getCurrentDay();

        // 同步每个季节的内容
        for (int i = 1; i <= 4; i++)
        {
            this.rotationSeeds.put(i, farmField.getSeasonSeed(i));
            this.seasonLength.put(i, farmField.getSeasonDuration(i));
        }
    }

    private void updateUniversal()
    {
        findPaneOfTypeByID(SET_SEASONS_COUNT_ID, TextField.class).setText(String.valueOf(season));
        findPaneOfTypeByID(NOW_SEASONS_LABEL_ID, TextField.class).setText(String.valueOf(currentSeason));
        findPaneOfTypeByID(NOW_DAYS_LABEL_ID, TextField.class).setText(String.valueOf(currentDay));
    }

    /**
     * 更新界面显示
     */
    private void updateAll()
    {
        updateSeasonEnabled();
        selectSeason();
        updateUniversal();
    }

    private void updateSeasonEnabled()
    {
        for (int i = 1; i <= 4; i++)
        {
            final int thisSeason = i;
            boolean enabled = i <= season;
            updateSeedDisplay(i);
            findPaneOfTypeByID(ADD_SEEDS_BUTTON_ID_PREFIX + i, Button.class).setEnabled(enabled);
            TextField days = findPaneOfTypeByID(SET_DAYS_LABEL_ID_PREFIX + i, TextField.class);
            days.setEnabled(enabled);
            days.setText(String.valueOf(seasonLength.getOrDefault(thisSeason,0)));
            if(enabled){
                days.setHandler(input -> setSeasonLength(thisSeason, input));
            }
        }
    }

    /**
     * 显示对应轮次的种子图标
     */
    private void updateSeedDisplay(int slot)
    {
        ItemStack stack = rotationSeeds.getOrDefault(slot, ItemStack.EMPTY);
        findPaneOfTypeByID(CURRENT_SEED_TEXT_ID_PREFIX + slot, ItemIcon.class).setItem(stack);
    }

    /**
     * 高亮当前选择的季节
     */
    private void selectSeason()
    {
        for (int i = 1; i <= 4; i++)
        {
            var bg = findPaneOfTypeByID("season-bg-" + i, com.ldtteam.blockui.controls.Image.class);
            if (bg != null)
            {
                bg.setVisible(i == currentSeason);
            }
        }
    }

    @Nullable
    private IColonyView getCurrentColony()
    {
        if (tileEntityScarecrow.getCurrentColony() instanceof IColonyView colonyView)
        {
            return colonyView;
        }
        return null;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        updateAll();
    }
}
