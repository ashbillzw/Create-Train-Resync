package com.ashbill.trainresync.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @ModifyArg(
        method = "handleContainerButtonClick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z"),
        index = 1
    )
    private int trainresync$restoreStonecutterRecipeIndex(Player player, int buttonId) {
        // The packet reads recipe indices 128-255 as signed bytes.
        return player.containerMenu instanceof StonecutterMenu && buttonId < 0 ? buttonId & 0xFF : buttonId;
    }
}
