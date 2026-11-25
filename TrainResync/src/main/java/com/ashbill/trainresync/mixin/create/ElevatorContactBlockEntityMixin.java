package com.ashbill.trainresync.mixin.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ElevatorContactBlockEntity.class, remap = false)
abstract class ElevatorContactBlockEntityMixin {
    // Create writes the int's default (zero) even when the column has no target yet.
    @ModifyExpressionValue(method = "write", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn;get(Lnet/minecraft/world/level/LevelAccessor;Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn$ColumnCoords;)Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn;"))
    private ElevatorColumn trainresync$columnWithTarget(ElevatorColumn column) {
        return column != null && column.isTargetAvailable() ? column : null;
    }

    // Old saves can contain an explicit zero. Check the physical contact, since other
    // floors in the same chunk may not have registered with the column during load yet.
    @WrapWithCondition(method = {"read", "initialize"}, at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn;target(I)V"), require = 2)
    private boolean trainresync$restoreFloor(ElevatorColumn column, int target) {
        if (target != 0) return true;
        var contact = (ElevatorContactBlockEntity) (Object) this;
        return contact.columnCoords.equals(ElevatorContactBlock.getColumnCoords(
                contact.getLevel(), column.contactAt(0)));
    }
}
