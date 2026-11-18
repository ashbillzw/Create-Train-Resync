package com.ashbill.trainresync.currency;

import dev.ithundxr.createnumismatics.Numismatics;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CurrencyBalance {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("trainresync", "currency"), () -> "1",
            NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));
    private static final Map<ServerPlayer, Integer> lastSent = new WeakHashMap<>();
    public static int clientBalance;

    public static void init() {
        CHANNEL.messageBuilder(Integer.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((value, buf) -> buf.writeVarInt(value))
                .decoder(buf -> buf.readVarInt())
                .consumerMainThread((value, ctx) -> clientBalance = value)
                .add();
        MinecraftForge.EVENT_BUS.addListener(CurrencyBalance::update);
    }

    private static void update(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 10 != 0 || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        var account = Numismatics.BANK.getAccount(player.getUUID());
        int balance = account == null ? 0 : account.getBalance();
        Integer previous = lastSent.get(player);
        if (previous != null && previous == balance) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), balance);
        lastSent.put(player, balance);
    }
}
