package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronViews;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vectorwing.farmersdelight.common.block.entity.HeatableBlockEntity;
import vectorwing.farmersdelight.common.tag.ModTags;

@Mixin(value = HeatableBlockEntity.class, remap = false)
public interface HeatableBlockEntityMixin {
    @Shadow boolean requiresDirectHeat();

    /**
     * @author chatgpt-codex-astra
     * @reason Mixin 0.8.5 forbids interface injectors. Preserve FD 1.3.4's checks,
     * changing only its two block-state reads to recognize unified lava cauldrons.
     */
    @Overwrite
    default boolean isHeated(Level world, BlockPos pos) {
        var below = CauldronViews.heat(world, pos.below());
        if (below.is(ModTags.Blocks.HEAT_SOURCES)) {
            if (below.hasProperty(BlockStateProperties.LIT))
                return below.getValue(BlockStateProperties.LIT);
            return true;
        }
        if (!requiresDirectHeat() && below.is(ModTags.Blocks.HEAT_CONDUCTORS)) {
            var heat = CauldronViews.heat(world, pos.below(2));
            if (heat.is(ModTags.Blocks.HEAT_SOURCES)) {
                if (heat.hasProperty(BlockStateProperties.LIT))
                    return heat.getValue(BlockStateProperties.LIT);
                return true;
            }
        }
        return false;
    }
}
