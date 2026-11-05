package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.PotionMixingAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = SoftFluidTank.class, remap = false)
public abstract class SoftFluidTankMixin implements PotionMixingAccess {
    @Shadow public abstract SoftFluidStack getFluid();
    @Unique private int trainresync$effectsBeforeMix = -1;

    @Override public int trainresync$takeEffectsBeforeMix() {
        int previous = trainresync$effectsBeforeMix;
        trainresync$effectsBeforeMix = -1;
        return previous;
    }

    @WrapMethod(method = "addFluid(Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Z)I")
    private int trainresync$rememberPotionMix(SoftFluidStack input, boolean simulate, Operation<Integer> original) {
        if (simulate) return original.call(input, true);
        var stored = getFluid();
        int previous = -1;
        if (!stored.isEmpty() && stored.is(BuiltInSoftFluids.POTION) && input.is(BuiltInSoftFluids.POTION)) {
            var effects = PotionUtils.getAllEffects(stored.getTag());
            if (!effects.equals(PotionUtils.getAllEffects(input.getTag()))) previous = effects.size();
        }
        int added = original.call(input, false);
        trainresync$effectsBeforeMix = added > 0 ? previous : -1;
        return added;
    }
}
