package net.sorenon.mixin.rendering;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorenon.MCOpenXRClient;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import net.sorenon.rendering.RenderPass;
import net.sorenon.rendering.VrFirstPersonRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.sorenon.MCOpenXRClient.GUI_ICONS_LOCATION;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private MCOpenXRGameRenderer minecraftOpenXR$MCOpenXRGameRenderer = MCOpenXRClient.MCXR_GAME_RENDERER;

    @Unique
    private Camera minecraftOpenXR$camera;
    @Unique
    private DeltaTracker minecraftOpenXR$tracker;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;
    @Shadow
    private ClientLevel level;

    @Shadow protected abstract ParticleStatus calculateParticleLevel(boolean canSpawnOnMinimal);

    @Shadow public abstract void tickRain(Camera camera);

    @Inject(method = "graphicsChanged", at = @At("HEAD"))
    void ongraphicsChanged(CallbackInfo ci) {
        minecraftOpenXR$MCOpenXRGameRenderer.reloadingDepth += 1;
    }

    @Inject(method = "graphicsChanged", at = @At("RETURN"))
    void aftergraphicsChanged(CallbackInfo ci) {
        minecraftOpenXR$MCOpenXRGameRenderer.reloadingDepth -= 1;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void headRenderLevel(DeltaTracker tracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.minecraftOpenXR$camera = camera;
        this.minecraftOpenXR$tracker = tracker;
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private void afterEntities(CallbackInfo ci, @Local PoseStack matrices) {
        if (minecraftOpenXR$MCOpenXRGameRenderer.renderPass instanceof RenderPass.XrWorld) {
            if (!Minecraft.getInstance().options.hideGui && !MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.isScreenOpen()) {
                var hitResult = Minecraft.getInstance().hitResult;
                if (hitResult != null && !MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.isScreenOpen()) {
                    Vec3 camPos = minecraftOpenXR$camera.getPosition();
                    matrices.pushPose();

                    double x = hitResult.getLocation().x();
                    double y = hitResult.getLocation().y();
                    double z = hitResult.getLocation().z();
                    matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);

                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        matrices.mulPose(((BlockHitResult) hitResult).getDirection().getRotation());
                    } else {
                        matrices.mulPose(minecraftOpenXR$camera.rotation());
                        matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }

                    matrices.scale(0.5f, 1, 0.5f);
                    RenderType SHADOW_LAYER = RenderType.entityCutoutNoCull(GUI_ICONS_LOCATION);
                    VertexConsumer vertexConsumer = renderBuffers.bufferSource().getBuffer(SHADOW_LAYER);

                    PoseStack.Pose entry = matrices.last();

                    vertexConsumer.addVertex(entry.pose(), -0.3f + (0.5f / 16f), 0.005f, -0.3f + (0.5f / 16f)).setColor(1.0F, 1.0F, 1.0F, 1.0f).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(15728880, 1).setNormal(0.0F, 0.0F, 1.0F);
                    vertexConsumer.addVertex(entry.pose(), -0.3f + (0.5f / 16f), 0.005f, 0.3f + (0.5f / 16f)).setColor(1.0F, 1.0F, 1.0F, 1.0f).setUv(0, 0.0625f).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(15728880, 1).setNormal(0.0F, 0.0F, 1.0F);
                    vertexConsumer.addVertex(entry.pose(), 0.3f + (0.5f / 16f), 0.005f, 0.3f + (0.5f / 16f)).setColor(1.0F, 1.0F, 1.0F, 1.0f).setUv(0.0625f, 0.0625f).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(15728880, 1).setNormal(0.0F, 0.0F, 1.0F);
                    vertexConsumer.addVertex(entry.pose(), 0.3f + (0.5f / 16f), 0.005f, -0.3f + (0.5f / 16f)).setColor(1.0F, 1.0F, 1.0F, 1.0f).setUv(0.0625f, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(15728880, 1).setNormal(0.0F, 0.0F, 1.0F);

                    matrices.popPose();
                }

                if (minecraftOpenXR$camera.getEntity() instanceof LocalPlayer player) {
                    MCOpenXRClient.INSTANCE.vrFirstPersonRenderer.render(
                            player,
                            VrFirstPersonRenderer.getLight(minecraftOpenXR$camera, level),
                            matrices,
                            renderBuffers.bufferSource(),
                            minecraftOpenXR$tracker.getGameTimeDeltaTicks()
                    );
                }
            }
        }
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"
            )
    )
    private void lastRender(DeltaTracker tracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local PoseStack stack) {
        if (minecraftOpenXR$MCOpenXRGameRenderer.renderPass instanceof RenderPass.XrWorld) {
            Matrix4fStack poseStack = RenderSystem.getModelViewStack();
            poseStack.pushMatrix();
            poseStack.identity();
            RenderSystem.applyModelViewMatrix();
            GlStateManager._disableDepthTest();

            MCOpenXRClient.INSTANCE.vrFirstPersonRenderer.renderLast(camera, stack, level, tracker);

            poseStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
