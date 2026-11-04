package com.ashbill.trainresync.compat.cauldron;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.minecraft.core.BlockSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared by the filled-container and empty-container dispenser paths. */
public final class CauldronContainers {
    static int containerAmount(String id, String input) {
        if (id.equals("minecraft:bucket") || (!input.isEmpty() && id.endsWith("_bucket"))) return 1000;
        if (id.equals("minecraft:glass_bottle") || !input.isEmpty()) return 250;
        return 0;
    }

    static boolean canTransfer(String input, String storedType, int stored, int amount) {
        if (!input.isEmpty())
            return (stored == 0 || input.equals(storedType)) && MilkAmounts.wholeContainerFits(stored, amount, true);
        return stored >= amount && !storedType.isEmpty()
                && (amount == 1000 ? CauldronContents.liquid(storedType) : !storedType.equals("lava"));
    }

    public static InteractionResultHolder<ItemStack> dispense(BlockSource source, ItemStack held) {
        var world = source.getLevel();
        var pos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
        var tank = MilkIntegration.tank(world, pos);
        boolean empty = MilkIntegration.plain(world.getBlockState(pos));
        if (!empty && tank == null) return null;
        var unchanged = InteractionResultHolder.success(held);
        if (!empty && MappedFluids.managed(tank.trainresync$content())) {
            if (!MappedFluids.containerFits(tank, held)) return unchanged;
            var tile = (LiquidCauldronBlockTile)tank;
            var result = tile.getSoftFluidTank().interactWithItem(held, world, pos, false);
            if (result == null) return unchanged;
            tile.setChanged();
            return InteractionResultHolder.success(result);
        }
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem()));
        String input = MilkIntegration.inputType(id, held);
        int amount = containerAmount(id, input);
        if (amount == 0) return null;
        String type = empty ? "" : tank.trainresync$content();
        int stored = empty ? 0 : tank.trainresync$milkAmount();
        if (!canTransfer(input, type, stored, amount)) return unchanged;
        boolean bucket = amount == 1000;
        if (!input.isEmpty()) {
            if (MilkIntegration.fillContentAt(world, pos, input, amount, FluidAction.EXECUTE) != amount) return unchanged;
            return InteractionResultHolder.success(MilkIntegration.item(bucket ? "minecraft:bucket" : "minecraft:glass_bottle"));
        }
        tank.trainresync$setMilkAmount(stored - amount);
        return InteractionResultHolder.success(MilkIntegration.output(type, bucket));
    }
}
