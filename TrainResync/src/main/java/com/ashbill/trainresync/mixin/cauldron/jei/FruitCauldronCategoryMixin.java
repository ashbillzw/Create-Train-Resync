package com.ashbill.trainresync.mixin.cauldron.jei;

import com.ashbill.trainresync.compat.jei.CauldronDisplays;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.xkmc.fruitsdelight.compat.jei.CauldronRecipeCategory", remap = false)
public abstract class FruitCauldronCategoryMixin {
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ldev/xkmc/fruitsdelight/content/cauldrons/CauldronRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void trainresync$displayStacks(IRecipeLayoutBuilder builder, CauldronRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        if (CauldronDisplays.layout(recipe, builder)) ci.cancel();
    }
}
