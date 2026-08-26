package com.ashbill.trainresync.mixin.maid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;

import com.github.tartaricacid.touhoulittlemaid.network.message.MaidConfigMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidModelMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidSubConfigMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidTaskMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.RefreshMaidBrainMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.RequestEffectMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.SetMaidSoundIdMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.ToggleTabMessage;
import com.github.tartaricacid.touhoulittlemaid.network.message.YsmMaidModelMessage;

import com.ashbill.trainresync.mixin.maid.EntityMaidMixin;


@Mixin(
    value = {
        MaidConfigMessage.class,
        MaidModelMessage.class,
        MaidSubConfigMessage.class,
        MaidTaskMessage.class,
        RefreshMaidBrainMessage.class,
        RequestEffectMessage.class,
        SetMaidSoundIdMessage.class,
        ToggleTabMessage.class,
        YsmMaidModelMessage.class
    },
    remap = false
)
public abstract class MaidMessageMixin {
    
    @ModifyExpressionValue(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = true
        ),
        expect = 1
    )
    private static boolean trainresync$allowAdminConfigMaid(boolean original, @Local Player playerIn) {
        return EntityMaidMixin.trainresync$adminIsOwner(original, playerIn);
    }
}
