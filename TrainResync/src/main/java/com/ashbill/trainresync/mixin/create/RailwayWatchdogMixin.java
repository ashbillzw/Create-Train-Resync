package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.safety.AtomicRailwayFile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.dedicated.ServerWatchdog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWatchdog.class)
public abstract class RailwayWatchdogMixin {
    @Unique private static final Path trainresync$heapDumpFlag = Path.of("trainresync-heap-dump.flag");
    @Unique private boolean trainresync$heapDump;

    @ModifyExpressionValue(method = "run", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/dedicated/ServerWatchdog;maxTickTime:J", ordinal = 0))
    private long trainresync$diagnosticLimit(long original) {
        trainresync$heapDump = Files.isRegularFile(trainresync$heapDumpFlag);
        return trainresync$heapDump ? 300_000L : original;
    }

    @ModifyArg(method = "run", at = @At(value = "INVOKE",
            target = "Ljava/lang/Thread;sleep(J)V", remap = false), index = 0)
    private long trainresync$diagnosticSleep(long original) {
        // A tolerated slow tick can make vanilla's sleep negative; recheck instead of exiting the watchdog.
        return trainresync$heapDump ? Math.max(1_000L, original) : original;
    }

    @Inject(method = "exit", at = @At("HEAD"))
    private void trainresync$awaitRailwaySave(CallbackInfo ci) {
        try {
            AtomicRailwayFile.awaitCurrentSave();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
