package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronContainers;
import net.mehvahdjukaar.moonlight.api.util.DispenserHelper.FillFluidHolderBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FillFluidHolderBehavior.class, remap = false)
public abstract class FillFluidHolderBehaviorMixin {
    @Inject(method = "customBehavior", at = @At("HEAD"), cancellable = true)
    private void trainresync$containers(BlockSource source, ItemStack held, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var result = CauldronContainers.dispense(source, held);
        if (result != null) cir.setReturnValue(result);
    }
}
