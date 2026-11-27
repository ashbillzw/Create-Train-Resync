package com.ashbill.trainresync.meituan;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.RiceBowlRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashSet;
import java.util.Set;

/** Called only when the Cookery version with flex recipes is installed. */
final class CuisineQuality {
    static Set<Item> products(ServerLevel level) {
        var recipes = level.getRecipeManager();
        var access = level.registryAccess();
        Set<Item> foods = new HashSet<>();
        recipes.getAllRecipesFor(ModRecipes.FLEX_POT_RECIPE).forEach(r -> foods.add(r.getResultItem(access).getItem()));
        recipes.getAllRecipesFor(ModRecipes.FLEX_STOCKPOT_RECIPE).forEach(r -> foods.add(r.getResultItem(access).getItem()));
        var bowls = recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(RiceBowlRecipe.class::isInstance).toList();
        boolean added;
        do {
            added = false;
            for (var bowl : bowls) {
                if (bowl.getIngredients().stream().anyMatch(ingredient ->
                        foods.stream().anyMatch(food -> ingredient.test(new ItemStack(food)))))
                    added |= foods.add(bowl.getResultItem(access).getItem());
            }
        } while (added);
        return foods;
    }

    static int bonusHalves(ItemStack stack) {
        String key = QualityUtils.CUISINE_QUALITY;
        if (stack.getTag() == null || !stack.getTag().contains(key, Tag.TAG_INT)) return -1;
        Quality quality = QualityUtils.getQuality(stack);
        if (quality.getId() != stack.getTag().getInt(key)) return -1;
        ItemStack plain = stack.copy();
        plain.removeTagKey(key);
        if (!ItemStack.isSameItemSameTags(plain, new ItemStack(stack.getItem()))) return -1;
        return quality == Quality.SUPERB ? 2 : quality == Quality.EXCELLENT ? 1 : 0;
    }
}
