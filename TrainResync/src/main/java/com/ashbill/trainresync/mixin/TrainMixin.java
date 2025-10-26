package com.ashbill.trainresync.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.Level;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainStatus;

import com.ashbill.trainresync.mixin_interfaces.IElectricTrainCarriage;


@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

    @Shadow public int fuelTicks;

    @Shadow public TrainStatus status;

    @Unique private boolean trainresync$isFirstTick = true;

    @Shadow	public List<Carriage> carriages;

    @Inject(method = "earlyTick", at = @At("HEAD"))
    private void trainresync$electricityIsFuel(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;

        if (trainresync$isFirstTick) {
            trainresync$isFirstTick = false;
            int count = 0;
            for (Carriage carriage : carriages) count += ((IElectricTrainCarriage)carriage).trainresync$getPantographCount();
            if (count > 0) status.displayInformation("has_pantograph", true, count);
        }

        if (fuelTicks < 5 && trainresync$hasElectricity(level))
            fuelTicks = 25;
    }

    @Unique private boolean trainresync$hasElectricity(Level level) {
        for (Carriage carriage : carriages)
            if (((IElectricTrainCarriage)carriage).trainresync$hasElectricity(level)) return true;
        return false;
    }
}
