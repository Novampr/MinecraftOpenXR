package net.sorenon.mixin.rendering;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.sorenon.MCOpenXR;
import net.sorenon.MCOpenXRGuiManager;
import net.sorenon.MCOpenXRClient;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Redirect(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowSizeLimits(JIIII)V")
    )
    private void redirectWindowLimit(long window, int minwidth, int minheight, int maxwidth, int maxheight) {
        MCOpenXR.LOGGER.info("Bypassed glfwSetWindowSizeLimits.");
    }

    @Unique
    private final MCOpenXRGuiManager FGM = MCOpenXRClient.INSTANCE.MCOpenXRGuiManager;

    @Unique
    private final MCOpenXRGameRenderer MCOpenXRGameRenderer = MCOpenXRClient.MCXR_GAME_RENDERER;


    @ModifyVariable(method = "updateVsync", ordinal = 0, at = @At("HEAD"))
    boolean overwriteVsync(boolean v) {
        GLFW.glfwSwapInterval(0);
        return false;
    }

    @Inject(method = "getScreenWidth", at = @At("HEAD"), cancellable = true)
    void getFramebufferWidth(CallbackInfoReturnable<Integer> cir) {
        if (isCustomFramebuffer()) {
            if (MCOpenXRGameRenderer.reloadingDepth > 0) {
                var swapchain = MCOpenXRClient.OPEN_XR_STATE.session.swapchain;
                cir.setReturnValue(swapchain.getRenderWidth());
            } else {
                var mainTarget = Minecraft.getInstance().getMainRenderTarget();
                cir.setReturnValue(mainTarget.viewWidth);
            }
        }
    }

    @Inject(method = "getScreenHeight", at = @At("HEAD"), cancellable = true)
    void getFramebufferHeight(CallbackInfoReturnable<Integer> cir) {
        if (isCustomFramebuffer()) {
            if (MCOpenXRGameRenderer.reloadingDepth > 0) {
                var swapchain = MCOpenXRClient.OPEN_XR_STATE.session.swapchain;
                cir.setReturnValue(swapchain.getRenderHeight());
            } else {
                var mainTarget = Minecraft.getInstance().getMainRenderTarget();
                cir.setReturnValue(mainTarget.viewHeight);
            }
        }
    }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    void getWidth(CallbackInfoReturnable<Integer> cir) {
        getFramebufferWidth(cir);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    void getHeight(CallbackInfoReturnable<Integer> cir) {
        getFramebufferHeight(cir);
    }


    @Inject(method = "getGuiScaledHeight", at = @At("HEAD"), cancellable = true)
    void getScaledHeight(CallbackInfoReturnable<Integer> cir) {
        if (isCustomFramebuffer()) {
            cir.setReturnValue(FGM.scaledHeight);
        }
    }

    @Inject(method = "getGuiScaledWidth", at = @At("HEAD"), cancellable = true)
    void getScaledWidth(CallbackInfoReturnable<Integer> cir) {
        if (isCustomFramebuffer()) {
            cir.setReturnValue(FGM.scaledWidth);
        }
    }

    @Inject(method = "getGuiScale", at = @At("HEAD"), cancellable = true)
    void getScaleFactor(CallbackInfoReturnable<Double> cir) {
        if (isCustomFramebuffer()) {
            cir.setReturnValue(FGM.guiScale);
        }
    }

    @Inject(method = "onFocus", at = @At("HEAD"), cancellable = true)
    void preventPauseOnUnFocus(long window, boolean focused, CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    boolean isCustomFramebuffer() {
        return MCOpenXRGameRenderer.overrideWindowSize || (MCOpenXRGameRenderer.isXrMode() && MCOpenXRGameRenderer.reloadingDepth > 0);
    }
}