package net.sorenon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.config.MCOpenXRConfig;
import net.sorenon.config.MCOpenXRConfigImpl;
import net.sorenon.network.PosePacket;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class MCOpenXR {
    public static MCOpenXR INSTANCE;
    public static final String MODID = "mcoxr";

    public static final Logger LOGGER = LogManager.getLogger("MCOpenXR");

    public final MCOpenXRConfigImpl config = new MCOpenXRConfigImpl();

    public MCOpenXR() {
        init();
    }

    public void init() {
        INSTANCE = this;
        if (!XPlat.isClient()) {
            config.xrEnabled = true;
        }
    }

    public static MCOpenXRConfig getCoreConfig() {
        return INSTANCE.config;
    }

    public static HumanoidArm handToArm(LivingEntity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return entity.getMainArm();
        } else {
            return entity.getMainArm().getOpposite();
        }
    }
}
