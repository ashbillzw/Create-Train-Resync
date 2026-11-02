package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LayeredCauldronBlock.class, remap = false)
public abstract class LayeredCauldronBlockMixin {
    @Inject(method = "m_153559_", at = @At("HEAD"), cancellable = true)
    private static void trainresync$consumeWater(BlockState state, Level world, BlockPos pos, CallbackInfo ci) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank == null || !tank.trainresync$content().equals("water")) return;
        if (!world.isClientSide && tank.trainresync$milkAmount() >= 250) {
            tank.trainresync$setMilkAmount(tank.trainresync$milkAmount() - 250);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(world.getBlockState(pos)));
        }
        ci.cancel();
    }
}
