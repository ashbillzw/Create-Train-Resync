package com.ashbill.trainresync.mixin.cauldron;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TippedArrowItem.class)
public abstract class TippedArrowItemMixin {
    @ModifyArg(method = "appendHoverText", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/alchemy/PotionUtils;addPotionTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;F)V"))
    private float trainresync$arrowTooltipScale(ItemStack arrow, List<Component> tooltip, float scale) {
        // Cauldron arrows carry only custom effects, which Arrow applies at full duration.
        return PotionUtils.getPotion(arrow) == Potions.EMPTY ? 1.0F : scale;
    }
}
