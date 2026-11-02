package com.ashbill.trainresync.mixin.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronContactHeightMixin {
    @Inject(method = "isEntityInsideContent", at = @At("HEAD"), cancellable = true)
    private void trainresync$limitContactHeight(BlockState state, BlockPos pos, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemEntity) && entity.getBoundingBox().minY >= pos.getY() + 0.5)
            cir.setReturnValue(false);
    }
}
