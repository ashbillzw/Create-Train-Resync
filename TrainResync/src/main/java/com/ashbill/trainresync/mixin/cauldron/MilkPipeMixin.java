package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.fluids.OpenEndedPipe$OpenEndFluidHandler", remap = false)
public abstract class MilkPipeMixin {
    @Shadow @Final private OpenEndedPipe this$0;
    @Inject(method = "drainInner", at = @At("HEAD"), cancellable = true)
    private void trainresync$drainCauldron(int amount, FluidStack filter, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        // Let Create empty an existing pipe buffer before drawing from the block.
        if (!((net.minecraftforge.fluids.capability.templates.FluidTank)(Object)this).getFluid().isEmpty()) return;
        var world = this$0.getWorld();
        var pos = this$0.getOutputPos();
        if (world == null || !world.isLoaded(pos)) return;
        var tank = MilkIntegration.tank(world, pos);
        if (tank == null) return;
        var handler = (net.minecraftforge.fluids.capability.IFluidHandler)tank;
        if (filter == null) cir.setReturnValue(handler.drain(Math.max(0, amount), action));
        else {
            var request = filter.copy();
            if (!request.isEmpty()) request.setAmount(Math.max(0, amount));
            cir.setReturnValue(handler.drain(request, action));
        }
    }
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void trainresync$fillOrdinaryCauldron(FluidStack input, FluidAction action, CallbackInfoReturnable<Integer> cir) {
        int accepted = MilkIntegration.fillAt(this$0.getWorld(), this$0.getOutputPos(), input, action);
        if (accepted >= 0) cir.setReturnValue(accepted);
    }
}
