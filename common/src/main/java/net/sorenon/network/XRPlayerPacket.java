package net.sorenon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record XRPlayerPacket(boolean isXr) implements CustomPacketPayload {
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("mcxr", "is_xr_player");
    public static final CustomPacketPayload.Type<XRPlayerPacket> ID = new CustomPacketPayload.Type<>(LOCATION);
    public static final StreamCodec<FriendlyByteBuf, XRPlayerPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, XRPlayerPacket::isXr,
            XRPlayerPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
