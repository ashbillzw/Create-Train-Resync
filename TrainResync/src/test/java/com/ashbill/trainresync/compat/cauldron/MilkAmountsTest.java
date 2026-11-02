package com.ashbill.trainresync.compat.cauldron;

public final class MilkAmountsTest {
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    public static void main(String[] args) {
        // Registered native containers must preserve fractional pipe remainders.
        for (int stored = 0; stored <= 1000; stored++) {
            for (int container : new int[]{250, 500, 1000}) {
                if (MilkAmounts.wholeContainerFits(stored, container, false))
                    check(MilkAmounts.reconcileLayers(stored, MilkAmounts.layers(stored) - container / 250) == stored - container,
                        "native extraction preserves fractional remainder");
                else check(stored < container, "rounded layer cannot provide an extra container");
                if (MilkAmounts.wholeContainerFits(stored, container, true))
                    check(MilkAmounts.reconcileLayers(stored, MilkAmounts.layers(stored) + container / 250) == stored + container,
                        "native insertion preserves fractional remainder");
            }
        }
        // Check both sides of every threshold, including changes within one visual layer.
        for (int capacity : new int[]{1000, 4000, 16000}) {
            check(MilkAmounts.comparatorSignal(0, capacity) == 0, "empty signal");
            for (int signal = 1; signal <= 15; signal++) {
                int threshold = (capacity * signal + 14) / 15;
                check(MilkAmounts.comparatorSignal(threshold - 1, capacity) == signal - 1, "below signal threshold");
                check(MilkAmounts.comparatorSignal(threshold, capacity) == signal, "at signal threshold");
            }
        }
        for (int servings = 1; servings <= 64; servings++) {
            int capacity = servings * 250;
            int remaining = capacity;
            check(MilkAmounts.displayLayers(remaining, capacity) == 4, "full shake has four visual layers");
            for (int bottle = 0; bottle < servings; bottle++) {
                check(remaining >= 250, "every configured serving is extractable");
                remaining -= 250;
                int layers = MilkAmounts.displayLayers(remaining, capacity);
                check(layers >= 0 && layers <= 4, "no invalid blockstate for increased yield");
                check((remaining == 0) == (layers == 0), "last bottle empties tank");
            }
            check(remaining == 0, "exact configured milkshake yield");
        }
        int[] requests = {-1, 0, 1, 7, 249, 250, 251, 500, 999, 1000, 1001, Integer.MAX_VALUE};
        for (int stored = 0; stored <= 1000; stored++) {
            for (int request : requests) {
                int added = MilkAmounts.fill(stored, request);
                int taken = MilkAmounts.drain(stored, request);
                check(added >= 0 && added <= Math.max(0, request) && stored + added <= 1000, "fill bounds");
                check(taken >= 0 && taken <= Math.max(0, request) && taken <= stored, "drain bounds");
                check(MilkAmounts.drain(stored + added, added) == added, "round-trip conservation");
            }
            check(MilkAmounts.reconcileLayers(stored, MilkAmounts.layers(stored)) == stored, "no rounding on save/change");
            check(MilkAmounts.wholeContainerFits(stored, 250, false) == (stored >= 250), "bottle extraction");
            check(MilkAmounts.wholeContainerFits(stored, 1000, false) == (stored == 1000), "full bucket only");
            check(MilkAmounts.wholeContainerFits(stored, 1000, true) == (stored == 0), "no overflow or partial bucket consumption");
        }
        int tank = 0, consumed = 0, recovered = 0;
        while (tank < 1000) { int added = MilkAmounts.fill(tank, 7); tank += added; consumed += added; }
        for (int i = 0; i < 4; i++) { int taken = MilkAmounts.drain(tank, 250); tank -= taken; recovered += taken; }
        check(tank == 0 && consumed == recovered && consumed == 1000, "low-speed pump -> four bottles");
        check(MilkAmounts.reconcileLayers(251, 1) == 1, "external layer drain preserves remainder");
        check(MilkAmounts.fill(0, 7) == 7, "empty cauldron refill accepts sub-bottle flow");
        var recipes = new java.util.HashMap<String, String>();
        recipes.put("neapolitan:milk/milk_bottles_from_bucket", "three bottles");
        recipes.put("neapolitan:milk/milk_bucket_from_bottles", "three bottles");
        recipes.put("create:filling/compat/neapolitan/milk_bottle", "duplicate");
        recipes.put("create:emptying/compat/neapolitan/milk_bottle", "wrong input");
        recipes.put("farmersdelight:milk_bottle", "keep");
        recipes.put("farmersdelight:milk_bucket_from_bottles", "keep");
        recipes.put("create:emptying/compat/farmersdelight/milk_bottle", "keep");
        for (String flavor : new String[]{"vanilla", "chocolate", "strawberry", "banana", "mint", "adzuki"})
            recipes.put("neapolitan:" + flavor + "/" + flavor + "_milkshake", "remove");
        for (String flavor : new String[]{"yucca", "aloe", "passion_fruit"})
            recipes.put("peculiars:" + flavor + "_milkshake", "remove");
        recipes.put("neapolitan:chocolate/chocolate_ice_cream", "keep");
        recipes.put("peculiars:yucca_ice_cream", "keep");
        recipes.put("create:mixing/compat/neapolitan/chocolate_milkshake", "keep");
        check(MilkRecipes.removeDuplicates(recipes) == 13 && recipes.size() == 6, "targeted recipe removal");
        check(recipes.values().stream().allMatch("keep"::equals), "unrelated routes retained");
        System.out.println("Milk accounting PASS: 1001 storage states, low-flow conservation, container limits, fractional remainder.");
    }
}
