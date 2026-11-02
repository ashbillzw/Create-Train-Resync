package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.collision.CarriageColliderUpdates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ContraptionHandler.class, remap = false)
public abstract class ContraptionHandlerMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/ContraptionCollider;collideEntities(Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;)V"))
    private static void trainresync$updateColliders(AbstractContraptionEntity entity, Operation<Void> original) {
        if (entity.getContraption() instanceof CarriageContraption contraption)
            ((CarriageColliderUpdates) contraption).trainresync$tickColliders();
        original.call(entity);
    }
}
