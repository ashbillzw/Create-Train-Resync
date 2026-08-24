package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;


@Mixin(value = EntityMaid.class, remap = false)
public abstract class EntityMaidMixin { 
    
    @ModifyExpressionValue(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z"
        ),
        remap = true
    )
    private boolean trainresync$allowAdminMaidInteraction(boolean original, @Local Player playerIn) {
        if (!original && playerIn.hasPermissions(2)) {
            if (!playerIn.level().isClientSide)
                playerIn.sendSystemMessage(Component.literal("[AshBill] 提示：您正在使用管理员权限强行访问女仆喵（其他主人的）"));
            return true;
        }
        return original;
    }
}
