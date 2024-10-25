package net.sorenon.mixin.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.Util;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.sorenon.MCOpenXR;
import net.sorenon.XPlat;
import net.sorenon.accessor.PlayerExt;
import net.sorenon.MCOpenXRClient;
import net.sorenon.accessor.MinecraftExt;
import net.sorenon.mixin.accessor.WindowAcc;
import net.sorenon.network.XRPlayerPacket;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import net.sorenon.openxr.OpenXRState;
import net.sorenon.openxr.XrRuntimeException;
import net.sorenon.rendering.RenderPass;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openxr.XR10;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import static net.sorenon.MCOpenXRClient.GUI_ICONS_LOCATION;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> implements MinecraftExt {

    public MinecraftMixin(String string) {
        super(string);
    }

    @Shadow
    private ProfilerFiller profiler;

    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract void stop();

    @Shadow
    @Nullable
    private CompletableFuture<Void> pendingReload;

    @Shadow
    @Nullable
    private Overlay overlay;

    @Shadow
    public abstract CompletableFuture<Void> reloadResourcePacks();

    @Shadow
    @Final
    private Queue<Runnable> progressTasks;

    @Shadow
    @Final
    private DeltaTracker.Timer timer;

    @Shadow
    public abstract void tick();

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Mutable
    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Shadow
    public boolean noRender;

    @Shadow
    private boolean pause;

    @Shadow
    @Final
    private ToastComponent toast;

    @Shadow
    @Nullable
    private ProfileResults fpsPieResults;

    @Shadow
    protected abstract void renderFpsMeter(GuiGraphics graphics, ProfileResults profileResult);

    @Shadow
    private int frames;

    @Shadow
    public abstract boolean hasSingleplayerServer();

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    private long lastNanoTime;

    @Shadow
    private long lastTime;

    @Shadow
    private static int fps;

    @Shadow
    public String fpsString;

    @Shadow
    @Nullable
    private IntegratedServer singleplayerServer;

    @Shadow
    @Final
    public static boolean ON_OSX;

    @Shadow
    protected abstract void runTick(boolean tick);

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Final
    public Options options;

    @Shadow
    protected abstract void openChatScreen(String string);

    @Shadow
    public abstract void resizeDisplay();

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    public abstract void prepareForMultiplayer();

    @Unique
    private static final MCOpenXRGameRenderer XR_RENDERER = MCOpenXRClient.MCXR_GAME_RENDERER;

    @Inject(method = "run", at = @At("HEAD"))
    void start(CallbackInfo ci) {
        MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.init();
    }

    private boolean renderedNormallyLastFrame = true;

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"), method = "run")
    void loop(Minecraft minecraftClient, boolean tick) {
        OpenXRState openXRState = MCOpenXRClient.OPEN_XR_STATE;
        //TODO build a more rusty error system to handle this
        try {
            if (openXRState.loop()) {
                if (!renderedNormallyLastFrame) {
                    MCOpenXRClient.LOGGER.info("Resizing framebuffers due to XR -> Pancake transition");
                    this.resizeDisplay();
                }
                if (this.player != null && MCOpenXR.getCoreConfig().supportsMCXR()) {
                    PlayerExt acc = (PlayerExt) this.player;
                    if (acc.isXR()) {
                        XPlat.sendXRPlayerPacket(new XRPlayerPacket(false));
                        acc.setIsXr(false);
                        this.player.refreshDimensions();
                    }
                }
                //Just render normally
                runTick(tick);
                renderedNormallyLastFrame = true;
            } else {
                if (renderedNormallyLastFrame) {
                    if (this.screen != null) {
                        MCOpenXRClient.LOGGER.info("Resizing gui due to Pancake -> XR transition");
                        var fgm = MCOpenXRClient.INSTANCE.MCOpenXRGuiManager;
                        this.screen.resize((Minecraft) (Object) this, fgm.scaledWidth, fgm.scaledHeight);
                        fgm.needsReset = true;
                    }
                }
                renderedNormallyLastFrame = false;
            }
        } catch (XrRuntimeException runtimeException) {
            openXRState.session.close();
            openXRState.session = null;
            MCOpenXRClient.MCXR_GAME_RENDERER.setSession(null);

            if (runtimeException.result != XR10.XR_ERROR_SESSION_LOST) {
                openXRState.instance.close();
                openXRState.instance = null;
            }

            runtimeException.printStackTrace();
        }
    }

    @Override
    public void preRender(boolean tick, Runnable preTick) {
        this.window.setErrorSection("Pre render");
        if (this.window.shouldClose()) {
            this.stop();
        }

        if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
            CompletableFuture<Void> completableFuture = this.pendingReload;
            this.pendingReload = null;
            this.reloadResourcePacks().thenRun(() -> {
                completableFuture.complete(null);
            });
        }

        Runnable runnable;
        while ((runnable = this.progressTasks.poll()) != null) {
            runnable.run();
        }

        if (tick) {
            int i = this.timer.advanceTime(Util.getMillis(), true);
            this.profiler.push("scheduledExecutables");
            this.runAllTasks();
            this.profiler.pop();
            this.profiler.push("tick");

            for (int j = 0; j < Math.min(10, i); ++j) {
                this.profiler.incrementCounter("clientTick");
                preTick.run();
                this.tick();
            }

            this.profiler.pop();
        }
    }

    @Override
    public void doRender(boolean tick, long frameStartTime, RenderPass renderPass) {
        XR_RENDERER.renderPass = renderPass;

        if (renderPass == RenderPass.GUI) {
            MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.guiRenderTarget.bindWrite(true);
        } else {
            this.mainRenderTarget.bindWrite(true);
        }

        this.profiler.push("render");
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(16640, ON_OSX);
        FogRenderer.setupNoFog();
        this.profiler.push("display");
        RenderSystem.enableCull();
        this.profiler.pop();
        if (!this.noRender) {
            this.profiler.popPush("gameRenderer");
            this.gameRenderer.render(this.timer, tick);
            //cursor rendering
            if(this.screen != null) {
                renderCursor(new GuiGraphics((Minecraft) (Object) this, MultiBufferSource.immediate(new ByteBufferBuilder(1000))), (Minecraft) (Object) this);
            }
            if (XR_RENDERER.renderPass == RenderPass.GUI || XR_RENDERER.renderPass == RenderPass.VANILLA) {
                this.profiler.popPush("toasts");
                this.toast.render(new GuiGraphics((Minecraft) (Object) this, MultiBufferSource.immediate(new ByteBufferBuilder(1000))));
                this.profiler.pop();
            }
        }

        if (this.fpsPieResults != null) {
            this.profiler.push("fpsPie");
            this.renderFpsMeter(new GuiGraphics((Minecraft) (Object) this, MultiBufferSource.immediate(new ByteBufferBuilder(1000))), this.fpsPieResults);
            this.profiler.pop();
        }

//        this.profiler.push("blit");
        this.mainRenderTarget.unbindWrite();
        matrixStack.popMatrix();

        XR_RENDERER.renderPass = RenderPass.VANILLA;
    }

    @Override
    public void postRender() {
        GLFW.glfwPollEvents();
        RenderSystem.replayQueue();
        Tesselator.getInstance().clear();
//        GLFW.glfwSwapBuffers(window.getHandle());
        WindowAcc windowAcc = ((WindowAcc) (Object) window);
        if (window.isFullscreen() != windowAcc.getActuallyFullscreen()) {
            windowAcc.setActuallyFullscreen(window.isFullscreen());
            windowAcc.invokeUpdateFullscreen(windowAcc.getVsync());
        }
        GLFW.glfwPollEvents();

        this.window.setErrorSection("Post render");
        ++this.frames;
        boolean bl = this.hasSingleplayerServer() && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen()) && !this.singleplayerServer.isPublished();
        if (this.pause != bl) {
            this.pause = bl;
        }

        long m = Util.getNanos();
        this.lastNanoTime = m;
        this.profiler.push("fpsUpdate");

        while (Util.getMillis() >= this.lastTime + 1000L) {
            fps = this.frames;
            this.fpsString = String.format("T: %s%s%s%s B: %d", (double) this.options.framerateLimit().get(), this.options.enableVsync().get() ? " vsync" : "", this.options.graphicsMode().toString(), this.options.cloudStatus().get() == CloudStatus.OFF ? "" : (this.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius().get());
            this.lastTime += 1000L;
            this.frames = 0;
        }

        this.profiler.pop();
    }

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    void swapWithGUI(CallbackInfoReturnable<RenderTarget> cir){
        if (XR_RENDERER.guiMode) {
            cir.setReturnValue(MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.guiRenderTarget);
        }
    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"), cancellable = true)
    void cancelResize(CallbackInfo ci) {
        if (XR_RENDERER.isXrMode()) {
            ci.cancel();
        }
    }

    @Unique
    private static void renderCursor(GuiGraphics matrices, Minecraft client) {
        int mouseX = (int) ((client.mouseHandler.xpos()) * (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getWidth());
        int mouseY = (int) ((client.mouseHandler.ypos()) * (double) client.getWindow().getGuiScaledHeight() / (double) client.getWindow().getHeight());
        RenderSystem.disableDepthTest();

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        //RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
        matrices.blit(GUI_ICONS_LOCATION, mouseX-7, mouseY-7,
                0.f, 0.f,
                15, 15, 256, 256);
        RenderSystem.enableDepthTest();
    }
}
