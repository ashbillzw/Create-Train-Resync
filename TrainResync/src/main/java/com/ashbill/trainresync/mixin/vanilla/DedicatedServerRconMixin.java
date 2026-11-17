package com.ashbill.trainresync.mixin.vanilla;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DedicatedServer.class)
abstract class DedicatedServerRconMixin {
    @Unique private final CompletableFuture<Void> trainresync$rconClosed = new CompletableFuture<>();

    @Inject(method = "onServerExit", at = @At("HEAD"))
    private void trainresync$releaseRconClients(CallbackInfo ci) {
        // The main thread is about to join the RCON workers and can no longer serve their queue.
        trainresync$rconClosed.completeExceptionally(new CancellationException("Server stopping"));
    }

    @Redirect(method = "runCommand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/dedicated/DedicatedServer;executeBlocking(Ljava/lang/Runnable;)V"))
    private void trainresync$waitForRconCommand(DedicatedServer server, Runnable task) {
        if (trainresync$rconClosed.isDone()) {
            throw new CancellationException("Server stopping");
        }
        if (server.isSameThread()) {
            task.run();
            return;
        }
        CompletableFuture<Void> command = server.submitAsync(task);
        try {
            CompletableFuture.anyOf(command, trainresync$rconClosed).join();
        } finally {
            // If shutdown won, the queued task must not execute later.
            command.cancel(false);
        }
    }
}
