package com.ashbill.trainresync.mixin.create;

import com.simibubi.create.content.fluids.FluidNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkMixin {
    // Create #6658: break after the first matching drain in each simulation/execution pass.
    // The for-loop increments i once more before checking i < handler.getTanks().
    @ModifyVariable(method = "tick", name = "i", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/fluids/capability/IFluidHandler;drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"))
    private int trainresync$finishTankScan(int index) {
        return Integer.MAX_VALUE - 1;
    }
}
