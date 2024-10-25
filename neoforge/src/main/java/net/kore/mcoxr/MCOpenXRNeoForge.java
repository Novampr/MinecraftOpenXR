package net.kore.mcoxr;

import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sorenon.*;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;
import org.joml.Vector3f;

@Mod(MCOpenXR.MODID)
public class MCOpenXRNeoForge {
    public MCOpenXRNeoForge(IEventBus eventBus) {
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::packetSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        new MCOpenXR();
    }

    private void clientSetup(FMLClientSetupEvent event) {
        new MCOpenXRClient();
    }

    private void packetSetup(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(XRPlayerPacket.ID, XRPlayerPacket.CODEC, ((payload, context) -> {
            context.player().getServer().execute(() -> {
                PlayerExt acc = (PlayerExt) context.player();
                acc.setIsXr(payload.isXr());
                context.player().refreshDimensions();
            });
        }));

        registrar.playToServer(PosePacket.ID, PosePacket.CODEC, ((payload, context) -> {
            Pose pose1 = payload.pose1();
            Pose pose2 = payload.pose2();
            Pose pose3 = payload.pose3();
//          var height = buf.readFloat();
            context.player().getServer().execute(() -> {
                PlayerExt acc = (PlayerExt) context.player();
                acc.getHeadPose().set(pose1);
                acc.getLeftHandPose().set(pose2);
                acc.getRightHandPose().set(pose3);
//              acc.setHeight(height);
            });
        }));

        registrar.playToServer(TeleportPacket.ID, TeleportPacket.CODEC, ((payload, context) -> {
            context.player().getServer().execute(() -> {
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
}
