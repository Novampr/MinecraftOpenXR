package net.sorenon.openxr;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.sorenon.*;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.mixin.LivingEntityAcc;
import net.sorenon.accessor.MinecraftExt;
import net.sorenon.input.XrInput;
import net.sorenon.network.TeleportPacket;
import net.sorenon.network.XRPlayerPacket;
import net.sorenon.rendering.MCXRCamera;
import net.sorenon.rendering.RenderPass;
import net.sorenon.rendering.XrRenderTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import java.nio.IntBuffer;

import static net.minecraft.client.Minecraft.ON_OSX;
import static org.lwjgl.system.MemoryStack.stackCallocInt;
import static org.lwjgl.system.MemoryStack.stackPush;

public class MCOpenXRGameRenderer {
    private static final Logger LOGGER = LogManager.getLogger("MCOpenXR");
    private Minecraft client;
    private MinecraftExt clientExt;
    private MainTarget mainRenderTarget;
    private MCXRCamera camera;

    private OpenXRInstance instance;
    private OpenXRSession session;

    public RenderPass renderPass = RenderPass.VANILLA;
    public ShaderInstance blitShader;
    public ShaderInstance blitShaderSRGB;
    public ShaderInstance guiBlitShader;

    private boolean xrDisabled = false;
    private boolean xrReady = true;

    public boolean overrideWindowSize = false;
    public int reloadingDepth = 0;
    public boolean guiMode = false;

    public void initialize(Minecraft client) {
        this.client = client;
        this.clientExt = (MinecraftExt) client;
        mainRenderTarget = (MainTarget) client.getMainRenderTarget();
        camera = (MCXRCamera) client.gameRenderer.getMainCamera();
    }

    public void setSession(OpenXRSession session) {
        this.session = session;
        if (session != null) {
            this.instance = session.instance;
        } else {
            this.instance = null;
        }
    }

    public boolean isXrMode() {
        return Minecraft.getInstance().level != null && session != null && session.running && xrReady && !xrDisabled;
    }

    public void renderFrame(boolean xrDisabled) {
        if (this.xrDisabled != xrDisabled) {
            MCOpenXRClient.resetView();
        }
        this.xrDisabled = xrDisabled;

        try (MemoryStack stack = stackPush()) {
            var frameState = XrFrameState.calloc(stack).type(XR10.XR_TYPE_FRAME_STATE);

            if (isXrMode()) {
                GLFW.glfwSwapBuffers(Minecraft.getInstance().getWindow().getWindow());
            }
            //TODO tick game and poll input during xrWaitFrame (this might not work due to the gl context belonging to the xrWaitFrame thread)
            instance.checkPanic(XR10.xrWaitFrame(
                    session.handle,
                    XrFrameWaitInfo.calloc(stack).type(XR10.XR_TYPE_FRAME_WAIT_INFO),
                    frameState
            ), "xrWaitFrame");

            xrReady = frameState.shouldRender();

            instance.checkPanic(XR10.xrBeginFrame(
                    session.handle,
                    XrFrameBeginInfo.calloc(stack).type(XR10.XR_TYPE_FRAME_BEGIN_INFO)
            ), "xrBeginFrame");

            PointerBuffer layers = stack.callocPointer(1);

            if (frameState.shouldRender()) {
                if (this.isXrMode() && !xrDisabled) {
                    var layer = renderXrGame(frameState.predictedDisplayTime(), stack);
                    if (layer != null) {
                        layers.put(layer.address());
                    }
                } else {
                    var layer = renderBlankLayer(frameState.predictedDisplayTime(), stack);
                    layers.put(layer.address());
                }
            }
            layers.flip();

            int result = XR10.xrEndFrame(
                    session.handle,
                    XrFrameEndInfo.calloc(stack)
                            .type(XR10.XR_TYPE_FRAME_END_INFO)
                            .displayTime(frameState.predictedDisplayTime())
                            .environmentBlendMode(XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
                            .layers(layers)
            );
            if (result != XR10.XR_ERROR_TIME_INVALID) {
                instance.checkPanic(result, "xrEndFrame");
            } else {
                LOGGER.warn("Rendering frame took too long! (probably)");
            }
        }
    }

    private Struct renderXrGame(long predictedDisplayTime, MemoryStack stack) {
//        try (MemoryStack stack = stackPush()) {
        this.overrideWindowSize = true;

        XrViewState viewState = XrViewState.calloc(stack).type(XR10.XR_TYPE_VIEW_STATE);
        IntBuffer intBuf = stackCallocInt(1);

        XrViewLocateInfo viewLocateInfo = XrViewLocateInfo.calloc(stack);
        viewLocateInfo.set(XR10.XR_TYPE_VIEW_LOCATE_INFO,
                0,
                session.viewConfigurationType,
                predictedDisplayTime,
                session.xrAppSpace
        );

        instance.checkPanic(XR10.xrLocateViews(session.handle, viewLocateInfo, viewState, intBuf, session.viewBuffer), "xrLocateViews");

        if ((viewState.viewStateFlags() & XR10.XR_VIEW_STATE_POSITION_VALID_BIT) == 0 ||
                (viewState.viewStateFlags() & XR10.XR_VIEW_STATE_ORIENTATION_VALID_BIT) == 0) {
            LOGGER.error("Invalid headset position, try restarting your device");
            return null;
        }

        var projectionLayerViews = XrCompositionLayerProjectionView.calloc(2, stack);

        MCOpenXRGuiManager FGM = MCOpenXRClient.INSTANCE.MCOpenXRGuiManager;

        if (FGM.needsReset) {
            FGM.resetTransform();
        }

        long frameStartTime = Util.getNanos();

        //Ticks the game
        clientExt.preRender(true, () -> {
            //Pre-tick
            //Update poses for tick
            updatePoses(camera.getEntity(), false, predictedDisplayTime, 1.0f, MCOpenXRClient.getCameraScale());

            //Update the server-side player poses
            if (Minecraft.getInstance().player != null && MCOpenXR.getCoreConfig().supportsMCXR()) {
                Player player = Minecraft.getInstance().player;
                PlayerExt acc = (PlayerExt) player;
                if (!acc.isXR()) {
                    XPlat.sendXRPlayerPacket(new XRPlayerPacket(true));
                    acc.setIsXr(true);
                }
                MCOpenXRClient.setPlayerPoses(
                        Minecraft.getInstance().player,
                        MCOpenXRClient.viewSpacePoses.getMinecraftPose(),
                        XrInput.handsActionSet.gripPoses[0].getMinecraftPose(),
                        XrInput.handsActionSet.gripPoses[1].getMinecraftPose(),
//                        MCXRPlayClient.viewSpacePoses.getMinecraftPose().getPos().y - (float) player.position().y,
                        (float) Math.toRadians(PlayOptions.handPitchAdjust)
                );

                if (XrInput.teleport) {
                    XrInput.teleport = false;
                    int handIndex = 0;
                    if (player.getMainArm() == HumanoidArm.LEFT) {
                        handIndex = 1;
                    }

                    Pose pose = XrInput.handsActionSet.gripPoses[handIndex].getMinecraftPose();

                    Vector3f dir = pose.getOrientation().rotateX((float) java.lang.Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf()).transform(new Vector3f(0, -1, 0));

                    var pos = Teleport.tp(player, JOMLUtil.convert(pose.getPos()), JOMLUtil.convert(dir));
                    if (pos != null) {
                        XPlat.sendTeleportPacket(new TeleportPacket());
                        player.setPos(pos);
                    }
                }
            } else {
                XrInput.teleport = false;
            }
        });

        Entity cameraEntity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
        boolean calculate = false;
        if (XrInput.vanillaGameplayActionSet.stand.changedSinceLastSync && XrInput.vanillaGameplayActionSet.stand.currentState) {
            MCOpenXRClient.heightAdjustStand = !MCOpenXRClient.heightAdjustStand;
            if (MCOpenXRClient.heightAdjustStand) {
                calculate = true;
            }
        }

        float frameUserScale = MCOpenXRClient.getCameraScale(client.getFrameTimeNs());
        updatePoses(cameraEntity, calculate, predictedDisplayTime, client.getFrameTimeNs(), frameUserScale);
        camera.updateXR(this.client.level, cameraEntity, MCOpenXRClient.viewSpacePoses.getMinecraftPose());

        client.getWindow().setErrorSection("Render");
        client.getProfiler().push("sound");
        client.getSoundManager().updateSource(client.gameRenderer.getMainCamera());
        client.getProfiler().pop();

        //Render GUI
        this.guiMode = true;
        XrInput.postTick(predictedDisplayTime);
        clientExt.doRender(true, frameStartTime, RenderPass.GUI);
        this.guiMode = false;

        FGM.guiPostProcessRenderTarget.bindWrite(true);
        this.guiBlitShader.setSampler("DiffuseSampler", FGM.guiRenderTarget.getColorTextureId());
        this.guiBlitShader.setSampler("DepthSampler", FGM.guiRenderTarget.getDepthTextureId());
        this.blit(FGM.guiPostProcessRenderTarget, guiBlitShader);
        FGM.guiPostProcessRenderTarget.unbindWrite();

        OpenXRSwapchain swapchain = session.swapchain;

        if (swapchain.getRenderWidth() != mainRenderTarget.viewWidth || swapchain.getRenderHeight() != mainRenderTarget.viewHeight) {
            mainRenderTarget.resize(swapchain.getRenderWidth(), swapchain.getRenderHeight(), ON_OSX);
            client.gameRenderer.resize(swapchain.getRenderWidth(), swapchain.getRenderHeight());
        }

        int swapchainImageIndex = swapchain.acquireImage();

        // Render view to the appropriate part of the swapchain image.
        for (int viewIndex = 0; viewIndex < 2; viewIndex++) {
            // Each view has a separate swapchain which is acquired, rendered to, and released.

            var subImage = projectionLayerViews.get(viewIndex)
                    .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW)
                    .pose(session.viewBuffer.get(viewIndex).pose())
                    .fov(session.viewBuffer.get(viewIndex).fov())
                    .subImage();
            subImage.swapchain(swapchain.handle);
            subImage.imageRect().offset().set(0, 0);
            subImage.imageRect().extent().set(swapchain.width, swapchain.height);
            subImage.imageArrayIndex(viewIndex);

            XrRenderTarget swapchainFramebuffer;
            if (viewIndex == 0) {
                swapchainFramebuffer = swapchain.leftFramebuffers[swapchainImageIndex];
            } else {
                swapchainFramebuffer = swapchain.rightFramebuffers[swapchainImageIndex];
            }
            RenderPass.XrWorld worldRenderPass = RenderPass.XrWorld.create();
            worldRenderPass.fov = session.viewBuffer.get(viewIndex).fov();
            worldRenderPass.eyePoses.updatePhysicalPose(session.viewBuffer.get(viewIndex).pose(), MCOpenXRClient.stageTurn, frameUserScale);
            worldRenderPass.eyePoses.updateGamePose(MCOpenXRClient.xrOrigin);
            worldRenderPass.viewIndex = viewIndex;
            camera.setPose(worldRenderPass.eyePoses.getMinecraftPose());
            clientExt.doRender(true, frameStartTime, worldRenderPass);

            swapchainFramebuffer.bindWrite(true);
            ShaderInstance blitShader;
            if (swapchain.sRGB) {
                blitShader = this.blitShaderSRGB;
            } else {
                blitShader = this.blitShader;
            }

            blitShader.setSampler("DiffuseSampler", mainRenderTarget.getColorTextureId());
            Uniform inverseScreenSize = blitShader.getUniform("InverseScreenSize");
            if (inverseScreenSize != null) {
                inverseScreenSize.set(1f / mainRenderTarget.width, 1f / mainRenderTarget.height);
            }

            mainRenderTarget.setFilterMode(GlConst.GL_LINEAR);
            this.blit(swapchainFramebuffer, blitShader);

            //          ==render to eyes here after eye swapchain.rendertarget is sampled and blit-ed to swapchainFramebuffer (displayed image per eye?)==
            LocalPlayer player = this.client.player;
            if (player != null) {
                //vanilla vignette
                renderVignette(swapchainFramebuffer, cameraEntity);
                //portal
                float g = Mth.lerp(client.getFrameTimeNs(), player.getPortalCooldown(), player.getPortalCooldown());
                if (g > 0.0F && !player.hasEffect(MobEffects.CONFUSION)) {
                    renderPortalOverlay(swapchainFramebuffer, g);
                }
                //hurt
                int hurtTime = player.hurtTime;
                if (hurtTime > 0) {
                    renderOverlay(swapchainFramebuffer, MCOpenXRClient.id("textures/misc/hurt_vr.png"), 0.4f, 0f, 0f, hurtTime * 0.06f);
                }
                //drowning
                float drownPoint = Mth.clamp(2.5f * (0.7f - (float) player.getAirSupply() / (float) player.getMaxAirSupply()), 0f, 1f);
                if (drownPoint > 0f) {
                    renderOverlay(swapchainFramebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 0.0f, 0.0f, 0.25f, drownPoint * 0.9f);
                }
                //on fire
                if (player.isOnFire()) {
                    renderOverlay(swapchainFramebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 1f, 0.7f, 0.2f, 0.9f);
                }
                //frozen
                if (player.getTicksFrozen() > 0) {
                    float freeze = player.getPercentFrozen() * 0.9f;
                    renderOverlay(swapchainFramebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 0.85f, 0.85f, 1f, freeze);
                }
                //death point
                float deathPoint = Mth.clamp(2.5f * (0.7f - player.getHealth() / player.getMaxHealth()), 0f, 1f);
                if (!player.isCreative() && deathPoint > 0f) {
                    renderOverlay(swapchainFramebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 0.4f, 0f, 0f, deathPoint * 0.9f);
                }
            }

            swapchainFramebuffer.unbindWrite();
        }

        this.overrideWindowSize = false;

        blitToBackbuffer(mainRenderTarget);

        instance.checkPanic(XR10.xrReleaseSwapchainImage(
                swapchain.handle,
                XrSwapchainImageReleaseInfo.calloc(stack)
                        .type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        ), "xrReleaseSwapchainImage");

        camera.setPose(MCOpenXRClient.viewSpacePoses.getMinecraftPose());
        clientExt.postRender();

        return XrCompositionLayerProjection.calloc(stack)
                .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION)
                .space(session.xrAppSpace)
                .views(projectionLayerViews);
//        }
    }

    private void updatePoses(Entity camEntity,
                             boolean calculateHeightAdjust,
                             long predictedDisplayTime,
                             float delta,
                             float scale) {
        if (session.state == XR10.XR_SESSION_STATE_FOCUSED) {
            for (int i = 0; i < 2; i++) {
                if (!XrInput.handsActionSet.grip.isActive[i]) {
                    continue;
                }
                session.setPosesFromSpace(XrInput.handsActionSet.grip.spaces[i], predictedDisplayTime, XrInput.handsActionSet.gripPoses[i], scale);
                session.setPosesFromSpace(XrInput.handsActionSet.aim.spaces[i], predictedDisplayTime, XrInput.handsActionSet.aimPoses[i], scale);
            }
            session.setPosesFromSpace(session.xrViewSpace, predictedDisplayTime, MCOpenXRClient.viewSpacePoses, scale);
        }

        if (camEntity != null) { //TODO seriously need to tidy up poses
            if (client.isPaused()) {
                delta = 1.0f;
            }

            if (calculateHeightAdjust && MCOpenXRClient.heightAdjustStand && camEntity instanceof LivingEntity livingEntity) {
                float userHeight = MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos().y();
                float playerEyeHeight = ((LivingEntityAcc) livingEntity).callGetStandingEyeHeight(livingEntity.getPose());

                MCOpenXRClient.heightAdjust = playerEyeHeight - userHeight;
            }

            Entity vehicle = camEntity.getVehicle();
            if (MCOpenXR.getCoreConfig().roomscaleMovement() && vehicle == null) {
                MCOpenXRClient.xrOrigin.set(Mth.lerp(delta, camEntity.xo, camEntity.getX()) - MCOpenXRClient.playerPhysicalPosition.x,
                        Mth.lerp(delta, camEntity.yo, camEntity.getY()),
                        Mth.lerp(delta, camEntity.zo, camEntity.getZ()) - MCOpenXRClient.playerPhysicalPosition.z);
            } else {
                MCOpenXRClient.xrOrigin.set(Mth.lerp(delta, camEntity.xo, camEntity.getX()),
                        Mth.lerp(delta, camEntity.yo, camEntity.getY()),
                        Mth.lerp(delta, camEntity.zo, camEntity.getZ()));
            }
            if (vehicle != null) {
                if (vehicle instanceof LivingEntity) {
                    MCOpenXRClient.xrOrigin.y += 0.60;
                } else {
                    MCOpenXRClient.xrOrigin.y += 0.54;
                }
            }
            if (MCOpenXRClient.heightAdjustStand) {
                MCOpenXRClient.xrOrigin.y += MCOpenXRClient.heightAdjust;
            }

            //sneaking camera shift
            if (camEntity.isShiftKeyDown()) {
                MCOpenXRClient.xrOrigin.y -= 0.25;
            }

            MCOpenXRClient.viewSpacePoses.updateGamePose(MCOpenXRClient.xrOrigin);
            for (var poses : XrInput.handsActionSet.gripPoses) {
                poses.updateGamePose(MCOpenXRClient.xrOrigin);
            }
            for (var poses : XrInput.handsActionSet.aimPoses) {
                poses.updateGamePose(MCOpenXRClient.xrOrigin);
            }
        }
    }

    private Struct renderBlankLayer(long predictedDisplayTime, MemoryStack stack) {
        IntBuffer intBuf = stackCallocInt(1);

        instance.checkPanic(XR10.xrLocateViews(
                session.handle,
                XrViewLocateInfo.calloc(stack).set(XR10.XR_TYPE_VIEW_LOCATE_INFO,
                        0,
                        session.viewConfigurationType,
                        predictedDisplayTime,
                        session.xrAppSpace
                ),
                XrViewState.calloc(stack).type(XR10.XR_TYPE_VIEW_STATE),
                intBuf,
                session.viewBuffer
        ), "xrLocateViews");

        int viewCountOutput = intBuf.get(0);

        var projectionLayerViews = XrCompositionLayerProjectionView.calloc(viewCountOutput);

        int swapchainImageIndex = session.swapchain.acquireImage();

        for (int viewIndex = 0; viewIndex < viewCountOutput; viewIndex++) {
            XrCompositionLayerProjectionView projectionLayerView = projectionLayerViews.get(viewIndex);
            projectionLayerView.type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW);
            projectionLayerView.pose(session.viewBuffer.get(viewIndex).pose());
            projectionLayerView.fov(session.viewBuffer.get(viewIndex).fov());
            projectionLayerView.subImage().swapchain(session.swapchain.handle);
            projectionLayerView.subImage().imageRect().offset().set(0, 0);
            projectionLayerView.subImage().imageRect().extent().set(session.swapchain.width, session.swapchain.height);
            projectionLayerView.subImage().imageArrayIndex(0);
        }

        session.swapchain.leftFramebuffers[swapchainImageIndex].clear(ON_OSX);

        instance.checkPanic(XR10.xrReleaseSwapchainImage(
                session.swapchain.handle,
                XrSwapchainImageReleaseInfo.calloc(stack).type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        ), "xrReleaseSwapchainImage");

        XrCompositionLayerProjection layer = XrCompositionLayerProjection.calloc(stack).type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION);
        layer.space(session.xrAppSpace);
        layer.views(projectionLayerViews);
        return layer;
    }

    public void blit(RenderTarget framebuffer, ShaderInstance shader) {
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        RenderSystem.applyModelViewMatrix();

        int width = framebuffer.width;
        int height = framebuffer.height;

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._disableBlend();

        Matrix4f matrix4f = (new Matrix4f()).ortho2D((float) width, (float) -height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set((new Matrix4f()).translate(0.0F, 0.0F, -2000.0F));
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(matrix4f);
        }

        shader.apply();
        float u = (float) framebuffer.viewWidth / (float) framebuffer.width;
        float v = (float) framebuffer.viewHeight / (float) framebuffer.height;
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(0.0F, height * 2, 0.0F).setUv(0.0F, 1 - v * 2).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(width * 2, 0.0F, 0.0F).setUv(u * 2, 1).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 1).setColor(255, 255, 255, 255);
        BufferUploader.draw(bufferBuilder.build());
        shader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.popMatrix();
    }

    public void blitToBackbuffer(RenderTarget framebuffer) {
        //TODO render alyx-like gui over this
        ShaderInstance shader = Minecraft.getInstance().gameRenderer.blitShader;
        shader.setSampler("DiffuseSampler", framebuffer.getColorTextureId());

        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        RenderSystem.applyModelViewMatrix();

        int width = client.getWindow().getWidth();
        int height = client.getWindow().getHeight();

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._disableBlend();

        Matrix4f matrix4f = (new Matrix4f()).ortho2D((float) width, (float) (-height), 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set((new Matrix4f()).translate(0, 0, -2000.0F));
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(matrix4f);
        }

        shader.apply();
        float widthNormalized = (float) framebuffer.width / (float) width;
        float heightNormalized = (float) framebuffer.height / (float) height;
        float v = (widthNormalized / heightNormalized) / 2;

        //maintain screen's square aspect ratio
        int xOff = 0;
        int yOff = 0;

        if (width > height) {
            yOff = -(width - height) / 2;
        } else {
            xOff = -(height - width) / 2;
        }

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(xOff, height - yOff, 0.0F).setUv(0.0F, 0.0f).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(width - xOff, height - yOff, 0.0F).setUv(1, 0.0f).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(width - xOff, yOff, 0.0F).setUv(1, 1.0f).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(xOff, yOff, 0.0F).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
        BufferUploader.draw(bufferBuilder.build());
        shader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.popMatrix();
    }

    private void renderOverlay(RenderTarget framebuffer,
                               ResourceLocation texture,
                               float red,
                               float green,
                               float blue,
                               float alpha) {
        ShaderInstance shader = this.blitShader;//to eye

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstractTexture = textureManager.getTexture(texture);

        shader.setSampler("DiffuseSampler", abstractTexture.getId());

        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        RenderSystem.applyModelViewMatrix();

        int width = framebuffer.width;
        int height = framebuffer.height;

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GlStateManager.SourceFactor.SRC_ALPHA.value, GlStateManager.SourceFactor.ONE_MINUS_SRC_ALPHA.value);

        Matrix4f matrix4f = (new Matrix4f()).ortho2D((float) width, (float) -height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set((new Matrix4f()).translate(0.0F, 0.0F, -2000.0F));
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(matrix4f);
        }

        shader.apply();
        Tesselator tessellator = RenderSystem.renderThreadTesselator();//Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(0.0F, height, -90.0F).setUv(0f, 0f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(width, height, -90.0F).setUv(1f, 0f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(width, 0.0F, -90.0F).setUv(1f, 1f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(0.0F, 0.0F, -90.0F).setUv(0f, 1f).setColor(red, green, blue, alpha);

        BufferUploader.draw(bufferBuilder.build());
        shader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.popMatrix();
    }

    private void renderVignette(RenderTarget framebuffer, Entity entity) {
        WorldBorder worldBorder = this.client.level.getWorldBorder();
        float f = (float) worldBorder.getDistanceToBorder(entity);
        double d = Math.min(
                worldBorder.getLerpSpeed() * (double) worldBorder.getWarningTime() * 1000.0, Math.abs(worldBorder.getLerpTarget() - worldBorder.getSize())
        );
        double e = Math.max(worldBorder.getWarningBlocks(), d);
        if ((double) f < e) {
            f = 1.0F - (float) ((double) f / e);
        } else {
            f = 0.0F;
        }
        if (f > 0.0F) {
            f = Mth.clamp(f, 0.0F, 1.0F);
            renderOverlay(framebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 0f, 0f, 0f, f);
        } else {
            float l = LightTexture.getBrightness(entity.level().dimensionType(), entity.level().getMaxLocalRawBrightness(new BlockPos((int) entity.getX(), (int) entity.getEyeY(), (int) entity.getZ())));
            float g = Mth.clamp(1.0F - l, 0.0F, 1.0F);
            renderOverlay(framebuffer, MCOpenXRClient.id("textures/misc/vignette_vr.png"), 0f, 0f, 0f, g);
        }
    }

    private void renderPortalOverlay(RenderTarget framebuffer, float nauseaStrength) {
        if (nauseaStrength < 1.0F) {
            nauseaStrength *= nauseaStrength;
            nauseaStrength *= nauseaStrength;
            nauseaStrength = nauseaStrength * 0.8F;
        }
        ShaderInstance shader = this.blitShader;//to eye

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstractTexture = textureManager.getTexture(InventoryMenu.BLOCK_ATLAS);

        shader.setSampler("DiffuseSampler", abstractTexture.getId());

        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        RenderSystem.applyModelViewMatrix();

        int width = framebuffer.width;
        int height = framebuffer.height;

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        GlStateManager._enableBlend();

        Matrix4f matrix4f = (new Matrix4f()).ortho2D((float) width, (float) -height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set((new Matrix4f()).translate(0.0F, 0.0F, -2000.0F));
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(matrix4f);
        }

        shader.apply();
        TextureAtlasSprite textureAtlasSprite = this.client.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
        float f = textureAtlasSprite.getU0();
        float g = textureAtlasSprite.getV0();
        float h = textureAtlasSprite.getU1();
        float i = textureAtlasSprite.getV1();
        Tesselator tessellator = RenderSystem.renderThreadTesselator();//Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(0.0F, height, -90.0F).setUv(f, i).setColor(1f, 1f, 1f, nauseaStrength);
        bufferBuilder.addVertex(width, height, -90.0F).setUv(h, i).setColor(1f, 1f, 1f, nauseaStrength);
        bufferBuilder.addVertex(width, 0.0F, -90.0F).setUv(h, g).setColor(1f, 1f, 1f, nauseaStrength);
        bufferBuilder.addVertex(0.0F, 0.0F, -90.0F).setUv(f, g).setColor(1f, 1f, 1f, nauseaStrength);

        BufferUploader.draw(bufferBuilder.build());
        shader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);

        matrixStack.popMatrix();
    }
}
