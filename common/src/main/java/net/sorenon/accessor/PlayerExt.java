package net.sorenon.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.HumanoidArm;
import net.sorenon.Pose;

public interface PlayerExt {

    Pose getHeadPose();

    Pose getLeftHandPose();

    Pose getRightHandPose();

    default Pose getPoseForArm(HumanoidArm arm) {
        if (arm == HumanoidArm.LEFT) {
            return getLeftHandPose();
        } else {
            return getRightHandPose();
        }
    }

    void setIsXr(boolean isXr);

    boolean isXR();

    ThreadLocal<HumanoidArm> getOverrideTransform();

    EntityDimensions overrideDims(net.minecraft.world.entity.Pose pose, EntityDimensions entityDimensions);
}
