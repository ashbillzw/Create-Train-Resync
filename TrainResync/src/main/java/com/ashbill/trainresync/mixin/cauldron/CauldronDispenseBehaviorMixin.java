package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import dev.xkmc.fruitsdelight.content.cauldrons.CauldronDispenseBehavior;
import dev.xkmc.fruitsdelight.content.cauldrons.FDCauldronInteraction;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CauldronDispenseBehavior.class, remap = false)
public abstract class CauldronDispenseBehaviorMixin extends DefaultDispenseItemBehavior {
    @WrapOperation(method = "m_7498_", at = @At(value = "INVOKE", target = "Ldev/xkmc/fruitsdelight/content/cauldrons/FDCauldronInteraction;perform(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean trainresync$rememberCraftResult(FDCauldronInteraction interaction, BlockState state,
            Level world, BlockPos pos, ItemStack stack, Operation<Boolean> original,
            @Share("cauldronCrafted") LocalBooleanRef crafted) {
        boolean success = original.call(interaction, state, world, pos, stack);
        crafted.set(success);
        return success;
    }

    @Inject(method = "m_7498_", at = @At("RETURN"), cancellable = true)
    private void trainresync$fallbackWhenNotCrafted(BlockSource source, ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir, @Share("cauldronCrafted") LocalBooleanRef crafted) {
        // No matching interaction also leaves the per-call shared flag false.
        if (!crafted.get()) cir.setReturnValue(super.execute(source, stack));
    }

    @Redirect(method = "m_7498_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$fullWaterView(ServerLevel world, BlockPos pos) {
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null && tank.trainresync$content().equals("water") && tank.trainresync$milkAmount() == 1000)
            return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        return world.getBlockState(pos);
    }
}
