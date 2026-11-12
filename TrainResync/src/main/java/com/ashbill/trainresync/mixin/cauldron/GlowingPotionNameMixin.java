package com.ashbill.trainresync.mixin.cauldron;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class GlowingPotionNameMixin {
    @ModifyExpressionValue(method = "getHoverName", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;"))
    private Component trainresync$glowingPotionName(Component name) {
        var stack = (ItemStack)(Object)this;
        if (stack.getItem() instanceof PotionItem && PotionUtils.getMobEffects(stack).stream()
                .anyMatch(effect -> effect.getEffect() == MobEffects.GLOWING))
            return Component.translatable("item.trainresync.glowing_potion_name", name);
        return name;
    }
}
