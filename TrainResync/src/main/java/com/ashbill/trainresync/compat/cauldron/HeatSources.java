package com.ashbill.trainresync.compat.cauldron;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.mehvahdjukaar.moonlight.api.block.ILightable;

public final class HeatSources {
    public static boolean active(BlockState state, BlockGetter world, BlockPos pos) {
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return false;
        if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                && state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.NONE) return false;
        return !(state.getBlock() instanceof ILightable light) || light.isLitUp(state, world, pos);
    }

    // A local view for heat checks only; the world and tag registry are unchanged.
    public static BlockState view(BlockGetter world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return active(state, world, pos) ? state : Blocks.AIR.defaultBlockState();
    }
}
