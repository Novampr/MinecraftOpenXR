package net.sorenon.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;

import java.nio.file.Path;

public class XPlatImpl {
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }

    public static void sendPosePacket(PosePacket packet) {
        ClientPlayNetworking.send(packet);
    }

    public static void sendXRPlayerPacket(XRPlayerPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    public static void sendTeleportPacket(TeleportPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
