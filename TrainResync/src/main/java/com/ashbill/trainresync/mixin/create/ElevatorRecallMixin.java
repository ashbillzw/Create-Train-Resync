package com.ashbill.trainresync.mixin.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ElevatorContraption.class, remap = false)
abstract class ElevatorRecallMixin {
    @Shadow protected int contactYOffset;
    @Unique private boolean trainresync$checkedRecall;

    @Inject(method = "tickStorage", at = @At("HEAD"))
    private void trainresync$recallStrandedCabin(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (trainresync$checkedRecall || entity.tickCount < 20 || entity.tickCount % 20 != 0) return;
        var level = entity.level();
        if (level.isClientSide) return;
        var cabin = (ElevatorContraption) (Object) this;
        var coords = cabin.getGlobalColumn();
        var zero = new BlockPos(coords.x(), 0, coords.z());
        if (!level.hasChunkAt(zero)) return;
        var column = ElevatorColumn.get(level, coords);
        if (column == null || !column.isActive()) return;
        trainresync$checkedRecall = true;
        if (!cabin.arrived || Math.abs(entity.getY() + contactYOffset) > 1.0 / 128
                || (column.isTargetAvailable() && column.getTargetedYLevel() != 0)
                || coords.equals(ElevatorContactBlock.getColumnCoords(level, zero))) return;

        // All floors share one chunk. Include stored block entities which have not yet
        // registered themselves in Create's in-memory list, so a partial list cannot vote.
        String lastFloor = null;
        BlockPos destination = null;
        for (var pos : level.getChunkAt(zero).getBlockEntitiesPos()) {
            if (pos.getX() != coords.x() || pos.getZ() != coords.z()
                    || !coords.equals(ElevatorContactBlock.getColumnCoords(level, pos))
                    || !(level.getBlockEntity(pos) instanceof ElevatorContactBlockEntity floor)) continue;
            if (floor.columnCoords == null) {
                trainresync$checkedRecall = false;
                return;
            }
            String reported = floor.lastReportedCurrentFloor;
            if (reported == null || reported.isBlank() || (lastFloor != null && !lastFloor.equals(reported))) return;
            lastFloor = reported;
            if (reported.equals(floor.shortName)) {
                if (destination != null) return;
                destination = pos;
            }
        }
        if (destination == null || cabin.isTargetUnreachable(destination.getY())) return;
        var state = level.getBlockState(destination);
        if (state.getBlock() instanceof ElevatorContactBlock contact)
            contact.callToContactAndUpdate(column, state, level, destination, false);
    }
}
