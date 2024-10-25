package net.sorenon.mixin.rendering;

import net.minecraft.client.Minecraft;
import net.sorenon.MCOpenXRClient;
import net.sorenon.accessor.Matrix4fExt;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import net.sorenon.rendering.RenderPass;
import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.openxr.XrFovf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Matrix4f.class, remap = false)
public abstract class Matrix4fMixin implements Matrix4fExt {

    @Unique
    private static final MCOpenXRGameRenderer XR_RENDERER = MCOpenXRClient.MCXR_GAME_RENDERER;

    @Shadow
    float m00;

    @Shadow
    float m01;

    @Shadow
    float m02;

    @Shadow
    float m03;

    @Shadow
    float m13;

    @Shadow
    float m12;

    @Shadow
    float m11;

    @Shadow
    float m10;

    @Shadow
    float m20;

    @Shadow
    float m21;

    @Shadow
    float m22;

    @Shadow
    float m23;

    @Shadow
    float m30;

    @Shadow
    float m31;

    @Shadow
    float m32;

    @Shadow
    float m33;

    @Override
    public void setXrProjection(XrFovf fov, float nearZ, float farZ) {
        Minecraft client = Minecraft.getInstance();
        nearZ = MCOpenXRClient.modifyProjectionMatrixDepth(nearZ, client.getCameraEntity(), client.getFrameTimeNs());
        float tanLeft = Math.tan(fov.angleLeft());
        float tanRight = Math.tan(fov.angleRight());
        float tanDown = Math.tan(fov.angleDown());
        float tanUp = Math.tan(fov.angleUp());
        float tanAngleWidth = tanRight - tanLeft;
        float tanAngleHeight = tanUp - tanDown;
        m00 = 2.0f / tanAngleWidth;
        m10 = 0.0f;
        m20 = 0.0f;
        m30 = 0.0f;
        m01 = 0.0f;
        m11 = 2.0f / tanAngleHeight;
        m21 = 0.0f;
        m31 = 0.0f;
        m02 = (tanRight + tanLeft) / tanAngleWidth;
        m12 = (tanUp + tanDown) / tanAngleHeight;
        m22 = -(farZ + nearZ) / (farZ - nearZ);
        m32 = -1.0f;
        m03 = 0.0f;
        m13 = 0.0f;
        m23 = -(farZ * (nearZ + nearZ)) / (farZ - nearZ);
        m33 = 0.0f;
    }

    @Inject(method = "perspective(FFFF)Lorg/joml/Matrix4f;", cancellable = true, at = @At("HEAD"), remap = false)
    private void overwriteProjectionMatrix(float fov, float aspectRatio, float cameraDepth, float viewDistance, CallbackInfoReturnable<Matrix4f> cir) {
        if (XR_RENDERER.renderPass instanceof RenderPass.XrWorld renderPass) {
            Matrix4f mat = new Matrix4f();
            ((Matrix4fExt) mat).setXrProjection(renderPass.fov, cameraDepth, viewDistance);
            cir.setReturnValue(mat);
        }
    }
}
