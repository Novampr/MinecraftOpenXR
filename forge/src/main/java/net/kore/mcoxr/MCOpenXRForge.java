package net.kore.mcoxr;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.SimpleChannel;
import net.sorenon.*;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.network.PosePacket;
import net.sorenon.network.XRPlayerPacket;
import org.joml.Vector3f;

@Mod(MCOpenXR.MODID)
public class MCOpenXRForge {
    public static final EventNetworkChannel C2S_PLAYER_CHANNEL =
            ChannelBuilder.named("mcxr:player")
                    .acceptedVersions((status, version) -> true)
                    .optional()
                    .networkProtocolVersion(0)
                    .eventNetworkChannel();

    public static final EventNetworkChannel C2S_POSE_CHANNEL =
            ChannelBuilder.named("mcxr:pose")
                    .acceptedVersions((status, version) -> true)
                    .optional()
                    .networkProtocolVersion(0)
                    .eventNetworkChannel();

    public static final EventNetworkChannel C2S_TELEPORT_CHANNEL =
            ChannelBuilder.named("mcxr:player")
                    .acceptedVersions((status, version) -> true)
                    .optional()
                    .networkProtocolVersion(0)
                    .eventNetworkChannel();

    public MCOpenXRForge(IEventBus eventBus) {
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::clientSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        new MCOpenXR();

        C2S_PLAYER_CHANNEL.addListener(customPayloadEvent -> {
            XRPlayerPacket payload = XRPlayerPacket.CODEC.decode(customPayloadEvent.getPayload());

            ServerPlayer player = customPayloadEvent.getSource().getSender();
            MinecraftServer server = player.getServer();

            server.execute(() -> {
                PlayerExt acc = (PlayerExt) player;
                acc.setIsXr(payload.isXr());
                player.refreshDimensions();
            });

            customPayloadEvent.getSource().setPacketHandled(true);
        });

        C2S_POSE_CHANNEL.addListener(customPayloadEvent -> {
            PosePacket payload = PosePacket.CODEC.decode(customPayloadEvent.getPayload());

            ServerPlayer player = customPayloadEvent.getSource().getSender();
            MinecraftServer server = player.getServer();

            Pose pose1 = payload.pose1();
            Pose pose2 = payload.pose2();
            Pose pose3 = payload.pose3();
//          var height = buf.readFloat();
            server.execute(() -> {
                PlayerExt acc = (PlayerExt) player;
                acc.getHeadPose().set(pose1);
                acc.getLeftHandPose().set(pose2);
                acc.getRightHandPose().set(pose3);
//              acc.setHeight(height);
            });
        });

        C2S_TELEPORT_CHANNEL.addListener(customPayloadEvent -> {
            ServerPlayer player = customPayloadEvent.getSource().getSender();
            MinecraftServer server = player.getServer();

            server.execute(() -> {
                PlayerExt acc = (PlayerExt) player;
                Pose pose;

                if (player.getMainArm() == HumanoidArm.LEFT) {
                    pose = acc.getRightHandPose();
                } else {
                    pose = acc.getLeftHandPose();
                }

                Vector3f dir = pose.getOrientation().transform(new Vector3f(0, -1, 0));

                var pos = Teleport.tp(player, JOMLUtil.convert(pose.getPos()), JOMLUtil.convert(dir));
                if (pos != null) {
                    player.setPos(pos);
                } else {
                    MCOpenXR.LOGGER.warn("Player {} attempted an invalid teleport", player.toString());
                }
            });
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        new MCOpenXRClient();
    }
}
