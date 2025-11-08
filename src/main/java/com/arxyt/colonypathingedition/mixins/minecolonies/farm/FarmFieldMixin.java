package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.util.Utils;
import com.minecolonies.core.colony.buildingextensions.AbstractBuildingExtension;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(FarmField.class)
public abstract class FarmFieldMixin extends AbstractBuildingExtension implements FarmFieldExtra {
    @Shadow(remap = false) private ItemStack seed;
    @Unique private int date = -1;
    @Unique private int seasonCount = 1;
    @Unique private int currentSeason = 1;
    @Unique private int currentDay = 0;
    @Unique private final Map<Integer, Integer> seasonDurations = new HashMap<>();
    @Unique private final Map<Integer, ItemStack> seasonalSeeds = new HashMap<>();

    public FarmFieldMixin(final BuildingExtensionRegistries.BuildingExtensionEntry fieldType, final BlockPos position)
    {
        super(fieldType, position);
    }

    public void setSeasonCount(int num) {
        this.seasonCount = Math.max(1, Math.min(4, num)); // 限制在 1–4 之间
        // 如果当前季节超过季节总数，自动回退
        if (currentSeason > this.seasonCount) {
            currentSeason = this.seasonCount;
        }
    }

    public boolean isSeasonalSeedsEmpty(){
        return seasonalSeeds.isEmpty();
    }

    public int getSeasonCount() {
        return this.seasonCount;
    }

    public void setCurrentSeason(int season) {
        this.currentSeason = Math.max(1, Math.min(this.seasonCount, season));
    }

    public int getCurrentSeason() {
        return this.currentSeason;
    }

    public void setCurrentDay(int day) {
        this.currentDay = Math.max(1, Math.min(getSeasonDuration(currentSeason), day));
    }

    public int getDate(){
        return date;
    }

    public int getCurrentDay() {
        return this.currentDay;
    }

    public void setSeasonDuration(int season, int days) {
        seasonDurations.put(season, Math.max(1, days));
    }

    public int getSeasonDuration(int season) {
        return seasonDurations.getOrDefault(season, 0);
    }

    public void setSeasonSeed(int season, @NotNull ItemStack stack) {
        seasonalSeeds.put(season, stack.copy());
    }

    public @NotNull ItemStack getSeasonSeed(int season) {
        return seasonalSeeds.getOrDefault(season, ItemStack.EMPTY);
    }

    public void updateAdvanceDay(int date, int day, int season){
        this.date = date;
        setCurrentDay(day);
        setCurrentSeason(season);
    }

    public void advanceDay(final int worldDay)
    {
        if(date < 0){
            date = worldDay;
            return;
        }
        int day = worldDay - date;
        date = worldDay;
        if( day <= 0 ){
            return;
        }
        // 增加当前天数
        currentDay += day;
        int seasonLength = seasonDurations.getOrDefault(currentSeason, 0);
        while (currentDay > seasonLength)
        {
            currentDay -= seasonLength > 0 ? seasonLength : 1; // 重置天数
            currentSeason++; // 下一季
            if (currentSeason > seasonCount)
            {
                currentSeason = 1; // 循环回第一季
            }
            seasonLength = seasonDurations.getOrDefault(currentSeason, 0);
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @Inject(method = "getSeed",at = @At("HEAD"),remap = false,cancellable = true)
    public void getSeed(CallbackInfoReturnable<ItemStack> cir)
    {
        if(seasonalSeeds.isEmpty() && !seed.isEmpty()){
            seasonalSeeds.put(1,seed);
        }
        seed = seasonalSeeds.getOrDefault(currentSeason, ItemStack.EMPTY).copy();
        seed.setCount(1);
        cir.setReturnValue(seed);
    }

    @Inject(method = "serializeNBT", at = @At("TAIL"),remap = false, cancellable = true)
    public void additionalSerializeNBT(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("SeasonCount", seasonCount);
        tag.putInt("CurrentSeason", currentSeason);
        tag.putInt("CurrentDay", currentDay);

        // 季节天数
        CompoundTag durationTag = new CompoundTag();
        for (Map.Entry<Integer, Integer> e : seasonDurations.entrySet()) {
            durationTag.putInt(String.valueOf(e.getKey()), e.getValue());
        }
        tag.put("SeasonDurations", durationTag);

        // 种子
        CompoundTag seedTag = new CompoundTag();
        for (Map.Entry<Integer, ItemStack> e : seasonalSeeds.entrySet()) {
            seedTag.put(String.valueOf(e.getKey()), e.getValue().saveOptional(provider));
        }
        tag.put("SeasonalSeeds", seedTag);
        tag.putInt("Date", date);
        cir.setReturnValue(tag);
    }

    @Unique
    public void loadSeasonData(HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        if (tag.contains("SeasonCount")) {
            seasonCount = Math.max(1, Math.min(4, tag.getInt("SeasonCount")));
        }
        if (tag.contains("CurrentSeason")) {
            currentSeason = Math.max(1, Math.min(seasonCount, tag.getInt("CurrentSeason")));
        }
        if (tag.contains("CurrentDay")) {
            currentDay = Math.max(1, tag.getInt("CurrentDay"));
        }

        seasonDurations.clear();
        if (tag.contains("SeasonDurations")) {
            CompoundTag durationTag = tag.getCompound("SeasonDurations");
            for (String key : durationTag.getAllKeys()) {
                seasonDurations.put(Integer.parseInt(key), durationTag.getInt(key));
            }
        }

        seasonalSeeds.clear();
        if (tag.contains("SeasonalSeeds")) {
            CompoundTag seedTag = tag.getCompound("SeasonalSeeds");
            for (String key : seedTag.getAllKeys()) {
                seasonalSeeds.put(Integer.parseInt(key), ItemStack.parseOptional(provider, seedTag.getCompound(key)));
            }
        }
        if (tag.contains("Date")) {
            date = tag.getInt("Date");
        }

        // 自动修正天数范围
        currentDay = Math.max(1, Math.min(getSeasonDuration(currentSeason), currentDay));
    }

    @Inject(method = "deserializeNBT", at = @At("TAIL"),remap = false)
    public void additionalDeserializeNBT(HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        loadSeasonData(provider,compound);
    }

    @Inject(method = "serialize", at = @At("TAIL"),remap = false)
    public void additionalSerialize(RegistryFriendlyByteBuf buf, CallbackInfo ci)
    {
        // 季节总数
        buf.writeInt(seasonCount);

        // 当前季节和当前天数
        buf.writeInt(currentSeason);
        buf.writeInt(currentDay);

        // 写入每个季节的长度
        buf.writeVarInt(seasonDurations.size());
        for (Map.Entry<Integer, Integer> entry : seasonDurations.entrySet())
        {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }

        // 写入每个季节的种子
        buf.writeVarInt(seasonalSeeds.size());
        for (Map.Entry<Integer, ItemStack> entry : seasonalSeeds.entrySet())
        {
            buf.writeInt(entry.getKey());
            Utils.serializeCodecMess(buf, entry.getValue());
        }

        buf.writeInt(date);
    }

    @Inject(method = "deserialize", at = @At("TAIL"),remap = false)
    public void additionalDeserialize(RegistryFriendlyByteBuf buf, CallbackInfo ci)
    {
        // 季节总数
        seasonCount = buf.readInt();

        // 当前季节和当前天数
        currentSeason = buf.readInt();
        currentDay = buf.readInt();

        // 读取每个季节的长度
        int size = buf.readVarInt();
        seasonDurations.clear();
        for (int i = 0; i < size; i++)
        {
            int key = buf.readInt();
            int value = buf.readInt();
            seasonDurations.put(key, value);
        }

        // 读取每个季节的种子
        size = buf.readVarInt();
        seasonalSeeds.clear();
        for (int i = 0; i < size; i++)
        {
            int key = buf.readInt();
            ItemStack stack = Utils.deserializeCodecMess(buf);
            seasonalSeeds.put(key, stack);
        }

        date = buf.readInt();
    }
}
