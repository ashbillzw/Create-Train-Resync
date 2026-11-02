package com.ashbill.trainresync.mixin.cauldron;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mehvahdjukaar.amendments.common.item.DyeBottleItem;
import net.mehvahdjukaar.amendments.common.recipe.DyeBottleRecipe;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DyeBottleRecipe.class, remap = false)
public abstract class DyeBottleRecipeMixin {
    @Redirect(method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/item/DyeBottleItem;mixColor(IIII)I"))
    private int trainresync$mixLeatherColor(int dye, int leather, int dyeAmount, int leatherAmount) {
        if ((dye & 0xFFFFFF) == (leather & 0xFFFFFF)) return leather;
        return DyeBottleItem.mixColor(dye, leather, dyeAmount, leatherAmount);
    }

    @Unique private static boolean trainresync$validInputs(CraftingContainer container) {
        ItemStack dye = ItemStack.EMPTY, target = ItemStack.EMPTY;
        for (var stack : container.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == ModRegistry.DYE_BOTTLE_ITEM.get()) {
                if (!dye.isEmpty()) return false;
                dye = stack;
            } else {
                if (!target.isEmpty()) return false;
                target = stack;
            }
        }
        return !dye.isEmpty() && !target.isEmpty()
                && (target.getItem() instanceof DyeableLeatherItem
                || BlocksColorAPI.changeColor(target.getItem(), DyeBottleItem.getClosestDye(dye)) != null);
    }

    @Inject(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void trainresync$rejectExtraInputs(CraftingContainer container, Level world, CallbackInfoReturnable<Boolean> cir) {
        if (!trainresync$validInputs(container)) cir.setReturnValue(false);
    }

    @Inject(method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void trainresync$rejectInvalidResult(CraftingContainer container, RegistryAccess registry, CallbackInfoReturnable<ItemStack> cir) {
        if (!trainresync$validInputs(container)) cir.setReturnValue(ItemStack.EMPTY);
    }
}
