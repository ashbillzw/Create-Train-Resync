package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronGlow;
import net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = LiquidCauldronBlock.class, remap = false)
public abstract class LiquidCauldronParticlesMixin {
    @Redirect(method = "m_214162_", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"), allow = 1)
    private int trainresync$countEffectsForSmoke(List<MobEffectInstance> effects) {
        return CauldronGlow.mixingCount(effects);
    }
}
