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
        boolean bucket = id.endsWith("_bucket") && !input.isEmpty();
        boolean emptyBucket = id.equals("minecraft:bucket"), emptyBottle = id.equals("minecraft:glass_bottle");
        if (input.isEmpty() && !emptyBucket && !emptyBottle) return null;
        String type = empty ? "" : tank.trainresync$content();
        int amount = bucket || emptyBucket ? 1000 : 250;
        int stored = empty ? 0 : tank.trainresync$milkAmount();
        if (!input.isEmpty()) {
            if (stored > 0 && !input.equals(type)) return unchanged;
            if (!MilkAmounts.wholeContainerFits(stored, amount, true)) return unchanged;
            if (MilkIntegration.fillContentAt(world, pos, input, amount, FluidAction.EXECUTE) != amount) return unchanged;
            return InteractionResultHolder.success(MilkIntegration.item(bucket ? "minecraft:bucket" : "minecraft:glass_bottle"));
        }
        if (stored < amount || type.isEmpty() || (emptyBucket && !CauldronContents.liquid(type)) || (emptyBottle && type.equals("lava"))) return unchanged;
        tank.trainresync$setMilkAmount(stored - amount);
        return InteractionResultHolder.success(MilkIntegration.output(type, emptyBucket));
    }
}
