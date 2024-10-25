package net.sorenon.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface S2CHandler<T extends CustomPacketPayload> {
    void handle(T packet, ServerPlayer player, MinecraftServer server);
}
