package net.sorenon.mixin.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.sorenon.MCOpenXRClient;
import net.sorenon.input.XrInput;
import net.sorenon.mixin.accessor.MouseHandlerAcc;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void forceShiftKey(long handle, int code, CallbackInfoReturnable<Boolean> cir) {
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.isScreenOpen() && XrInput.guiActionSet.quickMove.currentState) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * @author Sorenon
     * @Reason I doubt any other mods mess with this and if they do we will probably need to create a dedicated compat mixin
     */
    @Overwrite
    public static void setupMouseCallbacks(long window,
                                           GLFWCursorPosCallbackI gLFWCursorPosCallbackI,
                                           GLFWMouseButtonCallbackI gLFWMouseButtonCallbackI,
                                           GLFWScrollCallbackI gLFWScrollCallbackI,
                                           GLFWDropCallbackI gLFWDropCallbackI) {
        Minecraft minecraft = Minecraft.getInstance();
        MouseHandlerAcc mouseHandler = (MouseHandlerAcc) minecraft.mouseHandler;

        GLFW.glfwSetCursorPosCallback(window, (lx, d, e) -> minecraft.execute(() -> {
            if (!MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode()) {
                mouseHandler.callOnMove(lx, d, e);
            }
        }));
        GLFW.glfwSetMouseButtonCallback(window, (lx, i, j, k) -> minecraft.execute(() -> {
            if (!MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode()) {
                mouseHandler.callOnPress(lx, i, j, k);
            }
        }));
        GLFW.glfwSetScrollCallback(window, (lx, d, e) -> minecraft.execute(() -> {
            if (!MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode()) {
                mouseHandler.callOnScroll(lx, d, e);
            }
        }));
        GLFW.glfwSetDropCallback(window, gLFWDropCallbackI);
    }
}
