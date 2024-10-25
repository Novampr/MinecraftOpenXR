package sorenon.neoforge;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;

import java.nio.file.Path;

public class XPlatImpl {
    public static boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    public static void sendPosePacket(PosePacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendXRPlayerPacket(XRPlayerPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendTeleportPacket(TeleportPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static Path getConfigDir() {
        return FMLLoader.getGamePath().resolve("config");
    }
}
