package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.HeatSources;
import net.mehvahdjukaar.amendments.common.block.ModCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CauldronHeatPlacementMixin {
    @Inject(method = "onPlace", at = @At("RETURN"))
    private void trainresync$refreshNewPot(Level world, BlockPos pos, BlockState old, boolean moving, CallbackInfo ci) {
        var state = (BlockState)(Object)this;
        if (old.getBlock() != state.getBlock() && state.hasProperty(ModCauldronBlock.BOILING))
            HeatSources.contentsChanged(world, pos);
    }
}
