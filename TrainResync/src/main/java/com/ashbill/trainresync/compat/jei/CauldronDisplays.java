package com.ashbill.trainresync.compat.jei;

import com.ashbill.trainresync.compat.cauldron.*;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronRecipe;
import dev.xkmc.fruitsdelight.content.cauldrons.FDCauldronInteraction;
import dev.xkmc.fruitsdelight.content.cauldrons.FruitCauldronBlock;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.hibiscus.naturespirit.registration.NSTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.registries.ForgeRegistries;

public final class CauldronDisplays {
    // The native DTO accepts Item for three slots, losing NBT and custom names.
    // Keep the richer stacks only for our JEI entries, for this registration cycle.
    private record Slots(ItemStack state, ItemStack input, ItemStack resultState, ItemStack product, ItemStack container) {}
    private static final Map<CauldronRecipe, Slots> slots = new IdentityHashMap<>();

    public static boolean layout(CauldronRecipe recipe, IRecipeLayoutBuilder builder) {
        var row = slots.get(recipe);
        if (row == null) return false;
        builder.addInputSlot(1, 1).addItemStack(row.state());
        builder.addInputSlot(19, 1).addItemStack(row.input());
        if (!row.resultState().isEmpty()) builder.addOutputSlot(73, 1).addItemStack(row.resultState());
        if (!row.product().isEmpty()) builder.addOutputSlot(91, 1).addItemStack(row.product());
        if (!row.container().isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(row.container());
        return true;
    }

    private static void add(List<CauldronRecipe> recipes, Slots row, boolean heat) {
        var recipe = new CauldronRecipe(row.state().getItem(), row.input().getItem(), row.input().getCount(),
                row.resultState().getItem(), row.product(), heat);
        slots.put(recipe, row);
        recipes.add(recipe);
    }

    static void register(IRecipeRegistration registration) {
        slots.clear();
        var recipes = new ArrayList<CauldronRecipe>();
        var milk = new ItemStack(Items.MILK_BUCKET).setHoverName(Component.literal("满奶锅 · 1000 mB"));
        for (var ingredient : Ingredient.of(NSTags.Items.CHEESE_MAKER).getItems()) {
            add(recipes, new Slots(milk, ingredient, new ItemStack(Items.CAULDRON).setHoverName(Component.literal("等待熟成、用桶取")),
                    MilkIntegration.item("natures_spirit:cheese_bucket"), new ItemStack(Items.BUCKET)), false);
        }
        for (String flavor : CauldronContents.FLAVORS.stream().sorted().toList()) {
            var iceCream = MilkIntegration.item(CauldronContents.iceCreamId(flavor));
            var shake = MilkIntegration.item(CauldronContents.milkshakeId(flavor));
            if (iceCream.isEmpty() || shake.isEmpty()) continue;
            add(recipes, new Slots(milk, iceCream, new ItemStack(Items.BOWL),
                    shake.copyWithCount(CauldronContents.MILKSHAKE_SERVINGS), new ItemStack(Items.GLASS_BOTTLE)), false);
        }
        addProcessRecipes(recipes);
        // These two top-ups are registered as interactions, outside Fruits Delight's JEI list.
        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            if (!(block instanceof FruitCauldronBlock pot)) continue;
            for (var ingredient : List.of(pot.type.getFruit(), pot.type.getJelly())) {
                if (pot.getInteractions().get(ingredient) instanceof FDCauldronInteraction action) {
                    add(recipes, new Slots(new ItemStack(pot), new ItemStack(ingredient),
                            new ItemStack(pot), action.result(), ItemStack.EMPTY), action.requiresHeat());
                }
            }
        }
        registration.addRecipes(RecipeType.create("fruitsdelight", "cauldron", CauldronRecipe.class),
                recipes.stream().filter(recipe -> !recipe.requiresHeat()).toList());
        registration.addRecipes(RecipeType.create("fruitsdelight", "cauldron_heated", CauldronRecipe.class),
                recipes.stream().filter(CauldronRecipe::requiresHeat).toList());
    }

    private static void process(List<CauldronRecipe> recipes, ItemStack input, ItemStack liquid, ItemStack output, String context) {
        var state = liquid.copy().setHoverName(Component.literal(context + " · ").append(liquid.getHoverName()));
        add(recipes, new Slots(state, input, ItemStack.EMPTY, output, ItemStack.EMPTY), false);
    }

    private static void addProcessRecipes(List<CauldronRecipe> recipes) {
        var water = MilkIntegration.output("water", false);
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var input = new ItemStack(item);
            if (item instanceof DyeItem) {
                var dyed = MilkIntegration.item("minecraft:cauldron");
                dyed.setHoverName(Component.literal("染液锅"));
                process(recipes, input, water.copyWithCount(4), dyed, "满水锅 · 1000 mB");
            }
            if (input.is(CauldronGlow.INGREDIENTS)) {
                var glowing = MilkIntegration.item("minecraft:cauldron");
                glowing.setHoverName(Component.literal("发光水锅"));
                process(recipes, input, water, glowing, "水锅 · 不耗液");
                var potion = MilkIntegration.item("minecraft:potion");
                potion.getOrCreateTag().putString("Potion", "minecraft:swiftness");
                var glowingPotion = MilkIntegration.item("minecraft:cauldron");
                glowingPotion.setHoverName(Component.literal("发光药水锅"));
                process(recipes, input, potion, glowingPotion, "药水锅 · 不耗液");
            }
            if (item instanceof DyeableLeatherItem leather) leather.setColor(input, 0xB02E26);
            else if (item instanceof BannerItem) {
                var patterns = new net.minecraft.nbt.ListTag();
                var pattern = new net.minecraft.nbt.CompoundTag();
                pattern.putString("Pattern", "bs"); pattern.putInt("Color", 14);
                patterns.add(pattern);
                input.getOrCreateTagElement("BlockEntityTag").put("Patterns", patterns);
            } else if (ShulkerBoxBlock.getColorFromItem(item) == null) continue;
            var washed = CauldronCrafting.washed(input);
            if (washed != null) process(recipes, input, water, washed, "250 mB");
        }
        var arrow = MilkIntegration.item("minecraft:arrow");
        process(recipes, arrow, water, MilkIntegration.item("minecraft:spectral_arrow"), "发光水 · 250 mB");
        for (var potion : ForgeRegistries.POTIONS.getValues()) {
            if (potion.getEffects().isEmpty()) continue;
            var bottle = PotionUtils.setPotion(MilkIntegration.item("minecraft:potion"), potion);
            process(recipes, arrow, bottle, CauldronArrows.tippedArrow(potion.getEffects(), false, 1), "250 mB");
            process(recipes, arrow, bottle, CauldronArrows.tippedArrow(potion.getEffects(), true, 1), "发光药水 · 250 mB");
        }
    }
}
