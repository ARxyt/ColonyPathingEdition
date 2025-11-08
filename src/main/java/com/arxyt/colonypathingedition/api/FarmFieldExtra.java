package com.arxyt.colonypathingedition.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface FarmFieldExtra {
    /** set rotating season count（1–4） */
    void setSeasonCount(int num);

    /** get rotating season count */
    int getSeasonCount();

    /** set season */
    void setCurrentSeason(int season);

    /** get season */
    int getCurrentSeason();

    /** set day of a season */
    void setCurrentDay(int day);

    /** get day of a season */
    int getCurrentDay();

    /** set days limit of a season */
    void setSeasonDuration(int season, int days);

    /** get days limit of a season */
    int getSeasonDuration(int season);

    /** set seed of a season */
    void setSeasonSeed(int season, @NotNull ItemStack stack);

    /** get seed of a season */
    @NotNull ItemStack getSeasonSeed(int season);

    boolean isSeasonalSeedsEmpty();

    void advanceDay(final int worldDay);

    int getDate();

    void updateAdvanceDay(int date, int day, int season);
}
