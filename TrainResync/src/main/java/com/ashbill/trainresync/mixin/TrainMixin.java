package com.ashbill.trainresync.mixin;

import java.rmi.dgc.VMID;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;
import org.joml.Math;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;

// import de.mrjulsen.wires.WireNetwork;
import net.minecraftforge.server.ServerLifecycleHooks;

import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.WiresApi;

import com.ashbill.trainresync.mixin.accessor.PantographBlockEntityAccessor;

import com.ashbill.trainresync.mixin_interfaces.IElectricTrainCarriage;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import net.minecraft.world.phys.Vec3;

import com.ashbill.trainresync.DebugPrint;


@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

    @Shadow public int fuelTicks;

    @Shadow	public List<Carriage> carriages;

    @Inject(method = "earlyTick", at = @At("HEAD"))
    private void trainresync$electricityIsFuel(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        boolean HASELEC = trainresync$hasElectricity(level);
        DebugPrint.debugPrint("[TR] HASELEC: " + HASELEC);
        if (fuelTicks < 5 && HASELEC)
            fuelTicks = 25;
    }

    @Unique private boolean trainresync$hasElectricity(Level level) {
        for (Carriage carriage : carriages)
            if (((IElectricTrainCarriage)carriage).trainresync$hasElectricity(level)) return true;
        return false;
    }
}
