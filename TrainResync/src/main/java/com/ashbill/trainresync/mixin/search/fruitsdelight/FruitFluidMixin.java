package com.ashbill.trainresync.mixin.search.fruitsdelight;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.xkmc.fruitsdelight.init.entries.FruitFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FruitFluid.class)
public abstract class FruitFluidMixin {
    @ModifyReturnValue(method = "isSource", at = @At("RETURN"))
    private boolean trainresync$recognizeRegisteredSource(boolean original) {
        FruitFluid fluid = (FruitFluid) (Object) this;
        return original || fluid == fluid.getSource();
    }
}
