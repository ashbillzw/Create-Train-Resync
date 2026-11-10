package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.ArrayList;
import java.util.List;

public final class CauldronGlow {
    public static final TagKey<Item> INGREDIENTS = TagKey.create(Registries.ITEM,
            new ResourceLocation("trainresync", "cauldron_glow_ingredients"));

    /** Add the cauldron's ten-second glow without changing the original effects. */
    public static List<MobEffectInstance> withGlow(List<MobEffectInstance> effects) {
        var copy = new ArrayList<MobEffectInstance>(effects.size() + 1);
        for (var effect : effects) copy.add(new MobEffectInstance(effect));
        var glow = new MobEffectInstance(MobEffects.GLOWING, 200);
        copy.stream().filter(e -> e.getEffect() == MobEffects.GLOWING).findFirst()
                .ifPresentOrElse(existing -> existing.update(glow), () -> copy.add(glow));
        return copy;
    }

    public static ItemStack bottle(ItemStack output) {
        if (!(output.getItem() instanceof PotionItem)) return output;
        var effects = withGlow(PotionUtils.getCustomEffects(output));
        // Vanilla's setter appends; replace only the custom list, retaining base potion and other tags.
        output.getOrCreateTag().remove("CustomPotionEffects");
        return PotionUtils.setCustomEffects(output, effects);
    }

    public static int mixingCount(List<MobEffectInstance> effects) {
        return (int)effects.stream().filter(e -> e.getEffect() != MobEffects.GLOWING).count();
    }
}
