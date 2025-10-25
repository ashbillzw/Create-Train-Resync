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

import com.ashbill.trainresync.mixin_interfaces.IElectricTrain;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import net.minecraft.world.phys.Vec3;


import java.text.DecimalFormat;

@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

    @Shadow public int fuelTicks;

    @Shadow	public List<Carriage> carriages;

    @Unique private static void zeroRoll(Quaternionf rotation) {
        Vector3f euler = new Vector3f();
        rotation.getEulerAnglesYXZ(euler);
        rotation.rotationYXZ​(euler.y, euler.x, 0.0F);
    }

    @Unique
    private boolean trainresync$hasElectricity(Level level) {
        for (Carriage carriage : carriages){
            DimensionalCarriageEntity dce = carriage.getDimensional(level);

            for (BlockPos pantograph : ((IElectricTrain)carriage).trainresync$getPantographs()) {

                Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dce.rotationAnchors.get(false).subtract(dce.rotationAnchors.get(true)).toVector3f());
                zeroRoll(rotation);

                Vector3fc location = dce.positionAnchor.toVector3f().add(rotation.transform(Vec3.atLowerCornerOf(pantograph).toVector3f()));

                
                WireGraph wiregraph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);

                Integer count = 0;

                Vector3fc rightVector = rotation.transform(new Vector3f().set(PantographBlockEntity.BASE_RIGHT_VECTOR));
                for (BlockPos pos : PantographBlockEntityAccessor.trainresync$callFindIntersectingBlocks(
                    new Vector3d(location).sub(rightVector),
                    new Vector3d(location).add(rightVector),
                    rotation.transform(new Vector3d(PantographBlockEntity.BASE_UP_VECTOR))
                )) {
                    count += wiregraph.getCollisionsInBlock(pos).size();
                }

                
                // var df = new DecimalFormat("0.000");
                // var server = ServerLifecycleHooks.getCurrentServer();
                // if (server != null) {
                //     var msg = net.minecraft.network.chat.Component.literal("[TR] WIRE COUNT: " + count.toString());
                //     for (var p : server.getPlayerList().getPlayers())
                //         p.sendSystemMessage(msg);
                //     var msg = net.minecraft.network.chat.Component.literal("[TR] PANTO POS LOCA1: " + location.toString(df));
                //     for (var p : server.getPlayerList().getPlayers())
                //         p.sendSystemMessage(msg);
                //     var msg3 = net.minecraft.network.chat.Component.literal("[TR] PANTO POS LOCA2: " + location2.toString(df));
                //     for (var p : server.getPlayerList().getPlayers())
                //         p.sendSystemMessage(msg3);
                //     Vector3f tmp = new Vector3f();
                //     rotation.getEulerAnglesXYZ​(tmp);
                //     tmp.mul(57.295779513F);
                //     var msg2 = net.minecraft.network.chat.Component.literal("[TR] PANTO ROT EUT: " + tmp.toString(df));
                //     for (var p : server.getPlayerList().getPlayers())
                //         p.sendSystemMessage(msg2);
                // }

            }

            // var server = ServerLifecycleHooks.getCurrentServer();
            // if (server != null) {
            //     var msg = net.minecraft.network.chat.Component.literal("[TR] DETECTED positionAnchor: " + pos.toString());
            //     for (var p : server.getPlayerList().getPlayers())
            //         p.sendSystemMessage(msg);
            // }
        }
        return false;
    }

    @Inject(method = "earlyTick", at = @At("HEAD"))
    private void trainresync$electricityIsFuel(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (fuelTicks < 5 && trainresync$hasElectricity(level))
            fuelTicks = 25;
    }
}
