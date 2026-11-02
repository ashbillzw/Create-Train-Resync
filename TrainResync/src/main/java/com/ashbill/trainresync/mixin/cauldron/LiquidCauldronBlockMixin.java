package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;

@Mixin(value = LiquidCauldronBlock.class, remap = false)
public abstract class LiquidCauldronBlockMixin {
    @Inject(method = "handleEntityInsideFluidSpecial", at = @At("HEAD"), cancellable = true)
    private void trainresync$milkAndPotionContact(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (com.ashbill.trainresync.compat.cauldron.CauldronContact.apply(world, pos, entity)) ci.cancel();
    }

    @Redirect(method = "handleEntityInsideFluidSpecial", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_150930_(Lnet/minecraft/world/item/Item;)Z"))
    private boolean trainresync$glowIngredient(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item original) {
        return stack.is(com.ashbill.trainresync.compat.cauldron.CauldronGlow.INGREDIENTS);
    }

    @Redirect(method = "maybeExplode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_254877_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion trainresync$tntPotionExplosion(Level world, Entity source, DamageSource damage,
            ExplosionDamageCalculator calculator, double x, double y, double z, float power,
            boolean fire, Level.ExplosionInteraction interaction) {
        return world.explode(source, damage, calculator, x, y, z, 4.0F, true, Level.ExplosionInteraction.TNT);
    }

    @Inject(method = "m_141997_", at = @At("HEAD"), cancellable = true)
    private void trainresync$nextRainPortion(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
        if (precipitation == Biome.Precipitation.RAIN && !MilkIntegration.collectRain(world)) ci.cancel();
    }

    @ModifyArg(method = "m_142310_", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;fromFluid(Lnet/minecraft/world/level/material/Fluid;ILnet/minecraft/nbt/CompoundTag;)Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;"), index = 1)
    private int trainresync$quarterBucketDrip(int portions) { return 1; }

    // Native liquid tanks reject plain water. These two environmental inputs must
    // accept whole portions for unified water pots, retaining native change events.
    @Redirect(method = {"m_142310_", "m_141997_"}, at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidTank;addFluid(Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Z)I"))
    private int trainresync$collectWater(SoftFluidTank tank, SoftFluidStack input, boolean simulate) {
        if (!input.is(BuiltInSoftFluids.WATER)) return tank.addFluid(input, simulate);
        var stored = tank.getFluid();
        int portions = input.getCount();
        if ((!stored.isEmpty() && !stored.is(BuiltInSoftFluids.WATER)) || tank.getSpace() < portions) return 0;
        if (!simulate) tank.setFluid(SoftFluidStack.of(BuiltInSoftFluids.WATER, (stored.isEmpty() ? 0 : stored.getCount()) + portions));
        return portions;
    }

    @Inject(method = "handleEntityInsideFluidSpecial", at = @At("HEAD"))
    private void trainresync$lavaContact(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null && tank.trainresync$content().equals("lava") && tank.trainresync$milkAmount() > 0)
            Blocks.LAVA_CAULDRON.entityInside(Blocks.LAVA_CAULDRON.defaultBlockState(), world, pos, entity);
    }
}
