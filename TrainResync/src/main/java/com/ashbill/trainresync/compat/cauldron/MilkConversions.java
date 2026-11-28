package com.ashbill.trainresync.compat.cauldron;

import net.hibiscus.naturespirit.blocks.MilkCauldronBlock;
import net.hibiscus.naturespirit.registration.NSBlocks;
import net.hibiscus.naturespirit.registration.NSTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared conversion and remainder rules for hand and dropped ingredients. */
public final class MilkConversions {
    static String productFor(String content, ItemStack ingredient) {
        if (!content.equals("milk")) return "";
        if (ingredient.is(NSTags.Items.CHEESE_MAKER)) return "cheese";
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(ingredient.getItem()));
        for (String flavor : CauldronContents.FLAVORS) {
            if (id.equals(CauldronContents.iceCreamId(flavor))) return "shake:" + flavor;
        }
        return "";
    }

    static ItemStack apply(Level world, BlockPos pos, MilkAccess tank, String product) {
        if (product.isEmpty() || tank.trainresync$milkAmount() != 1000) return null;
        ItemStack result;
        if (product.equals("cheese")) {
            if (!world.setBlock(pos, NSBlocks.MILK_CAULDRON.get().defaultBlockState()
                    .setValue(MilkCauldronBlock.AGE_INTO_CHEESE, true), 3)) return null;
            result = ItemStack.EMPTY;
        } else {
            int amount = CauldronContents.MILKSHAKE_SERVINGS * 250;
            tank.trainresync$setContent(product, amount, amount);
            result = MilkIntegration.item("minecraft:bowl");
        }
        CauldronContainers.sound(world, pos, false, true, "milk");
        return result;
    }
}
