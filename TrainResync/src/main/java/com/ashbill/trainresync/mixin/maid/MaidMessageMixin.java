package com.ashbill.trainresync.mixin.maid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.github.tartaricacid.touhoulittlemaid.network.message.MaidConfigMessage;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;


@Mixin(
    value = {
        MaidConfigMessage.class
        // MaidModelMessage.class,
        // MaidSubConfigMessage.class,
        // MaidTaskMessage.class,
        // RefreshMaidBrainMessage.class,
        // RequestEffectMessage.class,
        // SetMaidSoundIdMessage.class,
        // ToggleTabMessage.class,
        // YsmMaidModelMessage.class
    },
    remap = false
)
public abstract class MaidMessageMixin {
    
    @ModifyExpressionValue(
        method = "/^lambda\\$handle\\$\\d+$/", // lambda$handle$<int> for matching "entity instanceof EntityMaid maid && maid.isOwnedBy(sender)"
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = true
        ),
        expect = 1
    )
    private static boolean trainresync$allowAdminConfigMaid(boolean original, @Local ServerPlayer sender) {
        return AdminIsOwner.trainresync$adminIsOwner(original, sender);
    }
}
