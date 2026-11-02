package com.ashbill.trainresync.mixin.cauldron;

import net.hibiscus.naturespirit.blocks.MilkCauldronBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MilkCauldronBlock.class, remap = false)
public abstract class MilkCauldronBlockMixin {
    @ModifyArg(method = "m_6227_", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private int trainresync$notifyOnStarter(int flags) {
        return flags | Block.UPDATE_NEIGHBORS;
    }

    @ModifyArg(method = "m_213898_", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private int trainresync$notifyOnRipening(int flags) {
        return flags | Block.UPDATE_NEIGHBORS;
    }
}
