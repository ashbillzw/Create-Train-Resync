package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.PowderSnowCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PowderSnowCauldronBlock.class, remap = false)
public abstract class PowderSnowCauldronBlockMixin {
    @Inject(method = "m_142266_", at = @At("HEAD"), cancellable = true)
    private void trainresync$meltIntoUnifiedWater(BlockState state, Level world, BlockPos pos, CallbackInfo ci) {
        // Final amounts after extinguishing: 1/2/3 snow layers become 250/500/750 mB.
        int water = state.getValue(LayeredCauldronBlock.LEVEL) * 250;
        world.setBlockAndUpdate(pos, ModRegistry.LIQUID_CAULDRON.get().defaultBlockState());
        MilkIntegration.fillContentAt(world, pos, "water", water, FluidAction.EXECUTE);
        world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(world.getBlockState(pos)));
        ci.cancel();
    }
}
