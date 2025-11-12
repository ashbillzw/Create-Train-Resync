package com.ashbill.trainresync.mixin;

import java.io.PrintStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;

import net.adeptstack.ctl.behaviours.movement.HeadTailLightMovementBehaviour;


@Mixin(value = HeadTailLightMovementBehaviour.class, remap = false)
public abstract class HeadTailLightMovementBehaviourMixin {

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/PrintStream;println(Ljava/lang/Object;)V",
        )
    )
    private void trainresync$shutupPlease(PrintStream out, Object arg) {}

}
