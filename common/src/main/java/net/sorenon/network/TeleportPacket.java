package net.sorenon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record TeleportPacket() implements CustomPacketPayload {
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("mcxr", "teleport");
    public static final CustomPacketPayload.Type<TeleportPacket> ID = new CustomPacketPayload.Type<>(LOCATION);
    public static final StreamCodec<FriendlyByteBuf, TeleportPacket> CODEC = StreamCodec.unit(new TeleportPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}