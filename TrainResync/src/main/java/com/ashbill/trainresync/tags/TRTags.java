package com.ashbill.trainresync.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TRTags {
    public static final TagKey<Block> FAKE_SEATS =
        TagKey.create(Registries.BLOCK, new ResourceLocation("trainresync", "fake_seats"));
}
