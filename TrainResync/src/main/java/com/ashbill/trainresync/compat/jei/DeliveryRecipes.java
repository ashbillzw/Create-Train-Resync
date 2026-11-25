package com.ashbill.trainresync.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class DeliveryRecipes implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("trainresync", "delivery_recipes"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    public static void show(ItemStack dish) {
        if (runtime != null) {
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, dish);
            runtime.getRecipesGui().show(focus);
        }
    }
}
