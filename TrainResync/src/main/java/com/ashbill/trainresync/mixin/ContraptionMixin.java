package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

import com.ashbill.trainresync.tags.TRTags;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin {
    @Definition(id = "SeatBlock", type = SeatBlock.class)
    @Expression("? instanceof SeatBlock")
    @ModifyExpressionValue(
        method = "moveBlock",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean trainresync$seatOrFakeSeat(boolean original, @Local BlockState state) {
        return original || state.is(TRTags.FAKE_SEATS);
    }
}
