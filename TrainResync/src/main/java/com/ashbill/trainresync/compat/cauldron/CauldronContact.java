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
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int remainingContactSamples = 24;
    private static final String LAST_INSTANT = "shader_switch_debug:last_cauldron_instant";

    public static boolean apply(Level world, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)
                || !(world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile)) return false;
        var fluid = tile.getSoftFluidTank().getFluid();
        boolean milk = fluid.is(BuiltInSoftFluids.MILK);
        if (fluid.isEmpty() || (!milk && !fluid.is(BuiltInSoftFluids.POTION))) return false;
        if (world.isClientSide || !entity.mayInteract(world, pos)) return true;
        if (milk && !living.getActiveEffects().isEmpty() && remainingContactSamples > 0) {
            remainingContactSamples--;
            LOGGER.info("CAULDRON_EFFECT_CONTACT pos={} state={} feetY={} bounds={}",
                    pos, world.getBlockState(pos), entity.getY(), entity.getBoundingBox());
        }
        if (milk) {
            living.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            return true;
        }
        var data = living.getPersistentData();
        long now = world.getGameTime();
        boolean instantReady = !data.contains(LAST_INSTANT) || now - data.getLong(LAST_INSTANT) >= 10;
        boolean instantApplied = false;
        for (var effect : PotionUtils.getAllEffects(fluid.getTag())) {
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
                            && (effect.getDuration() == -1 || current.getDuration() < effect.getDuration())))
                    living.addEffect(new MobEffectInstance(effect));
            }
        }
        if (instantApplied) data.putLong(LAST_INSTANT, now);
        return true;
    }
}
