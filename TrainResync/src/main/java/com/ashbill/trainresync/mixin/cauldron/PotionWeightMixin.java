package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile$1", remap = false)
public abstract class PotionWeightMixin {
    @Unique private MilkAccess trainresync$owner;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void trainresync$owner(LiquidCauldronBlockTile tile, int capacity, CallbackInfo ci) {
        trainresync$owner = (MilkAccess)tile;
    }

    @WrapOperation(method = "addFluidOntoExisting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/LiquidMixer;mixPotions(Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;)V"))
    private void trainresync$weightByVolume(SoftFluidStack stored, SoftFluidStack input, Operation<Void> original) {
        int amount = trainresync$owner.trainresync$milkAmount();
        var weighted = stored.copyWithCount(amount >= 0 ? amount : stored.getCount() * 250);
        original.call(weighted, input.copyWithCount(input.getCount() * 250));
        stored.setTag(weighted.getTag());
    }
}
