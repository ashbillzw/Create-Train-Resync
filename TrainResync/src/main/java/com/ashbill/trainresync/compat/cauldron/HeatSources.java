package com.ashbill.trainresync.compat.cauldron;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.mehvahdjukaar.amendments.common.block.CommonCauldronCode;
import net.mehvahdjukaar.amendments.common.block.ModCauldronBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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

    // Placement precedes tank initialization. Read the final contents once all writes have completed.
    private static final Map<ServerLevel, Set<BlockPos>> pending = new WeakHashMap<>();

    public static void contentsChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && server.getServer().isSameThread())
            pending.computeIfAbsent(server, ignored -> new HashSet<>()).add(pos.immutable());
    }

    @SubscribeEvent
    public static void finishChanges(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world)) return;
        var positions = pending.remove(world);
        if (positions != null)
            for (var pos : positions)
                if (world.hasChunkAt(pos)) refresh(world, pos);
    }

    private static void refresh(ServerLevel world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if (!state.hasProperty(ModCauldronBlock.BOILING)) return;
        var below = pos.below();
        var updated = CommonCauldronCode.updateBoilingState(Direction.DOWN, world.getBlockState(below), world, below, state, pos);
        if (updated != state) world.setBlockAndUpdate(pos, updated);
    }

    // A local view for heat checks only; the world and tag registry are unchanged.
    public static BlockState view(BlockGetter world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return active(state, world, pos) ? state : Blocks.AIR.defaultBlockState();
    }
}
