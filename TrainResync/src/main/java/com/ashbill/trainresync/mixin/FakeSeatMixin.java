package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.content.contraptions.Contraption;

import com.ashbill.trainresync.tags.TRTags;

@Mixin(Contraption.class)
public abstract class ContraptionSeatMixin {
    @ModifyExpressionValue(
        method = "moveBlock",
        at = @At(
            value = "INSTANCEOF",
            target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/blocks/SeatBlock;"
        )
    )
    private boolean trainresync$seatOrFakeSeat(boolean original, @Local BlockState state) {
        return original || TRTags.FAKE_SEATS.matches(state);
    }
}
