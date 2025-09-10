package com.ashbill.trainresync;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;


@Mod("trainresync")
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public class TrainResyncMod {

    private static ResourceKey<Level> lastDimension = null;
    private static int syncCooldown = 0;
    private static final Map<Entity, Integer> checkQueue = new HashMap<>();
    private static int messageCooldown = -1; // -1 Initial Message, -2 Silent, -3 Verbose

    private static boolean isDebugMode = false;

    public TrainResyncMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("shuaxin").executes(
            ctx -> resyncAllCarriages()
        ));
        event.getDispatcher().register(Commands.literal("xuebaobizui").executes(
            ctx -> silentMode()
        ));
        event.getDispatcher().register(Commands.literal("verbose").executes(
            ctx -> verboseMode()
        ));
        event.getDispatcher().register(Commands.literal("trainresyncdebug").executes(
            ctx -> debugMode()
        ));
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {

        if (event.getLevel().dimension() == Level.OVERWORLD) checkQueue.put(event.getEntity(), 5);

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        try{

            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            ResourceKey<Level> currentDimension = mc.level.dimension();
            if (currentDimension == Level.OVERWORLD && lastDimension == Level.NETHER) {
                if (syncCooldown == 0) showMessage("[AshBill TrainResync] 即将自动刷新");
                if (syncCooldown < 60) syncCooldown = 60;
            }
            lastDimension = currentDimension;

            if (syncCooldown > 0) {
                syncCooldown--;
                if (syncCooldown == 0 && currentDimension == Level.OVERWORLD) resyncAllCarriages(); 
            }

            Iterator<Map.Entry<Entity, Integer>> iter = checkQueue.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Entity, Integer> entry = iter.next();
                Entity e = entry.getKey();
                int t = entry.getValue();

                if (t > 1) {
                    entry.setValue(t - 1);
                }
                else {
                    if (
                        e != null && currentDimension == Level.OVERWORLD && (
                            e.getVehicle() instanceof CarriageContraptionEntity
                            || e instanceof CarriageContraptionEntity cce
                                && cce.getContraption() instanceof CarriageContraption cc
                                && !cc.notInPortal()
                        )
                    ){
                        if (syncCooldown == 0) showMessage("[AshBill TrainResync] 即将自动刷新");
                        if (syncCooldown < 40) syncCooldown = 40;
                    }
                    iter.remove();
                }
            }

            if (messageCooldown > 0) messageCooldown--;

            if (isDebugMode){
                showMessage(
                    "sync: "    + syncCooldown
                +   ", len(cQ): "   + checkQueue.size()
                +   ", msg: "   + messageCooldown, -1
                );
            }

        } catch (Exception t) {
            showMessage("[AshBill TrainResync] 刷新失败（" + t.getClass().getSimpleName() + "）", -1);
        }

    }

    public static int resyncAllCarriages() {

        try {

            Minecraft mc = Minecraft.getInstance();        
            if (mc.level == null || mc.player == null) return -1;

            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof CarriageContraptionEntity) {
                    e.setPos(mc.player.getX(), mc.player.getY() - 512, mc.player.getZ());
                }
            }

            showMessage("[AshBill TrainResync] 已刷新列车状态", 6000);
            return 0;

        } catch (Exception t) {
            showMessage("[AshBill TrainResync] 刷新失败（" + t.getClass().getSimpleName() + "）", -1);
            return -1;
        }

    }

    public static int silentMode() {
        messageCooldown = -2;
        showMessage("[AshBill TrainResync] 提示已关闭", -1);
        return 0;
    }

    public static int verboseMode() {
        messageCooldown = -3;
        return 0;
    }

    public static int debugMode() {
        isDebugMode = true;
        return 0;
    }

    public static void showMessage(String msg) {
        showMessage(msg, 0);
    }

    public static void showMessage(String msg, int cooldown) {

        if (messageCooldown == -1) {
            msg += "，关闭此提示：/xuebaobizui";
            messageCooldown = 0;
        }

        if (messageCooldown == -2 && cooldown != -1) return;

        if (messageCooldown < 12000 || cooldown == -1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg));
            if (messageCooldown != -3 && cooldown != -1) messageCooldown += cooldown;
        }

    }
}
