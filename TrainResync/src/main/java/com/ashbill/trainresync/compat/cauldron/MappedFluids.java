package com.ashbill.trainresync.compat.cauldron;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.amendments.reg.ModTags;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.forge.SoftFluidStackImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Registered, lossless Forge/SoftFluid mappings; no per-mod fluid list. */
public final class MappedFluids {
    public static boolean managed(String type) { return type.startsWith("mapped:"); }
    public static SoftFluidStack fromForge(FluidStack input) {
        if (input.isEmpty()) return SoftFluidStack.empty();
        // Mapping must not round a small pipe transfer down to zero bottles.
        var soft = SoftFluidStack.fromFluid(input.getFluid(), 1, input.hasTag() ? input.getTag().copy() : null);
        if (soft.isEmpty() || soft.is(ModTags.CAULDRON_BLACKLIST)) return SoftFluidStack.empty();
        return SoftFluidStackImpl.toForgeFluid(soft).isFluidEqual(input) ? soft : SoftFluidStack.empty();
    }
    public static String type(SoftFluidStack soft) {
        if (soft.isEmpty()) return "";
        var forge = SoftFluidStackImpl.toForgeFluid(soft.copyWithCount(1));
        var roundTrip = fromForge(forge);
        return !roundTrip.isEmpty() && roundTrip.isFluidEqual(soft)
            ? "mapped:" + soft.fluidKey().location() : "";
    }
    public static FluidStack contents(MilkAccess access) {
        if (!managed(access.trainresync$content())) return CauldronContents.fluid(access.trainresync$content(), access.trainresync$milkAmount());
        var soft = ((LiquidCauldronBlockTile)access).getSoftFluidTank().getFluid();
        var result = SoftFluidStackImpl.toForgeFluid(soft.copyWithCount(1));
        result.setAmount(access.trainresync$milkAmount());
        return result;
    }
    /** Native containers still use their registered sizes, never the rounded visual amount. */
    public static boolean containerFits(MilkAccess access, ItemStack held) {
        var soft = ((LiquidCauldronBlockTile)access).getSoftFluidTank().getFluid();
        var empty = soft.getContainerList().getCategoryFromEmpty(held.getItem());
        if (empty.isPresent()) return MilkAmounts.wholeContainerFits(access.trainresync$milkAmount(), empty.get().getCapacity() * 250, false);
        var filled = SoftFluidStack.fromItem(held);
        return filled == null || MilkAmounts.wholeContainerFits(access.trainresync$milkAmount(), filled.getFirst().getCount() * 250, true);
    }
}
