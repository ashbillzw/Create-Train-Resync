package com.ashbill.trainresync.mixin.cauldron.tavern;

import com.ashbill.trainresync.compat.cauldron.CauldronViews;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.TapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TapBlock.class, remap = false)
public abstract class TapBlockMixin {
    @Redirect(method = "tryOpen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$openView(Level world, BlockPos pos) { return CauldronViews.tap(world, pos); }

    @Redirect(method = "m_213897_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$finishView(ServerLevel world, BlockPos pos) { return CauldronViews.tap(world, pos); }
}
