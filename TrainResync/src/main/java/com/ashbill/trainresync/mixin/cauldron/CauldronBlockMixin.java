package com.ashbill.trainresync.mixin.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CauldronBlock.class, priority = 1100, remap = false)
public abstract class CauldronBlockMixin {
    @Shadow(remap = true) protected static boolean shouldHandlePrecipitation(Level world, Biome.Precipitation precipitation) { throw new AssertionError(); }

    @Redirect(method = "m_141997_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CauldronBlock;m_182450_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome$Precipitation;)Z"))
    private boolean trainresync$firstRainPortion(Level world, Biome.Precipitation precipitation) {
        return precipitation == Biome.Precipitation.RAIN ? MilkIntegration.collectRain(world) : shouldHandlePrecipitation(world, precipitation);
    }

    @Redirect(method = "m_141997_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean trainresync$rainIntoUnifiedCauldron(Level world, BlockPos pos, BlockState state) {
        if (state.is(Blocks.WATER_CAULDRON))
            return MilkIntegration.fillContentAt(world, pos, "water", 250, FluidAction.EXECUTE) == 250;
        return world.setBlockAndUpdate(pos, state);
    }
    @Inject(method = "m_142310_", at = @At("HEAD"), cancellable = true)
    private void trainresync$dripIntoUnifiedCauldron(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (fluid != Fluids.WATER && fluid != Fluids.LAVA && fluid != ForgeMod.MILK.get()) return;
        LiquidCauldronBlock cauldron = (LiquidCauldronBlock) ModRegistry.LIQUID_CAULDRON.get();
        BlockState filled = cauldron.defaultBlockState();
        level.setBlockAndUpdate(pos, filled);
        cauldron.receiveStalactiteDrip(filled, level, pos, fluid);
        ci.cancel();
    }
}
