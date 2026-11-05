package com.ashbill.trainresync.compat.cauldron;

import net.hibiscus.naturespirit.blocks.MilkCauldronBlock;
import net.hibiscus.naturespirit.registration.NSBlocks;
import net.hibiscus.naturespirit.registration.NSTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared conversion and remainder rules for hand and dropped ingredients. */
public final class MilkConversions {
    public static ItemStack apply(Level world, BlockPos pos, ItemStack input) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank == null || !tank.trainresync$content().equals("milk") || tank.trainresync$milkAmount() != 1000) return null;
        ItemStack result = null;
        if (input.is(NSTags.Items.CHEESE_MAKER)) {
            result = world.setBlock(pos, NSBlocks.MILK_CAULDRON.get().defaultBlockState()
                    .setValue(MilkCauldronBlock.AGE_INTO_CHEESE, true), 3) ? ItemStack.EMPTY : null;
        } else for (var flavor : CauldronContents.FLAVORS) {
            if (input.is(MilkIntegration.item(CauldronContents.iceCreamId(flavor)).getItem())) {
                int amount = CauldronContents.MILKSHAKE_SERVINGS * 250;
                tank.trainresync$setContent("shake:" + flavor, amount, amount);
                result = MilkIntegration.item("minecraft:bowl");
                break;
            }
        }
        if (result != null) CauldronContainers.sound(world, pos, false, true, "milk");
        return result;
    }
}
