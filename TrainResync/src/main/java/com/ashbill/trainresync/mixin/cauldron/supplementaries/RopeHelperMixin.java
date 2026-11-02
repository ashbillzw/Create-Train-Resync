package com.ashbill.trainresync.mixin.cauldron.supplementaries;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.common.utils.RopeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RopeHelper.class, remap = false)
public abstract class RopeHelperMixin {
    @Redirect(method = "tryMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1))
    private static boolean trainresync$filledCauldron(Level world, BlockPos pos, BlockState state) {
        String type = state.is(Blocks.WATER_CAULDRON) ? "water" : state.is(Blocks.LAVA_CAULDRON) ? "lava" : "";
        if (type.isEmpty()) return world.setBlockAndUpdate(pos, state);
        if (!world.setBlockAndUpdate(pos, ModRegistry.LIQUID_CAULDRON.get().defaultBlockState())) return false;
        return MilkIntegration.fillContentAt(world, pos, type, 1000, FluidAction.EXECUTE) == 1000;
    }
}
