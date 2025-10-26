package com.ashbill.trainresync;

import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;


public class DebugPrint {
    public static void debugPrint(String string) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            var msg = Component.literal(string);
            for (var player : server.getPlayerList().getPlayers())
                player.sendSystemMessage(msg);
        }
    }
}
