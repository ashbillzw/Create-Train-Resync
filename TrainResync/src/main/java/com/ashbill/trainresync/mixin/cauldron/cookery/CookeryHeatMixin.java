package com.ashbill.trainresync.mixin.cauldron.cookery;

import com.ashbill.trainresync.compat.cauldron.HeatSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {PotBlockEntity.class, SteamerBlockEntity.class, StockpotBlockEntity.class, TeapotBlockEntity.class}, remap = false)
public abstract class CookeryHeatMixin {
    @Redirect(method = "hasHeatSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$activeHeat(Level world, BlockPos pos) {
        return HeatSources.view(world, pos);
    }
}
