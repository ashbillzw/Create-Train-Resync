package com.ashbill.trainresync.mixin.cauldron.jei;

import com.ashbill.trainresync.compat.jei.CauldronDisplays;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.xkmc.fruitsdelight.compat.jei.CauldronRecipeCategory", remap = false)
public abstract class FruitCauldronCategoryMixin {
    @Redirect(method = "init(Lmezz/jei/api/helpers/IGuiHelper;)Ldev/xkmc/fruitsdelight/compat/jei/CauldronRecipeCategory;", at = @At(value = "INVOKE", target = "Lmezz/jei/api/helpers/IGuiHelper;createDrawable(Lnet/minecraft/resources/ResourceLocation;IIII)Lmezz/jei/api/gui/drawable/IDrawableStatic;"))
    private IDrawableStatic trainresync$rowBackground(IGuiHelper gui, ResourceLocation tex, int u, int v, int w, int h) {
        return CauldronDisplays.background(gui, tex, u, v, w, h);
    }

    @ModifyExpressionValue(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ldev/xkmc/fruitsdelight/content/cauldrons/CauldronRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;addSlot(Lmezz/jei/api/recipe/RecipeIngredientRole;II)Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;", ordinal = 0))
    private IRecipeSlotBuilder trainresync$nativeBackground(IRecipeSlotBuilder first) {
        return CauldronDisplays.nativeBackground(first);
    }

    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ldev/xkmc/fruitsdelight/content/cauldrons/CauldronRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void trainresync$displayStacks(IRecipeLayoutBuilder builder, CauldronRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        if (CauldronDisplays.layout(recipe, builder)) ci.cancel();
    }
}
