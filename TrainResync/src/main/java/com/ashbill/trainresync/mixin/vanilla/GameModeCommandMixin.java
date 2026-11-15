package com.ashbill.trainresync.mixin.vanilla;

import com.ashbill.trainresync.sponsorship.PlayerSponsorship;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameModeCommand.class)
abstract class GameModeCommandMixin {
    @WrapOperation(method = "setMode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setGameMode(Lnet/minecraft/world/level/GameType;)Z"))
    private static boolean trainresync$adminGameMode(ServerPlayer player, GameType mode, Operation<Boolean> original) {
        return PlayerSponsorship.adminGameMode(player, mode, () -> original.call(player, mode));
    }
}
