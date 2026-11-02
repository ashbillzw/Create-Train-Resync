package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.*;
import net.minecraft.nbt.CompoundTag;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact accounting over Amendments' visual four-layer tank. */
@Mixin(value = LiquidCauldronBlockTile.class, priority = 1100, remap = false)
public abstract class MilkTankMixin implements MilkAccess {
    @Unique private int trainresync$amount = -1;
    @Unique private int trainresync$limit = 1000;
    @Unique private String trainresync$type = "";
    @Unique private LiquidCauldronBlockTile trainresync$tile() { return (LiquidCauldronBlockTile)(Object)this; }
    @Unique private SoftFluidTank trainresync$tank() { return trainresync$tile().getSoftFluidTank(); }
    @Override public String trainresync$content() {
        if (!(trainresync$tile().getBlockState().getBlock() instanceof net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock)) return "";
        return CauldronContents.type(trainresync$tank().getFluid());
    }
    @Override public int trainresync$capacity() { return CauldronContents.shake(trainresync$content()) ? trainresync$limit : 1000; }
    @Override public int trainresync$milkAmount() {
        if (!(trainresync$tile().getBlockState().getBlock() instanceof net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock)) return -1;
        SoftFluidStack fluid = trainresync$tank().getFluid();
        String type = trainresync$content();
        if (!fluid.isEmpty() && type.isEmpty()) return -1;
        return trainresync$amount >= 0 && trainresync$type.equals(type) ? trainresync$amount : MilkAmounts.clamp(fluid.getCount() * 250);
    }
    @Override public void trainresync$setMilkAmount(int amount) {
        if (MappedFluids.managed(trainresync$content())) trainresync$setMappedFluid(trainresync$tank().getFluid(), amount);
        else trainresync$setContent(trainresync$content(), amount, trainresync$capacity());
    }
    @Override public void trainresync$setMappedFluid(SoftFluidStack fluid, int amount) {
        trainresync$type = CauldronContents.type(fluid);
        trainresync$limit = 1000;
        trainresync$amount = MilkAmounts.clamp(amount);
        trainresync$tank().setFluid(fluid.copyWithCount(MilkAmounts.displayLayers(trainresync$amount, trainresync$limit)));
        trainresync$tile().setChanged();
    }
    @Override public void trainresync$setContent(String type, int amount, int capacity) {
        trainresync$type = type;
        trainresync$limit = CauldronContents.shake(type) ? Math.max(250, Math.min(16000, capacity)) : 1000;
        trainresync$amount = Math.max(0, Math.min(trainresync$limit, amount));
        trainresync$tank().setFluid(CauldronContents.soft(type, MilkAmounts.displayLayers(trainresync$amount, trainresync$limit)));
        trainresync$tile().setChanged(); // Original empty -> ordinary cauldron behavior is retained.
    }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void trainresync$load(CompoundTag tag, CallbackInfo ci) {
        trainresync$type = tag.contains("sd_content") ? tag.getString("sd_content") : "milk";
        trainresync$limit = CauldronContents.shake(trainresync$type) ? Math.max(250, Math.min(16000, tag.getInt("sd_capacity"))) : 1000;
        trainresync$amount = tag.contains("sd_milk_mb") ? Math.max(0, Math.min(trainresync$limit, tag.getInt("sd_milk_mb"))) : -1;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void trainresync$save(CompoundTag tag, CallbackInfo ci) {
        if (trainresync$amount >= 0) {
            tag.putInt("sd_milk_mb", trainresync$amount);
            tag.putString("sd_content", trainresync$type);
            tag.putInt("sd_capacity", trainresync$limit);
        }
    }
    @Inject(method = "m_6596_", at = @At("HEAD"))
    private void trainresync$reconcileExternalLayers(CallbackInfo ci) {
        if (trainresync$amount < 0) return;
        SoftFluidStack fluid = trainresync$tank().getFluid();
        if (!fluid.isEmpty() && !trainresync$type.equals(trainresync$content())) { trainresync$amount = -1; return; }
        if (CauldronContents.shake(trainresync$type)) {
            // Milkshake containers are exclusively handled by our interaction bridge.
            // Native evaporation can remove visible layers; never create extra servings.
            int oldLayers = MilkAmounts.displayLayers(trainresync$amount, trainresync$limit);
            if (fluid.getCount() < oldLayers) trainresync$amount = Math.min(trainresync$amount, (fluid.getCount() * trainresync$limit / 1000) * 250);
        } else trainresync$amount = MilkAmounts.reconcileLayers(trainresync$amount, fluid.getCount());
    }
    @Inject(method = "getFluidInTank(I)Lnet/minecraftforge/fluids/FluidStack;", at = @At("HEAD"), cancellable = true)
    private void trainresync$contents(int index, CallbackInfoReturnable<FluidStack> cir) {
        if (trainresync$tile().isRemoved()) { cir.setReturnValue(FluidStack.EMPTY); return; }
        int stored = trainresync$milkAmount();
        if (stored >= 0) cir.setReturnValue(index == 0 ? MappedFluids.contents(this) : FluidStack.EMPTY);
    }
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void trainresync$fill(FluidStack input, FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (trainresync$tile().isRemoved()) { cir.setReturnValue(0); return; }
        int stored = trainresync$milkAmount();
        if (stored < 0) return;
        var mapped = MappedFluids.fromForge(input);
        if (!mapped.isEmpty() && MappedFluids.managed(CauldronContents.type(mapped))) {
            int accepted = stored == 0 || trainresync$tank().getFluid().isFluidEqual(mapped) ? MilkAmounts.fill(stored, input.getAmount()) : 0;
            if (accepted > 0 && action.execute()) trainresync$setMappedFluid(mapped, stored + accepted);
            cir.setReturnValue(accepted);
            return;
        }
        String type = CauldronContents.type(input);
        int accepted = !type.isEmpty() && (stored == 0 || type.equals(trainresync$content())) ? MilkAmounts.fill(stored, input.getAmount()) : 0;
        if (accepted > 0 && action.execute()) trainresync$setContent(type, stored + accepted, 1000);
        cir.setReturnValue(accepted);
    }
    @Inject(method = "drain(ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;", at = @At("HEAD"), cancellable = true)
    private void trainresync$drain(int requested, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        if (trainresync$tile().isRemoved()) { cir.setReturnValue(FluidStack.EMPTY); return; }
        int stored = trainresync$milkAmount();
        if (stored < 0) return;
        String type = trainresync$content();
        int taken = CauldronContents.liquid(type) || MappedFluids.managed(type) ? MilkAmounts.drain(stored, requested) : 0;
        var result = MappedFluids.contents(this);
        if (!result.isEmpty()) result.setAmount(taken);
        if (taken > 0 && action.execute()) trainresync$setMilkAmount(stored - taken);
        cir.setReturnValue(result);
    }
    @Inject(method = "drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;", at = @At("HEAD"), cancellable = true)
    private void trainresync$drainTyped(FluidStack requested, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        if (trainresync$tile().isRemoved()) { cir.setReturnValue(FluidStack.EMPTY); return; }
        int stored = trainresync$milkAmount();
        if (stored < 0) return;
        var result = MappedFluids.contents(this);
        int taken = result.isFluidEqual(requested) ? MilkAmounts.drain(stored, requested.getAmount()) : 0;
        if (!result.isEmpty()) result.setAmount(taken);
        if (taken > 0 && action.execute()) trainresync$setMilkAmount(stored - taken);
        cir.setReturnValue(result);
    }
    @Inject(method = "isFluidValid", at = @At("HEAD"), cancellable = true)
    private void trainresync$valid(int index, FluidStack input, CallbackInfoReturnable<Boolean> cir) {
        if (trainresync$tile().isRemoved()) { cir.setReturnValue(false); return; }
        int stored = trainresync$milkAmount();
        if (stored >= 0) {
            var mapped = MappedFluids.fromForge(input);
            if (!mapped.isEmpty() && MappedFluids.managed(CauldronContents.type(mapped))) {
                cir.setReturnValue(index == 0 && (stored == 0 || trainresync$tank().getFluid().isFluidEqual(mapped)));
                return;
            }
            String type = CauldronContents.type(input);
            cir.setReturnValue(index == 0 && !type.isEmpty() && (stored == 0 || type.equals(trainresync$content())));
        }
    }
}
