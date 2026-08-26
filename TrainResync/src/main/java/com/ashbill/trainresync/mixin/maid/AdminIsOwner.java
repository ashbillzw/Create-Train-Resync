package com.ashbill.trainresync.mixin.maid;

import net.minecraft.world.entity.player.Player;


public class AdminIsOwner {
    public static boolean trainresync$adminIsOwner(boolean original, Player playerIn) {
        return original || playerIn.hasPermissions(2);
    }
}
