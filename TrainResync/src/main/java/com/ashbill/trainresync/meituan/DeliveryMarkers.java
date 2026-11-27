package com.ashbill.trainresync.meituan;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public record DeliveryMarkers(List<BlockPos> points) {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("trainresync", "delivery_markers"), () -> "1",
            NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));

    public static void init() {
        CHANNEL.messageBuilder(DeliveryMarkers.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((markers, buf) -> buf.writeCollection(markers.points(), FriendlyByteBuf::writeBlockPos))
                .decoder(buf -> new DeliveryMarkers(buf.readList(FriendlyByteBuf::readBlockPos)))
                .consumerMainThread((markers, ctx) -> com.ashbill.trainresync.client.DeliveryLabels.update(markers.points()))
                .add();
    }

    static void send(ServerPlayer player, Set<BlockPos> points) {
        if (player != null && CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DeliveryMarkers(List.copyOf(points)));
    }
}
