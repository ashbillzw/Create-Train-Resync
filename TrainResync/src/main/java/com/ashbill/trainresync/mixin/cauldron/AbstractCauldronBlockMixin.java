package com.ashbill.trainresync.mixin.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractCauldronBlock.class)
public interface AbstractCauldronBlockMixin {
    @Invoker("isEntityInsideContent")
    boolean trainresync$inside(BlockState state, BlockPos pos, Entity entity);
}
