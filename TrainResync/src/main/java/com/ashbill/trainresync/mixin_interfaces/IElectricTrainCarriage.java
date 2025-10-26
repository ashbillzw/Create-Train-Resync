package com.ashbill.trainresync.mixin_interfaces;

import java.util.Set;

import org.joml.Quaternionf;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;


public interface IElectricTrainCarriage {
    public boolean trainresync$hasElectricity(Level level);
    public void trainresync$setInitialRotation(Quaternionf initialRotation);
    public void trainresync$setPantographs(Set<BlockPos> pantographs);
}
