package com.ashbill.trainresync;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod("trainresync")
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public class TrainResyncMod {
    private static ResourceKey<Level> lastDimension = null;
    private static int ticksUntilSync = -1;

    public TrainResyncMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("shuaxin")
                .executes(ctx -> resyncCarriages())
        );
        event.getDispatcher().register(
            Commands.literal("sx")
                .executes(ctx -> resyncCarriages())
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        
        ResourceKey<Level> currentDimension = mc.level.dimension();

        if (lastDimension == Level.NETHER && currentDimension == Level.OVERWORLD) {
            ticksUntilSync = 60;
            mc.player.sendSystemMessage(Component.literal("[AshBill TrainResync] 3秒后将刷新列车状态"));
        }

        if (ticksUntilSync > 0) {
            ticksUntilSync--;
            if (ticksUntilSync == 0) resyncCarriages();
        }

        lastDimension = currentDimension;
    }

    private static int resyncCarriages() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null)
            return 0;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof CarriageContraptionEntity) {
                e.setPos(player.getX(), player.getY() - 512, player.getZ());
            }
        }

        player.sendSystemMessage(Component.literal("[AshBill TrainResync] 已刷新列车状态"));
        return 1;
    }
}
