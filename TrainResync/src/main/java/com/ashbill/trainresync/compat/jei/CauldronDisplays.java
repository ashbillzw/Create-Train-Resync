package com.ashbill.trainresync.compat.jei;

import com.ashbill.trainresync.compat.cauldron.*;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronRecipe;
import dev.xkmc.fruitsdelight.content.cauldrons.FDCauldronInteraction;
import dev.xkmc.fruitsdelight.content.cauldrons.FruitCauldronBlock;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.hibiscus.naturespirit.registration.NSTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.mehvahdjukaar.amendments.common.LiquidMixer;
import net.mehvahdjukaar.amendments.common.item.DyeBottleItem;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.ForgeMod;

public final class CauldronDisplays {
    // The native DTO accepts Item for three slots, losing NBT and custom names.
    // Keep the richer stacks only for our JEI entries, for this registration cycle.
    private record Slots(ItemStack state, ItemStack input, ItemStack resultState, ItemStack product, ItemStack container) {}
    private static final Map<CauldronRecipe, Consumer<IRecipeLayoutBuilder>> layouts = new IdentityHashMap<>();
    private static IDrawableStatic nativeRow, slot, arrowAndResult;

    public static IDrawableStatic background(IGuiHelper gui, ResourceLocation texture, int u, int v, int w, int h) {
        nativeRow = gui.createDrawable(texture, u, v, w, h);
        slot = gui.createDrawable(texture, u, v, 18, 18);
        arrowAndResult = gui.createDrawable(texture, u + 36, v, 54, 18);
        return gui.createBlankDrawable(w, h);
    }

    public static IRecipeSlotBuilder nativeBackground(IRecipeSlotBuilder first) {
        return first.setBackground(nativeRow, -1, -1);
    }

    public static boolean layout(CauldronRecipe recipe, IRecipeLayoutBuilder builder) {
        var layout = layouts.get(recipe);
        if (layout == null) return false;
        layout.accept(builder);
        return true;
    }

    private static void nativeSlots(IRecipeLayoutBuilder builder, Slots row) {
        var first = nativeBackground(builder.addInputSlot(1, 1));
        if (row.state().is(Items.MILK_BUCKET)) milk(builder, first);
        else first.addItemStack(row.state());
        builder.addInputSlot(19, 1).addItemStack(row.input());
        if (!row.resultState().isEmpty()) builder.addOutputSlot(73, 1).addItemStack(row.resultState());
        if (!row.product().isEmpty()) builder.addOutputSlot(91, 1).addItemStack(row.product());
        if (!row.container().isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(row.container());
    }

    private static void add(List<CauldronRecipe> recipes, Slots row, boolean heat) {
        var recipe = new CauldronRecipe(row.state().getItem(), row.input().getItem(), row.input().getCount(),
                row.resultState().getItem(), row.product(), heat);
        layouts.put(recipe, builder -> nativeSlots(builder, row));
        recipes.add(recipe);
    }

    static void register(IRecipeRegistration registration) {
        layouts.clear();
        var recipes = new ArrayList<CauldronRecipe>();
        var cheese = MilkIntegration.item("natures_spirit:cheese_bucket");
        grouped(recipes, Items.BROWN_MUSHROOM, cheese, builder -> {
            var row = row(builder, false);
            milk(builder, row.liquid);
            row.material.addIngredients(Ingredient.of(NSTags.Items.CHEESE_MAKER));
            row.result.addItemStack(cheese);
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Items.BUCKET));
        });
        for (String flavor : CauldronContents.FLAVORS.stream().sorted().toList()) {
            var iceCream = MilkIntegration.item(CauldronContents.iceCreamId(flavor));
            var shake = MilkIntegration.item(CauldronContents.milkshakeId(flavor));
            if (iceCream.isEmpty() || shake.isEmpty()) continue;
            add(recipes, new Slots(new ItemStack(Items.MILK_BUCKET), iceCream, new ItemStack(Items.BOWL),
                    shake.copyWithCount(CauldronContents.MILKSHAKE_SERVINGS), new ItemStack(Items.GLASS_BOTTLE)), false);
        }
        addProcessRecipes(recipes);
        addDyeAndPotionRecipes(recipes);
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

    // Native recipes keep their 2 → 2 strip. These rows have 2 or 3 inputs and one output.
    private record Row(IRecipeSlotBuilder liquid, IRecipeSlotBuilder material, IRecipeSlotBuilder result) {}
    private static Row row(IRecipeLayoutBuilder b, boolean addGlow) {
        int x = addGlow ? 1 : 10;
        var liquid = b.addInputSlot(x, 1).setBackground(slot, -1, -1);
        if (addGlow) b.addInputSlot(x += 18, 1).setBackground(slot, -1, -1).addIngredients(Ingredient.of(CauldronGlow.INGREDIENTS));
        var material = b.addInputSlot(x + 18, 1).setBackground(slot, -1, -1);
        var result = b.addOutputSlot(x + 72, 1).setBackground(arrowAndResult, -37, -1);
        return new Row(liquid, material, result);
    }

    private static void grouped(List<CauldronRecipe> recipes, Item material, ItemStack result,
                                Consumer<IRecipeLayoutBuilder> layout) {
        var recipe = new CauldronRecipe(Items.CAULDRON, material, 1, Items.AIR, result, false);
        layouts.put(recipe, layout);
        recipes.add(recipe);
    }

    private static void milk(IRecipeLayoutBuilder builder, IRecipeSlotBuilder slot) {
        if (ForgeMod.MILK.isPresent()) {
            slot.addFluidStack(ForgeMod.MILK.get(), 1000).setFluidRenderer(1000, false, 16, 16);
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Items.MILK_BUCKET));
        } else slot.addItemStack(new ItemStack(Items.MILK_BUCKET));
    }

    private static void water(Row row, int amount) {
        row.liquid.addFluidStack(Fluids.WATER, amount).setFluidRenderer(amount, false, 16, 16);
    }

    private static void addWash(List<CauldronRecipe> recipes, List<ItemStack> inputs, List<ItemStack> outputs) {
        if (inputs.isEmpty()) return;
        grouped(recipes, inputs.get(0).getItem(), outputs.get(0), builder -> {
            var row = row(builder, false);
            water(row, 250);
            row.material.addItemStacks(inputs);
            row.result.addItemStacks(outputs);
            if (outputs.size() > 1) builder.createFocusLink(row.material, row.result);
        });
    }

    private static void addDyeAndPotionRecipes(List<CauldronRecipe> recipes) {
        var dye = DyeBottleItem.fromFluidStack(DyeBottleItem.toFluidStack(DyeColor.RED, 1));
        var inputs = new ArrayList<ItemStack>();
        var outputs = new ArrayList<ItemStack>();
        for (Item white : List.of(Items.WHITE_WOOL, Items.WHITE_CARPET, Items.WHITE_STAINED_GLASS,
                Items.WHITE_STAINED_GLASS_PANE, Items.WHITE_TERRACOTTA)) {
            var red = BlocksColorAPI.changeColor(white, DyeColor.RED);
            if (red != null) {
                inputs.add(new ItemStack(white));
                outputs.add(new ItemStack(red));
            }
        }
        if (!outputs.isEmpty()) grouped(recipes, inputs.get(0).getItem(), outputs.get(0), b -> {
            var row = row(b, false);
            row.liquid.addItemStack(dye);
            row.material.addItemStacks(inputs);
            row.result.addItemStacks(outputs);
            b.createFocusLink(row.material, row.result);
        });

        // The two bottles represent equal portions already in / added to the hot pot.
        var speed = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
        var water = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        var vision = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.NIGHT_VISION);
        addPotionExample(recipes, speed, List.of(vision), false);
        addPotionExample(recipes, speed, List.of(water, CauldronGlow.bottle(water.copy())), true);
    }

    private static void addPotionExample(List<CauldronRecipe> recipes, ItemStack base, List<ItemStack> additions, boolean dilute) {
        var outputs = new ArrayList<ItemStack>();
        for (var addition : additions) {
            var stored = SoftFluidStack.of(BuiltInSoftFluids.POTION, 1, base.getTag().copy());
            var input = PotionUtils.setPotion(new ItemStack(Items.POTION), PotionUtils.getPotion(addition));
            LiquidMixer.mixPotions(stored, SoftFluidStack.of(BuiltInSoftFluids.POTION, 1, input.getTag().copy()));
            var result = new ItemStack(Items.POTION, 2);
            result.setTag(stored.getTag().copy());
            if (dilute && !PotionUtils.getCustomEffects(addition).isEmpty()) CauldronGlow.bottle(result);
            outputs.add(result);
        }
        var recipe = new CauldronRecipe(base.getItem(), additions.get(0).getItem(), 1, Items.AIR, outputs.get(0), true);
        layouts.put(recipe, b -> {
            var row = row(b, false);
            row.liquid.addItemStack(base);
            row.material.addItemStacks(additions);
            row.result.addItemStacks(outputs);
            if (outputs.size() > 1) b.createFocusLink(row.material, row.result);
        });
        recipes.add(recipe);
    }

    private static void addProcessRecipes(List<CauldronRecipe> recipes) {
        var dyes = new ArrayList<ItemStack>();
        var leather = new ArrayList<ItemStack>();
        var banners = new ArrayList<ItemStack>();
        var boxes = new ArrayList<ItemStack>();
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var input = new ItemStack(item);
            if (item instanceof DyeItem) dyes.add(input);
            if (item instanceof DyeableLeatherItem dyeable) {
                dyeable.setColor(input, 0xB02E26);
                leather.add(input);
            } else if (item instanceof BannerItem || CauldronInteraction.WATER.get(item) == CauldronInteraction.BANNER) {
                var patterns = new net.minecraft.nbt.ListTag();
                var pattern = new net.minecraft.nbt.CompoundTag();
                pattern.putString("Pattern", "bs"); pattern.putInt("Color", 14);
                patterns.add(pattern);
                input.getOrCreateTagElement("BlockEntityTag").put("Patterns", patterns);
                banners.add(input);
            } else if (ShulkerBoxBlock.getColorFromItem(item) != null) boxes.add(input);
        }
        var dyed = dyes.stream().map(d -> DyeBottleItem.fromFluidStack(
                DyeBottleItem.toFluidStack(((DyeItem)d.getItem()).getDyeColor(), 1))).toList();
        grouped(recipes, Items.RED_DYE, dyed.get(0), b -> {
            var row = row(b, false);
            water(row, 1000);
            row.material.addItemStacks(dyes);
            row.result.addItemStacks(dyed);
            b.createFocusLink(row.material, row.result);
        });
        addWash(recipes, leather, leather.stream().map(CauldronCrafting::washed).toList());
        addWash(recipes, banners, banners.stream().map(input -> {
            var clean = input.copy();
            BannerBlockEntity.removeLastPattern(clean);
            return clean;
        }).toList());
        addWash(recipes, boxes, List.of(new ItemStack(Items.SHULKER_BOX)));
        var potions = ForgeRegistries.POTIONS.getValues().stream().filter(p -> !p.getEffects().isEmpty()).toList();
        var bottles = potions.stream().map(p -> PotionUtils.setPotion(new ItemStack(Items.POTION), p)).toList();
        for (boolean potion : List.of(false, true)) {
            var liquids = potion ? bottles : List.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
            var glowing = liquids.stream().map(stack -> CauldronGlow.bottle(stack.copy())).toList();
            grouped(recipes, Items.GLOW_INK_SAC, glowing.get(0), b -> {
                var row = row(b, false);
                if (potion) row.liquid.addItemStacks(bottles); else water(row, 250);
                row.material.addIngredients(Ingredient.of(CauldronGlow.INGREDIENTS));
                row.result.addItemStacks(glowing);
                if (potion) b.createFocusLink(row.liquid, row.result);
            });
        }
        var spectral = new ItemStack(Items.SPECTRAL_ARROW);
        grouped(recipes, Items.ARROW, spectral, b -> {
            var row = row(b, true);
            water(row, 250);
            row.material.addItemStack(new ItemStack(Items.ARROW));
            row.result.addItemStack(spectral);
        });
        // The world produces custom effects for full hit duration. Index vanilla arrows as
        // lookup aliases, without changing gameplay or presenting the vanilla 1/8 duration.
        var aliases = potions.stream().map(p -> PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), p)).toList();
        for (boolean glow : List.of(false, true)) {
            var results = aliases.stream().map(alias -> CauldronArrows.tippedArrow(
                    PotionUtils.getPotion(alias).getEffects(), glow, 1).setHoverName(alias.getHoverName())).toList();
            if (results.isEmpty()) continue;
            grouped(recipes, Items.ARROW, results.get(0), b -> {
                var row = row(b, glow);
                row.material.addItemStack(new ItemStack(Items.ARROW));
                row.liquid.addItemStacks(bottles);
                row.result.addItemStacks(results);
                if (glow) b.createFocusLink(row.liquid, row.result);
                else {
                    var lookup = b.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(aliases);
                    b.createFocusLink(row.liquid, row.result, lookup);
                }
                row.result.addTooltipCallback((view, lines) -> view.getDisplayedItemStack().ifPresent(stack -> {
                    var vanilla = new ArrayList<Component>();
                    var full = new ArrayList<Component>();
                    PotionUtils.addPotionTooltip(stack, vanilla, 0.125F);
                    PotionUtils.addPotionTooltip(stack, full, 1F);
                    for (int i = 0; i < vanilla.size() && i < full.size(); i++) {
                        int pos = lines.indexOf(vanilla.get(i));
                        if (pos >= 0) lines.set(pos, full.get(i));
                    }
                }));
            });
        }
    }
}
