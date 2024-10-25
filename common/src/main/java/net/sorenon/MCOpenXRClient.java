package net.sorenon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.input.ControllerPoses;
import net.sorenon.network.PosePacket;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import net.sorenon.openxr.OpenXRState;
import net.sorenon.rendering.VrFirstPersonRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.*;
import org.lwjgl.openxr.XR;
import org.lwjgl.system.Configuration;

import static net.sorenon.MCOpenXR.MODID;
//import virtuoel.pehkui.util.ScaleUtils;

public class MCOpenXRClient {
    public static ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    public static Logger LOGGER = LogManager.getLogger("MCOpenXR");

    public static final OpenXRState OPEN_XR_STATE = new OpenXRState();
    public static final MCOpenXRGameRenderer MCXR_GAME_RENDERER = new MCOpenXRGameRenderer();

    public static MCOpenXRClient INSTANCE;
    public MCOpenXRGuiManager MCOpenXRGuiManager = new MCOpenXRGuiManager();
    public VrFirstPersonRenderer vrFirstPersonRenderer = new VrFirstPersonRenderer(MCOpenXRGuiManager);
    public static final ControllerPoses viewSpacePoses = new ControllerPoses();

    public static final boolean android = (boolean) System.getProperties().getOrDefault("mcoxr.android", false);

    //Stage space => Unscaled Physical Space => Physical Space => Minecraft Space
    //OpenXR         GUI                        Roomscale Logic   Minecraft Logic
    //      Rotated + Translated           Scaled          Translated

    public static boolean heightAdjustStand = false;

    public static float heightAdjust = 0;

    /**
     * The yaw rotation of STAGE space in physical space
     * Used to let the user turn
     */
    public static float stageTurn = 0;

    /**
     * The position of STAGE space in physical space
     * Used to let the user turn around one physical space position and
     * to let the user snap to the player entity position when roomscale movement is off
     */
    public static Vector3f stagePosition = new Vector3f(0, 0, 0);

    /**
     * The position of physical space in Minecraft space
     * xrOrigin = camaraEntity.pos - playerPhysicalPosition
     */
    public static Vector3d xrOrigin = new Vector3d(0, 0, 0);

    /**
     * The position of the player entity in physical space
     * If roomscale movement is disabled this vector is zero (meaning the player is at xrOrigin)
     * This is used to calculate xrOrigin
     */
    public static Vector3d playerPhysicalPosition = new Vector3d();

    public static int getMainHand() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getMainArm().ordinal();
        } else {
            return HumanoidArm.RIGHT.ordinal();
        }
    }

    public MCOpenXRClient() {
        Configuration.OPENXR_EXPLICIT_INIT.set(true);
        PlayOptions.init();
        PlayOptions.load();
        PlayOptions.save();

        INSTANCE = this;
        if (!PlayOptions.xrUninitialized) {
            XR.create("openxr_loader");
        }
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }

    public static void resetView() {
        Vector3f pos = new Vector3f(MCOpenXRClient.viewSpacePoses.getStagePose().getPos());
        new Quaternionf().rotateLocalY(stageTurn).transform(pos);
        if (MCOpenXR.getCoreConfig().roomscaleMovement()) {
            playerPhysicalPosition.set(MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos());
        } else {
            playerPhysicalPosition.zero();
        }

        MCOpenXRClient.stagePosition = new Vector3f(0, 0, 0).sub(pos).mul(1, 0, 1);
    }

    public static float getCameraScale() {
        return getCameraScale(1.0f);
    }

    public static float getCameraScale(float delta) {
        var cam = Minecraft.getInstance().cameraEntity;
        if (cam == null) {
            return 1;
        } else {
            return MCOpenXRScale.getScale(cam, delta);
        }
    }

    public static float modifyProjectionMatrixDepth(float depth, Entity entity, float tickDelta) {
        //if (Platform.isModLoaded("pehkui")) {
            //return ScaleUtils.modifyProjectionMatrixDepth(MCOpenXRClient.getCameraScale(tickDelta), depth, entity, tickDelta);
        //}
        return depth;
    }

    public static void setPlayerPoses(
            Player player,
            Pose headPose,
            Pose leftHandPose,
            Pose rightHandPose,
//            float height,
            float handAngleAdjust
    ) {
        PlayerExt acc = (PlayerExt) player;
        acc.getHeadPose().set(headPose);
        acc.getLeftHandPose().set(leftHandPose);
        acc.getRightHandPose().set(rightHandPose);
//        acc.setHeight(height);

        acc.getHeadPose().pos.sub((float) player.getX(), (float) player.getY(), (float) player.getZ());
        acc.getLeftHandPose().pos.sub((float) player.getX(), (float) player.getY(), (float) player.getZ());
        acc.getRightHandPose().pos.sub((float) player.getX(), (float) player.getY(), (float) player.getZ());

        acc.getLeftHandPose().orientation.rotateX(handAngleAdjust);
        acc.getRightHandPose().orientation.rotateX(handAngleAdjust);

//        FriendlyByteBuf buf = PacketByteBufs.create();
//        acc.getHeadPose().write(buf);
//        acc.getLeftHandPose().write(buf);
//        acc.getRightHandPose().write(buf);
//        buf.writeFloat(height);

        XPlat.sendPosePacket(new PosePacket(acc.getHeadPose(), acc.getLeftHandPose(), acc.getRightHandPose()));
    }
}
