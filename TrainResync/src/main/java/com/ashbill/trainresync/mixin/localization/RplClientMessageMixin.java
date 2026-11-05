package com.ashbill.trainresync.mixin.localization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Only the player-facing network-version mismatch; keep protocol checks and diagnostics intact. */
@Pseudo
@Mixin(targets = "rbasamoyai.ritchiesprojectilelib.network.RPLClientHandlers", remap = false)
public abstract class RplClientMessageMixin {
    @WrapOperation(method = "checkVersion", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent trainresync$networkMismatch(String text, Operation<MutableComponent> original) {
        return text.equals("Ritchie's Projectile Library on the client uses a different network format than the server.")
                ? Component.translatable("trainresync.rpl.network_mismatch") : original.call(text);
    }

    @WrapOperation(method = "checkVersion", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent trainresync$matchingFormat(MutableComponent component, String text,
                                                               Operation<MutableComponent> original) {
        return text.equals(" Please use a matching format.")
                ? component.append(Component.translatable("trainresync.rpl.matching_format")) : original.call(component, text);
    }
}
