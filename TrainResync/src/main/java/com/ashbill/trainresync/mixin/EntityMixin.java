package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.Entity;


@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyArg(
        method = {"setXRot(F)V", "setYRot(F)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/Util;logAndPauseIfInIde(Ljava/lang/String;)V"
        ),
        index = 0
    )
    private String trainresync$improveNanRotationErrorMessage(String s) {
        Entity e = (Entity)(Object)this;
        return "[" + e.getUUID() + " " + e.getType() + " " + e.level().dimension().location() + ":" + e.blockPosition() + "]: " + s;
    }
}
