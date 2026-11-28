package com.ashbill.trainresync.mixin.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class ContraptionSeatDespawnMixin {
    @Unique private boolean trainresync$ignoreSeatForDespawn;

    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;requiresCustomPersistence()Z"))
    private boolean trainresync$checkSeatPersistence(Mob mob, Operation<Boolean> original) {
        if (!(mob.getVehicle() instanceof AbstractContraptionEntity vehicle)) return original.call(mob);
        Contraption contraption = vehicle.getContraption();
        BlockPos seat = contraption == null ? null : contraption.getSeatOf(mob.getUUID());
        if (seat == null) return original.call(mob);
        if (contraption instanceof CarriageContraption carriage) {
            Couple<Boolean> controls = carriage.conductorSeats.get(seat);
            if (controls != null && (controls.getFirst() || controls.getSecond())) return original.call(mob);
        }

        // Let the mob evaluate its own persistence, ignoring only the base passenger check.
        trainresync$ignoreSeatForDespawn = true;
        try {
            return original.call(mob);
        } finally {
            trainresync$ignoreSeatForDespawn = false;
        }
    }

    @ModifyExpressionValue(method = "requiresCustomPersistence", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isPassenger()Z"))
    private boolean trainresync$passengerPersistence(boolean passenger) {
        return passenger && !trainresync$ignoreSeatForDespawn;
    }
}
