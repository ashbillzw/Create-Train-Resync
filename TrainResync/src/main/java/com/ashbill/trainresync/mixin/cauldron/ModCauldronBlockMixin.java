package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkAmounts;
import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.mehvahdjukaar.amendments.common.block.ModCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModCauldronBlock.class, remap = false)
public abstract class ModCauldronBlockMixin {
    @Inject(method = "m_6782_", at = @At("HEAD"), cancellable = true)
    private void trainresync$exactFluidSignal(BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null) cir.setReturnValue(MilkAmounts.comparatorSignal(tank.trainresync$milkAmount(), tank.trainresync$capacity()));
        else {
            var level = ((ModCauldronBlock)(Object)this).getLevelProperty();
            cir.setReturnValue(MilkAmounts.comparatorSignal(state.getValue(level), java.util.Collections.max(level.getPossibleValues())));
        }
    }
}
