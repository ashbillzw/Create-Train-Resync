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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.alchemy.Potions;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.util.PotionNBTHelper;
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
        // Moonlight can share the output's effect list with the fluid left in the tank.
        var tag = output.getOrCreateTag();
        tag.put("CustomPotionEffects", tag.getList("CustomPotionEffects", Tag.TAG_COMPOUND).copy());
        writeGlow(tag);
        return output;
    }

    public static int mixingCount(List<MobEffectInstance> effects) {
        return (int)effects.stream().filter(e -> e.getEffect() != MobEffects.GLOWING).count();
    }

    /** Return the input itself when its contents need no conversion. */
    public static SoftFluidStack unpack(SoftFluidStack input) {
        if (!input.is(BuiltInSoftFluids.POTION)) return input;
        var effects = PotionUtils.getAllEffects(input.getTag());
        if (effects.stream().noneMatch(e -> e.getEffect() == MobEffects.GLOWING)) return input;
        var tag = input.getTag().copy();
        var base = PotionUtils.getPotion(tag);
        var custom = tag.getList("CustomPotionEffects", Tag.TAG_COMPOUND);
        custom.removeIf(CauldronGlow::isGlow);
        if (base.getEffects().stream().anyMatch(e -> e.getEffect() == MobEffects.GLOWING)) {
            tag.remove("Potion");
            for (var effect : base.getEffects())
                if (effect.getEffect() != MobEffects.GLOWING) custom.add(effect.save(new CompoundTag()));
        }
        if (custom.isEmpty()) tag.remove("CustomPotionEffects");
        else tag.put("CustomPotionEffects", custom);
        if (PotionNBTHelper.getPotionType(tag) == PotionNBTHelper.Type.REGULAR
                && (base == Potions.WATER || PotionUtils.getPotion(tag) == Potions.EMPTY)
                && PotionUtils.getAllEffects(tag).isEmpty())
            return SoftFluidStack.of(BuiltInSoftFluids.WATER, input.getCount());
        return SoftFluidStack.of(BuiltInSoftFluids.POTION, input.getCount(), tag);
    }

    public static SoftFluidStack pack(SoftFluidStack stored) {
        if (stored.is(BuiltInSoftFluids.WATER))
            return SoftFluidStack.fromItem(bottle(MilkIntegration.output("water", false))).getFirst();
        if (!stored.is(BuiltInSoftFluids.POTION)) return stored;
        var copy = stored.copy();
        writeGlow(copy.getOrCreateTag());
        return copy;
    }

    private static boolean isGlow(Tag entry) {
        var effect = MobEffectInstance.load((CompoundTag)entry);
        return effect != null && effect.getEffect() == MobEffects.GLOWING;
    }

    private static void writeGlow(CompoundTag tag) {
        var custom = tag.getList("CustomPotionEffects", Tag.TAG_COMPOUND);
        custom.removeIf(CauldronGlow::isGlow);
        custom.add(new MobEffectInstance(MobEffects.GLOWING, 200).save(new CompoundTag()));
        tag.put("CustomPotionEffects", custom);
    }
}
