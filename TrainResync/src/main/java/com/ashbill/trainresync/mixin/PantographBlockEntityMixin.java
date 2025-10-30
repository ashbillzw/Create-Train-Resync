package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import de.mrjulsen.paw.blockentity.PantographBlockEntity;


@Mixin(value = PantographBlockEntity.class, remap = false)
public abstract class PantographBlockEntityMixin extends SmartBlockEntity {
    @Shadow private double catenaryWireHeight;
    @Unique private double trainresync$prevCatenaryWireHeight = 0.0D;
    @Unique private int trainresync$retractionBufferTicks = 0;

    public PantographBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @ModifyArg(
        method = "updateContraptionValues",
        at = @At(
            value = "INVOKE",
            target = "Lde/mrjulsen/paw/blockentity/PantographBlockEntity;setExpanded(Z)V"
        )
    )
    private boolean trainresync$delayRetraction(boolean original) {
        if (original) {
            trainresync$retractionBufferTicks = 5;
            trainresync$prevCatenaryWireHeight = catenaryWireHeight;
            return true;
        }
        else if (trainresync$retractionBufferTicks > 0) {
            trainresync$retractionBufferTicks--;
            catenaryWireHeight = trainresync$prevCatenaryWireHeight;
            return true;
        }
        return false;
    }
}
