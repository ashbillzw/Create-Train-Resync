package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import com.ashbill.trainresync.compat.cauldron.CauldronPotions;
import com.ashbill.trainresync.compat.cauldron.MappedFluids;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.fluids.OpenEndedPipe$OpenEndFluidHandler", remap = false)
public abstract class MilkPipeMixin extends FluidTank {
    protected MilkPipeMixin(int capacity) { super(capacity); }
    @Shadow @Final private OpenEndedPipe this$0;
    @Inject(method = "drainInner", at = @At("HEAD"), cancellable = true)
    private void trainresync$drainCauldron(int amount, FluidStack filter, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        // Let Create empty an existing pipe buffer before drawing from the block.
        if (!getFluid().isEmpty()) return;
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
        var world = this$0.getWorld();
        var pos = this$0.getOutputPos();
        if (world == null || !world.isLoaded(pos)) return;
        var tank = MilkIntegration.tank(world, pos);
        var soft = MappedFluids.fromForge(input);
        boolean potion = tank != null && soft.is(BuiltInSoftFluids.POTION);
        boolean different = potion && CauldronPotions.different(tank, soft);
        if (potion && (different || !getFluid().isEmpty())) {
            if (!getFluid().isEmpty() && !getFluid().isFluidEqual(input)) {
                cir.setReturnValue(0);
                return;
            }
            int buffered = getFluidAmount();
            if (!different) {
                // If the player changed the cauldron to match, resume ordinary mB transfer.
                var combined = input.copy();
                combined.grow(buffered);
                int delivered = MilkIntegration.fillAt(world, pos, combined, FluidAction.SIMULATE);
                int accepted = Math.max(0, delivered - buffered);
                if (accepted > 0 && action.execute()) {
                    combined.setAmount(delivered);
                    MilkIntegration.fillAt(world, pos, combined, action);
                    setFluid(FluidStack.EMPTY);
                }
                cir.setReturnValue(accepted);
                return;
            }
            if (CauldronPotions.mix(tank, soft, FluidAction.SIMULATE) != 250) {
                cir.setReturnValue(0);
                return;
            }
            // Reuse Create's saved, reversible pipe buffer; only a full portion reaches the pot.
            int accepted = Math.min(input.getAmount(), buffered >= 250 ? getCapacity() - buffered + 250 : 250 - buffered);
            if (accepted > 0 && action.execute()) {
                if (buffered >= 250) {
                    CauldronPotions.mix(tank, soft, action);
                    super.drain(250, action);
                }
                var portion = input.copy();
                portion.setAmount(accepted);
                super.fill(portion, action);
                if (buffered < 250 && getFluidAmount() == 250) {
                    CauldronPotions.mix(tank, soft, action);
                    super.drain(250, action);
                }
            }
            cir.setReturnValue(accepted);
            return;
        }
        int accepted = MilkIntegration.fillAt(world, pos, input, action);
        if (accepted >= 0) cir.setReturnValue(accepted);
    }
}
