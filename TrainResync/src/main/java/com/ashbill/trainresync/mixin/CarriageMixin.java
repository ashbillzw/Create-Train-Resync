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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import com.ashbill.trainresync.mixin_interfaces.IElectricTrainCarriage;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import de.mrjulsen.paw.block.PantographBlock;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;


@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageMixin implements IElectricTrainCarriage {
    
    @Unique private Set<BlockPos> trainresync$pantographs = null;

    @Shadow public abstract void forEachPresentEntity(Consumer<CarriageContraptionEntity> callback);

    @Unique
    private void trainresync$findPantographs() {
        trainresync$pantographs = new HashSet<BlockPos>();
        forEachPresentEntity(cce -> {
            for (Map.Entry<BlockPos, StructureBlockInfo> entry : cce.getContraption().getBlocks().entrySet()) {
                if (entry.getValue().state().getBlock() instanceof PantographBlock) {
                    trainresync$pantographs.add(entry.getKey());


                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        var msg = net.minecraft.network.chat.Component.literal("[TR] DETECTED PANTO: " + entry.getKey().toString());
                        for (var p : server.getPlayerList().getPlayers())
                            p.sendSystemMessage(msg);
                    }
                }
                    

            }
        });
    }

    @Override
    public Set<BlockPos> trainresync$getPantographs() {
        if (trainresync$pantographs == null) trainresync$findPantographs();
        return trainresync$pantographs;
    }
}
