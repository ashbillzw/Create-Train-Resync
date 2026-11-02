package com.ashbill.trainresync.mixin.cauldron.tavern;

import com.ashbill.trainresync.compat.cauldron.CauldronContents;
import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import com.github.ysbbbbbb.kaleidoscopetavern.game.tap.impl.LavaCauldronTapBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LavaCauldronTapBehavior.class, remap = false)
public abstract class LavaCauldronTapBehaviorMixin {
    @Redirect(method = "onEndExtract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean trainresync$fillUnified(Level world, BlockPos pos, BlockState state) {
        if (state.is(Blocks.LAVA_CAULDRON)) {
            int filled = MilkIntegration.fillAt(world, pos, CauldronContents.fluid("lava", 1000), FluidAction.EXECUTE);
            if (filled >= 0) return filled > 0;
        }
        return world.setBlockAndUpdate(pos, state);
    }
}
