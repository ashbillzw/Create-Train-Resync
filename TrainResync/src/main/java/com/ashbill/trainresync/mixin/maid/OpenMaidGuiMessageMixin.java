package com.ashbill.trainresync.mixin.maid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;

import com.github.tartaricacid.touhoulittlemaid.network.message.OpenMaidGuiMessage;


@Mixin(value = OpenMaidGuiMessage.class, remap = false)
public abstract class OpenMaidGuiMessageMixin {
    
    @ModifyExpressionValue(
        method = "stillValid",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = true
        )
    )
    private static boolean trainresync$allowAdminAccessMaidGui(boolean original, @Local Player playerIn) {
        return AdminIsOwner.trainresync$adminIsOwner(original, playerIn);
    }
}
