package com.ashbill.trainresync.compat.cauldron;

import java.util.Map;
import java.util.Set;

public final class MilkRecipes {
    // Neapolitan 5.1.0 has these two file names reversed. Remove both three-bottle paths.
    private static final Set<String> DUPLICATES = Set.of(
        "neapolitan:milk/milk_bottles_from_bucket",
        "neapolitan:milk/milk_bucket_from_bottles",
        "create:filling/compat/neapolitan/milk_bottle",
        "create:emptying/compat/neapolitan/milk_bottle",
        "neapolitan:vanilla/vanilla_milkshake",
        "neapolitan:chocolate/chocolate_milkshake",
        "neapolitan:strawberry/strawberry_milkshake",
        "neapolitan:banana/banana_milkshake",
        "neapolitan:mint/mint_milkshake",
        "neapolitan:adzuki/adzuki_milkshake",
        "peculiars:yucca_milkshake",
        "peculiars:aloe_milkshake",
        "peculiars:passion_fruit_milkshake");
    public static int removeDuplicates(Map<?, ?> recipes) {
        int before = recipes.size();
        recipes.keySet().removeIf(key -> DUPLICATES.contains(key.toString()));
        return before - recipes.size();
    }
}
