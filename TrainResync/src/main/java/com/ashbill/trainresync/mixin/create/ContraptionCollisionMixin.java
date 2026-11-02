package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.collision.CarriageColliderUpdates;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionCollisionMixin implements CarriageColliderUpdates {
    @Shadow public Optional<List<AABB>> simplifiedEntityColliders;
    @Shadow public AbstractContraptionEntity entity;
    @Shadow public abstract Map<BlockPos, StructureBlockInfo> getBlocks();
    @Shadow public abstract ContraptionWorld getContraptionWorld();

    @Unique private boolean trainresync$dirty;
    @Unique private CompletableFuture<List<AABB>> trainresync$pending;

    @Inject(method = "gatherBBsOffThread", at = @At("HEAD"), cancellable = true)
    private void trainresync$deferInitialCalculation(CallbackInfo ci) {
        if ((Object) this instanceof CarriageContraption) {
            trainresync$markCollidersDirty();
            ci.cancel();
        }
    }

    @Inject(method = "invalidateColliders", at = @At("HEAD"), cancellable = true)
    private void trainresync$deferRebuild(CallbackInfo ci) {
        if (!((Object) this instanceof CarriageContraption)) return;
        trainresync$markCollidersDirty();
        // setBlock updates every present dimension; their caches must follow too.
        if (entity instanceof CarriageContraptionEntity carriageEntity
                && carriageEntity.getCarriage() != null) {
            carriageEntity.getCarriage().forEachPresentEntity(other -> {
                if (other.getContraption() instanceof CarriageContraption contraption)
                    ((CarriageColliderUpdates) contraption).trainresync$markCollidersDirty();
            });
        }
        ci.cancel();
    }

    @Override
    public void trainresync$markCollidersDirty() {
        trainresync$dirty = true;
    }

    @Override
    public void trainresync$tickColliders() {
        if (trainresync$pending != null) {
            if (!trainresync$pending.isDone()) return;
            try {
                // Always advance to the completed result, even if another rebuild is due.
                simplifiedEntityColliders = Optional.of(trainresync$pending.join());
            } catch (CompletionException failure) {
                Create.LOGGER.error("Failed to rebuild carriage colliders", failure.getCause());
            }
            trainresync$pending = null;
        }
        if (!trainresync$dirty) return;
        trainresync$dirty = false;

        // World-dependent shape queries stay on the tick thread. The worker owns only geometry.
        ContraptionWorld world = getContraptionWorld();
        List<VoxelShape> shapes = getBlocks().entrySet().stream().map(entry -> {
            BlockPos pos = entry.getKey();
            return entry.getValue().state().getCollisionShape(world, pos, CollisionContext.empty())
                    .move(pos.getX(), pos.getY(), pos.getZ());
        }).filter(shape -> !shape.isEmpty()).toList();
        trainresync$pending = CompletableFuture.supplyAsync(() -> trainresync$combine(shapes));
    }

    @Unique
    private static List<AABB> trainresync$combine(List<VoxelShape> shapes) {
        VoxelShape combined = Shapes.empty();
        for (VoxelShape shape : shapes)
            combined = Shapes.joinUnoptimized(combined, shape, BooleanOp.OR);
        return combined.optimize().toAabbs();
    }
}
