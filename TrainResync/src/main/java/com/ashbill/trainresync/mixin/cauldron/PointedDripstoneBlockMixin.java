package com.ashbill.trainresync.mixin.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PointedDripstoneBlock.class, remap = false)
public abstract class PointedDripstoneBlockMixin {
    @Redirect(method = "m_213898_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;m_221859_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;F)V"))
    private void trainresync$quarterBucketRate(BlockState state, ServerLevel world, BlockPos pos, float chance) {
        // Only the top stalactite segment can transfer: its source is two blocks up.
        // Inspect actual fluid so mud conversion and Neapolitan's milk keep their rates.
        var source = world.getFluidState(pos.above(2)).getType();
        if (source == Fluids.WATER) chance *= 0.75F;
        else if (source == Fluids.LAVA) chance *= 0.25F;
        PointedDripstoneBlock.maybeTransferFluid(state, world, pos, chance);
    }
}
