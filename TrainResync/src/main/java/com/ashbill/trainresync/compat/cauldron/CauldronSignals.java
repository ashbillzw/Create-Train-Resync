package com.ashbill.trainresync.compat.cauldron;

import java.util.Collections;
import dev.xkmc.fruitsdelight.content.cauldrons.FruitCauldronBlock;
import dev.xkmc.fruitsdelight.content.cauldrons.JellyCauldronBlock;
import net.hibiscus.naturespirit.blocks.CheeseCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.mehvahdjukaar.amendments.common.block.ModCauldronBlock;

public final class CauldronSignals {
    public static int signal(BlockState state, Level world, BlockPos pos) {
        if (state.is(Blocks.CAULDRON)) return 0;
        if (state.getBlock() instanceof CheeseCauldronBlock) return 0;
        if (state.getBlock() instanceof FruitCauldronBlock)
            return 16 - state.getValue(FruitCauldronBlock.LEVEL);
        if (state.getBlock() instanceof JellyCauldronBlock)
            return state.getValue(JellyCauldronBlock.LEVEL);
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null) return MilkAmounts.comparatorSignal(tank.trainresync$milkAmount(), tank.trainresync$capacity());
        if (state.getBlock() instanceof ModCauldronBlock pot) {
            var level = pot.getLevelProperty();
            return MilkAmounts.comparatorSignal(state.getValue(level), Collections.max(level.getPossibleValues()));
        }
        for (var property : state.getValues().keySet()) {
            if (property instanceof IntegerProperty level && level.getName().equals("level"))
                return MilkAmounts.comparatorSignal(state.getValue(level), Collections.max(level.getPossibleValues()));
        }
        // Pots without a level property are full; mature cheese is handled above.
        return 15;
    }
}
