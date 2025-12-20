package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.Util;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityRotationNaNDebugMixin {

    @Redirect(
        method = "setYRot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/Util;logAndPauseIfInIde(Ljava/lang/String;)V"
        )
    )
    private void trainresync$debugNaNYRot(String msg) {
        Entity e = (Entity) (Object) this;
        System.err.println("[NaNRotDebug] " + msg
            + " entity=" + e.getType()
            + " uuid=" + e.getUUID()
            + " id=" + e.getId()
            + " dim=" + e.level().dimension().location()
            + " pos=" + e.blockPosition()
        );
        new RuntimeException("NaN YRot stack").printStackTrace();
        // vanilla spam is suppressed because we redirected the call
    }

    @Redirect(
        method = "setXRot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/Util;logAndPauseIfInIde(Ljava/lang/String;)V"
        )
    )
    private void trainresync$debugNaNXRot(String msg) {
        Entity e = (Entity) (Object) this;
        System.err.println("[NaNRotDebug] " + msg
            + " entity=" + e.getType()
            + " uuid=" + e.getUUID()
            + " id=" + e.getId()
            + " dim=" + e.level().dimension().location()
            + " pos=" + e.blockPosition()
        );
        new RuntimeException("NaN XRot stack").printStackTrace();
    }
}
