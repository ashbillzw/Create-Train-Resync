package com.ashbill.trainresync.mixin.maid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;

import com.github.tartaricacid.touhoulittlemaid.event.maid.HandleBackpackEvent;

import com.ashbill.trainresync.mixin.maid.EntityMaidMixin;


@Mixin(value = HandleBackpackEvent.class, remap = false)
public abstract class HandleBackpackEventMixin {
    
    @ModifyExpressionValue(
        method = "onInteractMaid",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = true
        ),
        expect = 2
    )
    private boolean trainresync$allowAdminInstallMaidBackpack(boolean original, @Local Player playerIn) {
        return EntityMaidMixin.trainresync$adminIsOwner(original, playerIn);
    }
}
