package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Local views for audited callers; never changes the block in the world. */
public final class CauldronViews {
    public static BlockState heat(BlockGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof MilkAccess tank && tank.trainresync$content().equals("lava") && tank.trainresync$milkAmount() > 0)
            return Blocks.LAVA_CAULDRON.defaultBlockState();
        return HeatSources.view(world, pos);
    }
    public static BlockState tap(Level world, BlockPos pos) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null && tank.trainresync$milkAmount() > 0) {
            if (tank.trainresync$content().equals("water"))
                return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, tank.trainresync$milkAmount() == 1000 ? 3 : 1);
            if (tank.trainresync$content().equals("lava") && tank.trainresync$milkAmount() == 1000)
                return Blocks.LAVA_CAULDRON.defaultBlockState();
        }
        return world.getBlockState(pos);
    }
}
