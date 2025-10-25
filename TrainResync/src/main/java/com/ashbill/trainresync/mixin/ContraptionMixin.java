package com.ashbill.trainresync.mixin;

import java.util.Set;
import java.util.HashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

import de.mrjulsen.paw.block.PantographBlock;

import com.ashbill.trainresync.tags.TRTags;
import com.ashbill.trainresync.mixin_interfaces.IElectricTrain;


import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.core.Direction;


@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin implements IElectricTrain {

    @Shadow public BlockPos anchor;

    @Unique private Set<BlockPos> trainresync$pantographs = new HashSet<BlockPos>();

    @Inject(
        method = "moveBlock",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            remap = true
        )
    )
    private void trainresync$findPantographs(CallbackInfoReturnable<Boolean> ci, @Local BlockState state, @Local BlockPos pos) {
        if (state.getBlock() instanceof PantographBlock) {
            trainresync$pantographs.add(pos.subtract(anchor));

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                var msg = net.minecraft.network.chat.Component.literal("[TR] DETECTED PANTO: " + pos.subtract(anchor).toString());
                for (var p : server.getPlayerList().getPlayers())
                    p.sendSystemMessage(msg);
                // var msg2 = net.minecraft.network.chat.Component.literal("[TR] DIRECTION: " + forcedDirection.toString());
                // for (var p : server.getPlayerList().getPlayers())
                //     p.sendSystemMessage(msg2);                
            }
        }
    }

    @Definition(id = "SeatBlock", type = SeatBlock.class)
    @Expression("? instanceof SeatBlock")
    @ModifyExpressionValue(
        method = "moveBlock",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean trainresync$seatOrFakeSeat(boolean original, @Local BlockState state) {
        return original || state.is(TRTags.FAKE_SEATS);
    }

    @Override
    public Set<BlockPos> trainresync$getPantographs() {
        return trainresync$pantographs;
    }
}
