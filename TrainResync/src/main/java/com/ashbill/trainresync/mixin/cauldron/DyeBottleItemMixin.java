package com.ashbill.trainresync.mixin.cauldron;

import net.mehvahdjukaar.amendments.common.item.DyeBottleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DyeBottleItem.class, remap = false)
public abstract class DyeBottleItemMixin {
    @Inject(method = "mixColor(IIII)I", at = @At("HEAD"), cancellable = true)
    private static void trainresync$preserveIdenticalColor(int first, int second, int firstAmount, int secondAmount,
            CallbackInfoReturnable<Integer> cir) {
        if (first == second) cir.setReturnValue(first);
    }
}
