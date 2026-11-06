package com.ashbill.trainresync.compat.cauldron;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.mehvahdjukaar.moonlight.api.fluids.*;

public final class CauldronContents {
    public static final int MILKSHAKE_SERVINGS = 4;
    // Fixed flavors; additions must also update the matching soft-fluid resources.
    private static final Set<String> PECULIARS = Set.of("yucca", "aloe", "passion_fruit");
    public static final Set<String> FLAVORS = Set.of("vanilla", "chocolate", "strawberry", "banana", "mint", "adzuki", "yucca", "aloe", "passion_fruit");
    private static String namespace(String flavor) {
        if (!FLAVORS.contains(flavor)) throw new IllegalArgumentException("Unsupported milkshake flavor: " + flavor);
        return PECULIARS.contains(flavor) ? "peculiars" : "neapolitan";
    }
    public static String iceCreamId(String flavor) { return namespace(flavor) + ":" + flavor + "_ice_cream"; }
    public static String milkshakeId(String flavor) { return namespace(flavor) + ":" + flavor + "_milkshake"; }
    public static String milkshakeTranslation(String flavor) { return "item." + namespace(flavor) + "." + flavor + "_milkshake"; }
    public static boolean shake(String type) { return type.startsWith("shake:") && FLAVORS.contains(type.substring(6)); }
    public static boolean liquid(String type) { return type.equals("milk") || type.equals("water") || type.equals("lava"); }
    public static String type(FluidStack stack) {
        if (stack.isEmpty() || stack.hasTag()) return "";
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null || !id.getNamespace().equals("minecraft")) return "";
        String path = id.getPath();
        return liquid(path) ? path : "";
    }
    public static FluidStack fluid(String type, int amount) {
        if (!liquid(type) || amount <= 0) return FluidStack.EMPTY;
        return new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("minecraft", type)), amount);
    }
    public static String type(SoftFluidStack stack) {
        if (stack.isEmpty()) return "";
        if (!stack.hasTag() || stack.getTag().isEmpty()) {
            if (stack.is(BuiltInSoftFluids.MILK)) return "milk";
            if (stack.is(BuiltInSoftFluids.WATER)) return "water";
            if (stack.is(BuiltInSoftFluids.LAVA)) return "lava";
        }
        for (String flavor : FLAVORS) {
            if (stack.is(SoftFluidRegistry.getHolder(new ResourceLocation("shader_switch_debug", flavor + "_milkshake")))) return "shake:" + flavor;
        }
        return MappedFluids.type(stack);
    }
    public static SoftFluidStack soft(String type, int layers) {
        if (layers <= 0) return SoftFluidStack.empty();
        if (shake(type)) return SoftFluidStack.of(SoftFluidRegistry.getHolder(new ResourceLocation("shader_switch_debug", type.substring(6) + "_milkshake")), layers);
        return SoftFluidStack.of(switch (type) {
            case "water" -> BuiltInSoftFluids.WATER;
            case "lava" -> BuiltInSoftFluids.LAVA;
            default -> BuiltInSoftFluids.MILK;
        }, layers);
    }
}
