package com.ashbill.trainresync.compat.cauldron;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class CauldronPotions {
    public static boolean different(MilkAccess access, SoftFluidStack input) {
        if (access == null || access.trainresync$milkAmount() <= 0 || !input.is(BuiltInSoftFluids.POTION)) return false;
        var stored = ((LiquidCauldronBlockTile)access).getSoftFluidTank().getFluid();
        return stored.is(BuiltInSoftFluids.POTION) && !stored.isFluidEqual(input);
    }

    /** One bottle's native transfer, without a player or an inventory transaction. */
    public static int mix(MilkAccess access, SoftFluidStack input, FluidAction action) {
        if (!MilkAmounts.wholeContainerFits(access.trainresync$milkAmount(), 250, true)) return 0;
        var tile = (LiquidCauldronBlockTile)access;
        var bottle = input.copyWithCount(1).toItem(new ItemStack(Items.GLASS_BOTTLE));
        if (bottle == null) return 0;
        var result = tile.getSoftFluidTank().drainItem(bottle.getFirst(), tile.getLevel(), tile.getBlockPos(), action.simulate());
        if (!result.getResult().consumesAction()) return 0;
        if (action.execute()) tile.setChanged();
        return 250;
    }
}
