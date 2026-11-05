package com.ashbill.trainresync.mixin.cauldron;

import net.mehvahdjukaar.amendments.common.block.DyeCauldronBlock;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DyeCauldronBlock.class, remap = false)
public abstract class DyeCauldronBlockMixin {
    // A dropped ingredient reuses native sounds; its caller owns consumption.
    @Redirect(method = "playDyeSoundAndConsume", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_7500_()Z"))
    private static boolean trainresync$callerConsumes(Player player) {
        return player == null || player.isCreative();
    }
}
