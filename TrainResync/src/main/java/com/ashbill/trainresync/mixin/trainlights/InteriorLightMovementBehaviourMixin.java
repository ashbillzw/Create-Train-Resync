package com.ashbill.trainresync.mixin.trainlights;

import com.simibubi.create.content.contraptions.Contraption;
import net.adeptstack.ctl.behaviours.movement.InteriorLightMovementBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = InteriorLightMovementBehaviour.class, remap = false)
public abstract class InteriorLightMovementBehaviourMixin {
    @Redirect(method = "toggleLight", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/Contraption;invalidateColliders()V"))
    private void trainresync$keepLightColliders(Contraption contraption) {}
}
