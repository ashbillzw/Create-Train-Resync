package com.ashbill.trainresync;

import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("trainresync")
public class TrainResyncMod {

    public TrainResyncMod() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(this::onClientCommandRegister);
    }

    private void onClientCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("shuaxin").executes(TrainResyncMod::resyncCarriages)
        );
        event.getDispatcher().register(
            Commands.literal("sx").executes(TrainResyncMod::resyncCarriages)
        );
    }

    private static int resyncCarriages(CommandContext<CommandSourceStack> context) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null)
            return 0;

        int moved = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof CarriageContraptionEntity) {
                e.setPos(player.getX(), player.getY(), player.getZ());
                moved++;
            }
        }

        player.sendSystemMessage(Component.literal("[AshBill TrainResync] 已刷新列车状态"));
        return 1;
    }
}
