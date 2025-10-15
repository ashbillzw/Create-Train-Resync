package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.content.contraptions.Contraption;

import com.ashbill.trainresync.tags.TRTags;

@Mixin(Contraption.class)
public abstract class FakeSeatMixin {
    @ModifyExpressionValue(
        method = "moveBlock",
        at = @At(
            value = "INSTANCEOF",
            target = "Lcom.simibubi.create.content.contraptions.actors.seat.SeatBlock;"
        )
    )
    private boolean trainresync$seatOrFakeSeat(boolean original, @Local BlockState state) {
        System.out.println("[TrainResync] FakeSeatMixin ran!");
        return original || state.is(TRTags.FAKE_SEATS);
    }
}
