package com.ashbill.trainresync.compat.cauldron;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.minecraft.core.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

/** Transfer contents once; each caller handles its inventory or item entity. */
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

    static void sound(Level world, BlockPos pos, boolean bucket, boolean filling, String type) {
        String id = (bucket ? "item.bucket." : "item.bottle.") + (filling ? "empty" : "fill");
        if (bucket && type.equals("lava")) id += "_lava";
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", id));
        if (sound != null) world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static ItemStack transfer(Level world, BlockPos pos, ItemStack held, Entity actor) {
        var tank = MilkIntegration.tank(world, pos);
        boolean empty = MilkIntegration.plain(world.getBlockState(pos));
        if (!empty && tank == null) return null;
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem()));
        String input = MilkIntegration.inputType(id, held);
        int amount = containerAmount(id, input);
        if (amount == 0) return null;
        String type = empty ? "" : tank.trainresync$content();
        int stored = empty ? 0 : tank.trainresync$milkAmount();
        if (!canTransfer(input, type, stored, amount)) return null;
        boolean filling = !input.isEmpty(), bucket = amount == 1000;
        if (filling) {
            if (MilkIntegration.fillContentAt(world, pos, input, amount, FluidAction.EXECUTE) != amount) return null;
        } else tank.trainresync$setMilkAmount(stored - amount);
        sound(world, pos, bucket, filling, filling ? input : type);
        world.gameEvent(actor, filling ? GameEvent.BLOCK_CHANGE : GameEvent.FLUID_PICKUP, pos);
        return filling ? MilkIntegration.item(bucket ? "minecraft:bucket" : "minecraft:glass_bottle")
                : MilkIntegration.output(type, bucket);
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
        var result = transfer(world, pos, held, null);
        return result == null ? unchanged : InteractionResultHolder.success(result);
    }
}
