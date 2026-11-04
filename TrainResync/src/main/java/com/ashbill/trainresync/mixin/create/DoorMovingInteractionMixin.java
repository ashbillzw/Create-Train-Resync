package com.ashbill.trainresync.mixin.create;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DoorMovingInteraction.class, remap = false)
public abstract class DoorMovingInteractionMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void trainresync$skipIncompleteDoor(Player player, Contraption contraption, BlockPos pos,
            BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (!(state.getBlock() instanceof DoorBlock)) return;
        BlockPos other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (!contraption.getBlocks().containsKey(other)) cir.setReturnValue(state);
    }
}
