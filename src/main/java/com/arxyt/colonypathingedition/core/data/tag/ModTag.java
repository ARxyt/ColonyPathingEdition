package com.arxyt.colonypathingedition.core.data.tag;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTag {
    public static final TagKey<Item> SEEDS_UNDERWATER = createTag("seeds_underwater");
    public static final TagKey<Item> SEEDS_NOFARMLAND = createTag("seeds_nofarmland");
    public static final TagKey<Item> ADDITIONAL_SEEDS = createTag("additional_seeds");

    private static TagKey<Item> createTag(String name) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(ColonyPathingEdition.MODID, name));
    }
}
