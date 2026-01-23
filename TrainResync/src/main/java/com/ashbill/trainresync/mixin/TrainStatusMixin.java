package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.entity.TrainStatus;


@Mixin(value = TrainStatus.class, remap = false)
public abstract class TrainStatusMixin {
    @Inject(method = "doublePortal", at = @At("HEAD"))
    private static void trainresync$silenceDoublePortalWarning(CallbackInfo ci) {
        System.out.println("[AshBill] double_portal warning suppressed.");
        ci.cancel();
    }
}
