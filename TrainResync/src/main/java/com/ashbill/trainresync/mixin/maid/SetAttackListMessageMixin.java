package com.ashbill.trainresync.mixin.maid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;

import com.github.tartaricacid.touhoulittlemaid.network.message.SetAttackListMessage;

import com.ashbill.trainresync.mixin.maid.EntityMaidMixin;


@Mixin(value = SetAttackListMessage.class, remap = false)
public abstract class SetAttackListMessageMixin {
    
    @ModifyExpressionValue(
        method = "writeList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = true
        ),
        expect = 1
    )
    private static boolean trainresync$allowAdminCommandMaidAttack(boolean original, @Local Player playerIn) {
        return EntityMaidMixin.trainresync$adminIsOwner(original, playerIn);
    }
}
