package net.sorenon;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;

import java.nio.file.Path;

public interface XPlat {
    @ExpectPlatform
    static boolean isClient() {
        return false;
    }

    @ExpectPlatform
    static void sendPosePacket(PosePacket packet) {

    }

    @ExpectPlatform
    static void sendXRPlayerPacket(XRPlayerPacket packet) {

    }

    @ExpectPlatform
    static void sendTeleportPacket(TeleportPacket packet) {

    }

    @ExpectPlatform
    static Path getConfigDir() {
        return null;
    }
}
