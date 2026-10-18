package com.ashbill.trainresync.mixin.pantographs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.mrjulsen.wires.item.IPawWireItemBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "de.mrjulsen.wires.item.MultiWireItem", remap = false)
public abstract class MultiWireItemMixin {
    @WrapOperation(method = "appendHoverText", remap = true, at = @At(value = "INVOKE", remap = false,
            target = "Lde/mrjulsen/mcdragonlib/util/TextUtils;text(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0))
    private MutableComponent trainresync$translateType(String text, Operation<MutableComponent> original) {
        // Upstream appends the translated wire type to this label.
        return Component.translatable("trainresync.paw.wire_type");
    }

    @WrapOperation(method = "appendHoverText", remap = true, at = @At(value = "INVOKE", remap = false,
            target = "Lde/mrjulsen/mcdragonlib/util/TextUtils;text(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 1))
    private MutableComponent trainresync$translateAmount(String text, Operation<MutableComponent> original,
            @Local(argsOnly = true) ItemStack stack) {
        return Component.translatable("trainresync.paw.remaining_length", IPawWireItemBase.getRemainingWire(stack));
    }
}
