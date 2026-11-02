package com.ashbill.trainresync.mixin.trainlights;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import java.io.PrintStream;
import net.adeptstack.ctl.behaviours.movement.HeadTailLightMovementBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeadTailLightMovementBehaviour.class, remap = false)
public abstract class HeadTailLightMovementBehaviourMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void trainresync$serverOnly(MovementContext context, CallbackInfo ci) {
        if (context.world.isClientSide()) ci.cancel();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;)V"))
    private void trainresync$sendChangedMode(AbstractContraptionEntity entity, BlockPos pos,
            StructureBlockInfo next, Operation<Void> original) {
        StructureBlockInfo current = entity.getContraption().getBlocks().get(pos);
        if (current == null || current.state() != next.state()) original.call(entity, pos, next);
    }

    @Redirect(method = "toggleLight", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/Contraption;invalidateColliders()V"))
    private void trainresync$keepLightColliders(Contraption contraption) {}

    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Ljava/io/PrintStream;println(Ljava/lang/Object;)V"))
    private void trainresync$shutupPlease(PrintStream out, Object arg) {}
}
