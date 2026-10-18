package com.ashbill.trainresync.mixin.pantographs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "de.mrjulsen.paw.item.CatenaryHeadspanWireItem", remap = false)
public abstract class CatenaryHeadspanWireItemMixin {
    @WrapOperation(method = "createHudInfoText", at = @At(value = "INVOKE",
            target = "Lde/mrjulsen/mcdragonlib/util/TextUtils;text(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent trainresync$translateHeightLabel(String text, Operation<MutableComponent> original) {
        // Leave numeric values, formatting and all other text construction unchanged.
        return switch (text) {
            case "Upper Tension Wire height difference" -> Component.translatable("trainresync.paw.upper_tension_height_difference");
            case "Top Support Wire height difference" -> Component.translatable("trainresync.paw.top_support_height_difference");
            default -> original.call(text);
        };
    }
}
