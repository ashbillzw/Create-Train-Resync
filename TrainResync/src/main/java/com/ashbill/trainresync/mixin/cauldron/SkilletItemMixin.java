package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronViews;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.item.SkilletItem;

@Mixin(value = SkilletItem.class, remap = false)
public abstract class SkilletItemMixin {
    @Redirect(method = "isPlayerNearHeatSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState trainresync$heatSourceView(LevelReader world, BlockPos pos) { return CauldronViews.heat(world, pos); }
}
