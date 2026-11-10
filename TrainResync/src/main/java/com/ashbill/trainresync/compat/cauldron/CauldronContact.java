package com.ashbill.trainresync.compat.cauldron;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

public final class CauldronContact {
    private static final String LAST_INSTANT = "shader_switch_debug:last_cauldron_instant";

    public static boolean apply(Level world, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)
                || !(world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile)) return false;
        var fluid = tile.getSoftFluidTank().getFluid();
        boolean milk = fluid.is(BuiltInSoftFluids.MILK);
        boolean glowingWater = fluid.is(BuiltInSoftFluids.WATER) && tile.isGlowing();
        if (fluid.isEmpty() || (!milk && !glowingWater && !fluid.is(BuiltInSoftFluids.POTION))) return false;
        if (world.isClientSide || !entity.mayInteract(world, pos)) return true;
        if (milk) {
            living.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            return true;
        }
        var data = living.getPersistentData();
        long now = world.getGameTime();
        boolean instantReady = !data.contains(LAST_INSTANT) || now - data.getLong(LAST_INSTANT) >= 10;
        boolean instantApplied = false;
        var effects = PotionUtils.getAllEffects(fluid.getTag());
        if (tile.isGlowing()) effects = CauldronGlow.withGlow(effects);
        for (var effect : effects) {
            var type = effect.getEffect();
            if (type.isInstantenous()) {
                if (instantReady) {
                    type.applyInstantenousEffect(null, null, living, effect.getAmplifier(), 1.0);
                    instantApplied = true;
                }
            } else {
                var current = living.getEffect(type);
                if (current == null || current.getAmplifier() < effect.getAmplifier()
                        || (current.getAmplifier() == effect.getAmplifier()
                            && current.getDuration() != -1
                            && (effect.getDuration() == -1 || effect.getDuration() - current.getDuration() >= 10)))
                    living.addEffect(new MobEffectInstance(effect));
            }
        }
        if (instantApplied) data.putLong(LAST_INSTANT, now);
        return true;
    }
}
