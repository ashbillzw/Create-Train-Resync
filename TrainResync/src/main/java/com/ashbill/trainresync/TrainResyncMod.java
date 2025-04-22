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

// Testing
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.contraptions.Contraption;

@Mod("trainresync")
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public class TrainResyncMod {
    private static final Map<CarriageContraptionEntity, Integer> carrageSyncQueue = new HashMap<>();
    private static int messageCooldown = -1;

    public TrainResyncMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("shuaxin")
                .executes(ctx -> resyncAllCarriages())
        );
        event.getDispatcher().register(
            Commands.literal("sx")
                .executes(ctx -> resyncAllCarriages())
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        Iterator<Map.Entry<CarriageContraptionEntity, Integer>> iter = carrageSyncQueue.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<CarriageContraptionEntity, Integer> entry = iter.next();
            CarriageContraptionEntity carriage = entry.getKey();
            int t = entry.getValue();

            if (t < 1) {
                resyncCarriage(carriage);
                iter.remove();
            } else {
                entry.setValue(t - 1);
            }
        }

        
        // ResourceKey<Level> currentDimension = mc.level.dimension();

        // if (currentDimension != Level.OVERWORLD) {
        //     return;
        // }

        // if (ticksUntilSync > 0) {
        //     ticksUntilSync--;
        //     if (ticksUntilSync == 0) resyncOneCarriage();
        // }

        // lastDimension = currentDimension;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        Entity e = event.getEntity();

        if (e instanceof CarriageContraptionEntity){
            carrageSyncQueue.put( (CarriageContraptionEntity) e, 60);
        }

    }

    private static void resyncCarriage(CarriageContraptionEntity carriage) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        if (carriage != null) {
            carriage.setPos(mc.player.getX(), mc.player.getY() - 512, mc.player.getZ());
            
            Contraption c = carriage.getContraption();
            CarriageContraption cc = (CarriageContraption) c;

            if (cc.notInPortal()) sendMessage("SYNC IN 3 - A");
            else sendMessage("SYNC IN 3 - B");
        }
        else mc.player.sendSystemMessage(Component.literal("DEBUG 1")); //DEBUG

        return;

    }


    private static int resyncAllCarriages() {

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

    private static void sendMessage(String msg) {

        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg));

    }
}
