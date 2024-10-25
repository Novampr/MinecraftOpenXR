package net.sorenon.forge;

import com.jcraft.jogg.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kore.mcoxr.MCOpenXRForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;

import java.nio.file.Path;

public class XPlatImpl {
    public static boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    public static void sendPosePacket(PosePacket packet) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());

        PosePacket.CODEC.encode(byteBuf, packet);

        MCOpenXRForge.C2S_POSE_CHANNEL.send(byteBuf, PacketDistributor.SERVER.noArg());
    }

    public static void sendXRPlayerPacket(XRPlayerPacket packet) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());

        XRPlayerPacket.CODEC.encode(byteBuf, packet);

        MCOpenXRForge.C2S_PLAYER_CHANNEL.send(byteBuf, PacketDistributor.SERVER.noArg());
    }

    public static void sendTeleportPacket(TeleportPacket packet) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());

        TeleportPacket.CODEC.encode(byteBuf, packet);

        MCOpenXRForge.C2S_PLAYER_CHANNEL.send(byteBuf, PacketDistributor.SERVER.noArg());
    }

    public static Path getConfigDir() {
        return FMLLoader.getGamePath().resolve("config");
    }
}
