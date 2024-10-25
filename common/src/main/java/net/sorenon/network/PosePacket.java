package net.sorenon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sorenon.Pose;
import org.jetbrains.annotations.NotNull;

public record PosePacket(Pose pose1, Pose pose2, Pose pose3) implements CustomPacketPayload {
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath("mcxr", "poses");
    public static final CustomPacketPayload.Type<PosePacket> ID = new CustomPacketPayload.Type<>(LOCATION);
    public static final StreamCodec<FriendlyByteBuf, PosePacket> CODEC = StreamCodec.composite(
            Pose.STREAM_CODEC, PosePacket::pose1,
            Pose.STREAM_CODEC, PosePacket::pose2,
            Pose.STREAM_CODEC, PosePacket::pose3,
            PosePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}