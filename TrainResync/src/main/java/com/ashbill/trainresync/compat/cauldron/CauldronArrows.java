package com.ashbill.trainresync.compat.cauldron;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.mehvahdjukaar.amendments.common.recipe.FluidAndItemCraftResult;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;

/** Shared hand/drop recipe: one ordinary arrow per 250 mB, full-duration effects. */
public final class CauldronArrows {
    public record Craft(FluidAndItemCraftResult result, int remaining) {}

    public static boolean matches(SoftFluidStack fluid, List<ItemStack> items) {
        return (fluid.is(BuiltInSoftFluids.POTION) || fluid.is(BuiltInSoftFluids.WATER))
                && items.stream().anyMatch(item -> item.is(Items.ARROW));
    }

    public static Craft craft(Level world, BlockPos pos, List<ItemStack> items, boolean batch) {
        if (!(world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile)) return null;
        var fluid = tile.getSoftFluidTank().getFluid();
        boolean potion = fluid.is(BuiltInSoftFluids.POTION);
        if (!potion && !(fluid.is(BuiltInSoftFluids.WATER) && tile.isGlowing())) return null;
        var tank = MilkIntegration.tank(world, pos);
        int amount = tank == null ? fluid.getCount() * 250 : tank.trainresync$milkAmount();
        var input = items.stream().filter(item -> item.is(Items.ARROW)).findFirst().orElseThrow();
        int count = Math.min(amount / 250, batch ? input.getCount() : 1);
        if (count == 0) return null;
        var output = potion ? tippedArrow(PotionUtils.getAllEffects(fluid.getTag()), tile.isGlowing(), count)
                : new ItemStack(Items.SPECTRAL_ARROW, count);
        input.shrink(count);
        int remaining = amount - count * 250;
        return new Craft(FluidAndItemCraftResult.of(output, fluid.copyWithCount((remaining + 249) / 250)), remaining);
    }

    public static ItemStack tippedArrow(List<MobEffectInstance> effects, boolean glowing, int count) {
        // Custom effects retain full duration on hit.
        var arrow = new ItemStack(Items.TIPPED_ARROW, count);
        if (!glowing) return PotionUtils.setCustomEffects(arrow, effects);
        var copy = new ArrayList<MobEffectInstance>(effects.size() + 1);
        for (var effect : effects) copy.add(new MobEffectInstance(effect));
        var glow = new MobEffectInstance(MobEffects.GLOWING, 200);
        copy.stream().filter(e -> e.getEffect() == MobEffects.GLOWING).findFirst()
                .ifPresentOrElse(existing -> existing.update(glow), () -> copy.add(glow));
        return PotionUtils.setCustomEffects(arrow, copy);
    }

    public static void setAmount(Level world, BlockPos pos, int amount) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null) tank.trainresync$setMilkAmount(amount);
        else if (world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile) {
            var fluid = tile.getSoftFluidTank().getFluid();
            tile.getSoftFluidTank().setFluid(fluid.copyWithCount(amount / 250));
            tile.setChanged();
        }
    }
}
