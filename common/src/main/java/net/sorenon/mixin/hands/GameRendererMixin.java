package net.sorenon.mixin.hands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.sorenon.JOMLUtil;
import net.sorenon.MCOpenXR;
import net.sorenon.MCOpenXRClient;
import net.sorenon.Pose;
import net.sorenon.PlayOptions;
import net.sorenon.input.XrInput;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean enabled() {
        return MCOpenXR.getCoreConfig().controllerRaytracing() && MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode();
    }

    @Inject(method = "pick(F)V", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/GameRenderer;pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"))
    private void overrideEntity$raycast(float tickDelta, CallbackInfo ci) {
        if (enabled()) {
            Entity entity = this.minecraft.getCameraEntity();
            Pose pose = XrInput.handsActionSet.gripPoses[MCOpenXRClient.getMainHand()].getMinecraftPose();
            Vec3 pos = JOMLUtil.convert(pose.getPos());
            Vector3f dir1 = pose.getOrientation().rotateX((float) Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf()).transform(new Vector3f(0, -1, 0));
            Vec3 dir = new Vec3(dir1.x, dir1.y, dir1.z);
            Vec3 endPos = pos.add(dir.scale(4.5));
            this.minecraft.hitResult = entity.level().clip(new ClipContext(pos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        }
    }

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 alterStartPosVec(Entity instance, float tickDelta) {
        if (enabled()) {
            Pose pose = XrInput.handsActionSet.gripPoses[MCOpenXRClient.getMainHand()].getMinecraftPose();
            return JOMLUtil.convert(pose.pos);
        } else {
            return instance.getEyePosition(tickDelta);
        }
    }

    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 alterDirVec(Entity instance, float tickDelta) {
        if (enabled()) {
            Pose pose = XrInput.handsActionSet.gripPoses[MCOpenXRClient.getMainHand()].getMinecraftPose();
            return JOMLUtil.convert(
                    pose.getOrientation()
                            .rotateX((float) Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf())
                            .transform(new Vector3f(0, -1, 0))
            );
        } else {
            return instance.getViewVector(tickDelta);
        }
    }
}
