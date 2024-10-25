package net.sorenon.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorenon.MCOpenXR;
import net.sorenon.MCOpenXRScale;
import net.sorenon.Pose;
import net.sorenon.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

import static net.minecraft.world.entity.player.Player.STANDING_DIMENSIONS;

@Mixin(value = Player.class, priority = 10_000 /*Pehuki*/)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerExt {

    @Shadow
    @Final
    private static Map<net.minecraft.world.entity.Pose, EntityDimensions> POSES;

    @Shadow
    public abstract boolean hurt(DamageSource source, float amount);

    @Unique
    public boolean isXr = false;

    @Unique
    public Pose headPose = new Pose();

    @Unique
    public Pose leftHandPose = new Pose();

    @Unique
    public Pose rightHandPose = new Pose();

    @Unique
    public float height = 0;

    @Unique
    public ThreadLocal<HumanoidArm> overrideTransform = ThreadLocal.withInitial(() -> null);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void preTick(CallbackInfo ci) {
        if (this.isXR()) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions overrideDims(net.minecraft.world.entity.Pose pose, EntityDimensions entityDimensions) {
        boolean dynamicHeight = MCOpenXR.getCoreConfig().dynamicPlayerHeight();
        boolean thinnerBB = MCOpenXR.getCoreConfig().thinnerPlayerBoundingBox();
        if (!dynamicHeight && !thinnerBB) {
            return entityDimensions;
        }

        EntityDimensions vanilla = POSES.getOrDefault(pose, STANDING_DIMENSIONS);

        if (this.isXR()) {
            final float scale = MCOpenXRScale.getScale(this);

            float width = vanilla.width();
            if (thinnerBB) {
                width = 0.5f;
            }

            if (dynamicHeight) {
                final float minHeight = 0.5f * scale;
                final float currentHeight = this.getBbHeight();
//                final float wantedHeight = (headPose.pos.y - (float) this.position().y + 0.125f * scale);
                final float wantedHeight = height + 0.125f * scale;
                final float deltaHeight = wantedHeight - currentHeight;

                if (deltaHeight <= 0) {
                    return EntityDimensions.scalable(width * scale, Math.max(wantedHeight, minHeight));
                }

                AABB currentSize = this.getBoundingBox();
                List<VoxelShape> list = this.level().getEntityCollisions(this, currentSize.expandTowards(0, deltaHeight, 0));
                final double maxDeltaHeight = collideBoundingBox(this, new Vec3(0, deltaHeight, 0), currentSize, this.level(), list).y;

                return EntityDimensions.scalable(width * scale, Math.max(currentHeight + (float) maxDeltaHeight, minHeight));
            } else {
                return EntityDimensions.scalable(width * scale, vanilla.height());
            }
        }
        return entityDimensions;
    }

    @Override
    public Pose getHeadPose() {
        return headPose;
    }

    @Override
    public Pose getLeftHandPose() {
        return leftHandPose;
    }

    @Override
    public Pose getRightHandPose() {
        return rightHandPose;
    }

    @Override
    public void setIsXr(boolean isXr) {
        this.isXr = isXr;
    }

    @Override
    public boolean isXR() {
        return isXr;
    }

    @Override
    public ThreadLocal<HumanoidArm> getOverrideTransform() {
        return this.overrideTransform;
    }
}
