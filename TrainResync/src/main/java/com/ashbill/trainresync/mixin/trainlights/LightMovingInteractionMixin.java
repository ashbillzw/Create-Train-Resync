package com.ashbill.trainresync.mixin.trainlights;

import net.adeptstack.ctl.behaviours.interaction.HeadTailLightMovingInteraction;
import net.adeptstack.ctl.behaviours.interaction.InteriorLightMovingInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {HeadTailLightMovingInteraction.class, InteriorLightMovingInteraction.class}, remap = false)
public abstract class LightMovingInteractionMixin {
    @Inject(method = "updateColliders", at = @At("HEAD"), cancellable = true)
    private void trainresync$keepLightColliders(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
