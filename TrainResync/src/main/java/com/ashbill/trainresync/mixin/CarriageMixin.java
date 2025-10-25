package com.ashbill.trainresync.mixin;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import de.mrjulsen.paw.block.PantographBlock;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.ashbill.trainresync.mixin_interfaces.IElectricTrain;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageMixin implements IElectricTrain {
    
    @Unique private Set<BlockPos> trainresync$pantographs = null;

    @Inject(method = "setContraption", at = @At("HEAD"))
    private void trainresync$findPantographs(Level level, CarriageContraption contraption, CallbackInfo ci) {
        trainresync$pantographs = ((IElectricTrain)contraption).trainresync$getPantographs();
    }

    @Override
    public Set<BlockPos> trainresync$getPantographs() {

        // var server = ServerLifecycleHooks.getCurrentServer();
        // if (server != null) {
        //     var msg = net.minecraft.network.chat.Component.literal("[TR] GET PANTO CALLED: " + trainresync$pantographs.size());
        //     for (var p : server.getPlayerList().getPlayers())
        //         p.sendSystemMessage(msg);
        // }

        assert trainresync$pantographs != null;
        return trainresync$pantographs;
    }
}
