package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.safety.AtomicRailwayFile;
import net.minecraft.server.dedicated.ServerWatchdog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWatchdog.class)
public abstract class RailwayWatchdogMixin {
    @Inject(method = "exit", at = @At("HEAD"))
    private void trainresync$awaitRailwaySave(CallbackInfo ci) {
        try {
            AtomicRailwayFile.awaitCurrentSave();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
