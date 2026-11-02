package com.ashbill.trainresync.compat.cauldron;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.Level;
import net.mehvahdjukaar.amendments.common.recipe.CauldronRecipeUtils;
import net.mehvahdjukaar.amendments.common.recipe.FluidAndItemCraftResult;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;

/** Preview on copies, then commit consumption only when the items actually change. */
public final class CauldronCrafting {
    public static boolean canWash(SoftFluidStack fluid, List<ItemStack> items) {
        return fluid.is(BuiltInSoftFluids.WATER) && items.size() == 1
                && washed(items.get(0)) != null;
    }

    public static ItemStack washed(ItemStack input) {
        var output = input.copyWithCount(1);
        if (input.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(input)) {
            dyeable.clearColor(output);
        } else if (input.getItem() instanceof BannerItem && BannerBlockEntity.getPatternCount(input) > 0) {
            BannerBlockEntity.removeLastPattern(output);
        } else if (ShulkerBoxBlock.getColorFromItem(input.getItem()) != null) {
            output = ShulkerBoxBlock.getColoredItemStack(null);
            if (input.getTag() != null) output.setTag(input.getTag().copy());
        } else return null;
        return output;
    }

    public static FluidAndItemCraftResult craft(Level world, boolean boiling, int capacity,
            SoftFluidStack fluid, List<ItemStack> inputs) {
        var copies = new ArrayList<ItemStack>();
        for (var input : inputs) copies.add(input.copy());
        FluidAndItemCraftResult result;
        if (!fluid.isEmpty() && canWash(fluid, copies)) {
            var output = washed(copies.get(0));
            copies.get(0).shrink(1);
            result = FluidAndItemCraftResult.of(output, fluid.copyWithCount(fluid.getCount() - 1));
        } else result = CauldronRecipeUtils.craft(world, boiling, capacity, fluid.copy(), copies);
        if (result == null) return null;
        var output = result.craftedItem();
        int consumed = 0;
        boolean same = !output.isEmpty();
        for (int i = 0; i < inputs.size(); i++) {
            int count = inputs.get(i).getCount() - copies.get(i).getCount();
            consumed += count;
            if (count > 0 && !ItemStack.isSameItemSameTags(inputs.get(i), output)) same = false;
        }
        if (same && consumed == output.getCount()) return null;
        for (int i = 0; i < inputs.size(); i++)
            inputs.get(i).shrink(inputs.get(i).getCount() - copies.get(i).getCount());
        return result;
    }
}
