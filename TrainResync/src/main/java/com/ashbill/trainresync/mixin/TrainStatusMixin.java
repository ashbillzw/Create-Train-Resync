package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.simibubi.create.content.trains.entity.TrainStatus;


@Mixin(value = TrainStatus.class, remap = false)
public abstract class TrainStatusMixin {
    @Unique private int trainresync$lastPrintedWarningTime = -100;
    @Unique private int trainresync$collapsedWarnings = 0;

    @Inject(method = "doublePortal", at = @At("HEAD"), cancellable = true)
    private void trainresync$silenceDoublePortalWarning(CallbackInfo ci) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int currentTick = server.getTickCount();

        if (currentTick - trainresync$lastPrintedWarningTime > 99) {
            String warningMessage = "[AshBill] double_portal warning suppressed.";
            if (trainresync$collapsedWarnings > 0) {
                warningMessage += " +" + trainresync$collapsedWarnings + " warnings collapsed.";
            }
            System.out.println(warningMessage);
            trainresync$lastPrintedWarningTime = currentTick;
            trainresync$collapsedWarnings = 0;
        }
        else {
            trainresync$collapsedWarnings++;
        }
        ci.cancel();
    }
}
