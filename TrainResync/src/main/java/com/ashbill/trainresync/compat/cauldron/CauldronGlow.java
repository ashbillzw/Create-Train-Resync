package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class CauldronGlow {
    public static final TagKey<Item> INGREDIENTS = TagKey.create(Registries.ITEM,
            new ResourceLocation("trainresync", "cauldron_glow_ingredients"));
}
