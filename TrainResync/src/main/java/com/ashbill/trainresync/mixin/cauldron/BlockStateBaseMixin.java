package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronSignals;
import com.ashbill.trainresync.compat.cauldron.CauldronDrops;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBehaviour.BlockStateBase.class, remap = false)
public abstract class BlockStateBaseMixin {
    @Inject(method = "m_60682_", at = @At("HEAD"), cancellable = true)
    private void trainresync$dropMaterial(Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof ItemEntity item && CauldronDrops.accept((BlockState)(Object)this, world, pos, item)) ci.cancel();
    }
    @Inject(method = "m_60807_", at = @At("HEAD"), cancellable = true)
    private void trainresync$cauldronComparator(CallbackInfoReturnable<Boolean> cir) {
        if (((BlockState)(Object)this).getBlock() instanceof AbstractCauldronBlock) cir.setReturnValue(true);
    }

    @Inject(method = "m_60674_", at = @At("HEAD"), cancellable = true)
    private void trainresync$cauldronSignal(Level world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        var state = (BlockState)(Object)this;
        if (state.getBlock() instanceof AbstractCauldronBlock)
            cir.setReturnValue(CauldronSignals.signal(state, world, pos));
    }
}
