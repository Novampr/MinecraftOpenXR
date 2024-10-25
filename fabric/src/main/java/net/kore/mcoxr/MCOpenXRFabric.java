package net.kore.mcoxr;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.HumanoidArm;
import net.sorenon.*;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;
import org.joml.Vector3f;

public class MCOpenXRFabric implements ClientModInitializer, ModInitializer {
    @Override
    public void onInitializeClient() {
        new MCOpenXRClient();

        PayloadTypeRegistry.playC2S().register(XRPlayerPacket.ID, XRPlayerPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PosePacket.ID, PosePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportPacket.ID, TeleportPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(XRPlayerPacket.ID, ((payload, context) -> {
            context.server().execute(() -> {
                PlayerExt acc = (PlayerExt) context.player();
                acc.setIsXr(payload.isXr());
                context.player().refreshDimensions();
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver(PosePacket.ID, ((payload, context) -> {
            Pose pose1 = payload.pose1();
            Pose pose2 = payload.pose2();
            Pose pose3 = payload.pose3();
//          var height = buf.readFloat();
            context.server().execute(() -> {
                PlayerExt acc = (PlayerExt) context.player();
                acc.getHeadPose().set(pose1);
                acc.getLeftHandPose().set(pose2);
                acc.getRightHandPose().set(pose3);
//              acc.setHeight(height);
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver(TeleportPacket.ID, ((payload, context) -> {
            context.server().execute(() -> {
                PlayerExt acc = (PlayerExt) context.player();
                Pose pose;

                if (context.player().getMainArm() == HumanoidArm.LEFT) {
                    pose = acc.getRightHandPose();
                } else {
                    pose = acc.getLeftHandPose();
                }

                Vector3f dir = pose.getOrientation().transform(new Vector3f(0, -1, 0));

                var pos = Teleport.tp(context.player(), JOMLUtil.convert(pose.getPos()), JOMLUtil.convert(dir));
                if (pos != null) {
                    context.player().setPos(pos);
                } else {
                    MCOpenXR.LOGGER.warn("Player {} attempted an invalid teleport", context.player().toString());
                }
            });
        }));
    }

    @Override
    public void onInitialize() {
        new MCOpenXR();
    }
}
