package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronDispenseBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CauldronDispenseBehavior.class, remap = false)
public abstract class CauldronDispenseBehaviorMixin {
    @Redirect(method = "m_7498_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$fullWaterView(ServerLevel world, BlockPos pos) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null && tank.trainresync$content().equals("water") && tank.trainresync$milkAmount() == 1000)
            return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        return world.getBlockState(pos);
    }
}
