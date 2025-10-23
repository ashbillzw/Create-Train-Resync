package com.ashbill.trainresync.mixin;

import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;

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

// import de.mrjulsen.wires.WireNetwork;
import net.minecraftforge.server.ServerLifecycleHooks;

import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;

import com.ashbill.trainresync.mixin.accessor.PantographBlockEntityAccessor;


@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

    @Shadow public int fuelTicks;

    @Shadow	public List<Carriage> carriages;

    @Unique
    private boolean trainresync$hasElectricity(Level level) {
        for (Carriage carriage : carriages){
            var pos = carriage.getDimensional(level).positionAnchor;


            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                var msg = net.minecraft.network.chat.Component.literal("[TR] DETECTED positionAnchor: " + pos.toString());
                for (var p : server.getPlayerList().getPlayers())
                    p.sendSystemMessage(msg);
            }
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
