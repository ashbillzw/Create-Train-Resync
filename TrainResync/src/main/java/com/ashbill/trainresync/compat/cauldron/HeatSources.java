package com.ashbill.trainresync.compat.cauldron;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
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
    public static final TagKey<Block> FROM_BOILER = TagKey.create(Registries.BLOCK,
            new ResourceLocation("trainresync", "cauldron_boiler_heaters"));

    public static void setupBoilerHeaters(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BoilerHeaters.Heater passive = (world, pos, state) -> active(state, world, pos) ? 0 : -1;
            for (String id : new String[]{"farmersdelight:stove", "kaleidoscope_cookery:stove",
                    "supplementaries:fire_pit", "supplementaries:lumisene", "supplementaries:blaze_rod",
                    "create_central_kitchen:blaze_stove"}) {
                var key = new ResourceLocation(id);
                if (ForgeRegistries.BLOCKS.containsKey(key)) BoilerHeaters.registerHeater(key, passive);
            }
            BoilerHeaters.registerHeater(Blocks.LAVA_CAULDRON, passive);
            BoilerHeaters.registerHeater(new ResourceLocation("amendments", "liquid_cauldron"),
                    (world, pos, state) -> CauldronViews.containsLava(world, pos) ? 0 : -1);
        });
    }

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
            for (var pos : positions) {
                if (!world.hasChunkAt(pos)) continue;
                refresh(world, pos);
                // The source can change fluid without changing its visible layer.
                var above = pos.above();
                refresh(world, above);
                if (world.getBlockEntity(above) instanceof FluidTankBlockEntity tank)
                    tank.updateBoilerTemperature();
            }
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
