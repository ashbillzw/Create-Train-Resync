package com.ashbill.trainresync.mixin.cauldron.supplementaries;

import com.ashbill.trainresync.compat.cauldron.CauldronContents;
import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.mehvahdjukaar.supplementaries.common.block.faucet.FaucetSource;
import net.mehvahdjukaar.supplementaries.common.block.faucet.FluidOffer;
import net.mehvahdjukaar.supplementaries.common.block.tiles.FaucetBlockTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@Mixin(value = FaucetBlockTile.class, remap = false)
public abstract class FaucetBlockTileMixin {
    @Redirect(method = "runInteractions", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/supplementaries/common/block/faucet/FaucetSource;getProvidedFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Ljava/lang/Object;)Lnet/mehvahdjukaar/supplementaries/common/block/faucet/FluidOffer;"))
    private <T> FluidOffer trainresync$wholePortions(FaucetSource<T> source, Level world, BlockPos pos, Direction side, T state) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank == null) return source.getProvidedFluid(world, pos, side, state);
        int portions = tank.trainresync$milkAmount() / 250;
        return portions == 0 ? null : FluidOffer.of(((net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile)tank).getSoftFluidTank().getFluid().copyWithCount(portions));
    }

    @Redirect(method = "runInteractions", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/supplementaries/common/block/faucet/FaucetSource;drain(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Ljava/lang/Object;I)V"))
    private <T> void trainresync$drainPortions(FaucetSource<T> source, Level world, BlockPos pos, Direction side, T state, int portions) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank == null) source.drain(world, pos, side, state, portions);
        else tank.trainresync$setMilkAmount(tank.trainresync$milkAmount() - portions * 250);
    }

    @Inject(method = "tryFillingBlockBelow", at = @At("HEAD"), cancellable = true)
    private void trainresync$fillPortions(FluidOffer offer, CallbackInfoReturnable<Integer> cir) {
        var self = (FaucetBlockTile)(Object)this;
        Level world = self.getLevel();
        BlockPos pos = self.getBlockPos().below();
        String type = CauldronContents.type(offer.fluid());
        if (!CauldronContents.liquid(type) && !CauldronContents.shake(type)) return;
        int accepted = MilkIntegration.fillContentAt(world, pos, type, offer.fluid().getCount() * 250, FluidAction.SIMULATE);
        if (accepted < 0) return;
        int portions = accepted / 250;
        if (portions < offer.minAmount()) { cir.setReturnValue(0); return; }
        MilkIntegration.fillContentAt(world, pos, type, portions * 250, FluidAction.EXECUTE);
        cir.setReturnValue(portions);
    }
}
