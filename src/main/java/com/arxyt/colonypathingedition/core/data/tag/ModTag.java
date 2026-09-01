package com.arxyt.colonypathingedition.core.data.tag;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTag {
    public static final TagKey<Item> SEEDS_UNDERWATER = createItemTag("seeds_underwater");
    public static final TagKey<Item> SEEDS_NOFARMLAND = createItemTag("seeds_nofarmland");
    public static final TagKey<Item> ADDITIONAL_SEEDS = createItemTag("additional_seeds");
    public static final TagKey<Item> MINER_MULTIPLY_ITEMS = createItemTag("miner_multiply");

    public static final TagKey<Block> MINER_MULTIPLY_BLOCKS = createBlockTag("miner_multiply");

    private static TagKey<Item> createItemTag(String name) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(ColonyPathingEdition.MODID, name));
    }

    private static TagKey<Block> createBlockTag(String name) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(ColonyPathingEdition.MODID, name));
    }
}
