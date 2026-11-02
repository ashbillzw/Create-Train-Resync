package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronCrafting;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mehvahdjukaar.amendments.common.recipe.CauldronRecipeUtils;
import net.mehvahdjukaar.amendments.common.recipe.FluidAndItemCraftResult;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CauldronRecipeUtils.class, remap = false)
public abstract class CauldronRecipeUtilsMixin {
    @Redirect(method = "craftMultiple", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/recipe/CauldronRecipeUtils;craft(Lnet/minecraft/world/level/Level;ZILnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Ljava/util/List;)Lnet/mehvahdjukaar/amendments/common/recipe/FluidAndItemCraftResult;"))
    private static FluidAndItemCraftResult trainresync$skipUnchanged(Level world, boolean boiling, int capacity,
            SoftFluidStack fluid, List<ItemStack> items) {
        return CauldronCrafting.craft(world, boiling, capacity, fluid, items);
    }
}
