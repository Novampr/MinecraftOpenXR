/*package net.sorenon.mixin;

import net.sorenon.MCOpenXRClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static org.lwjgl.opengl.GL11C.GL_RGBA16;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;

import com.mojang.blaze3d.pipeline.RenderTarget;

@Mixin(RenderTarget.class)
public class FramebufferMixin {
    /*@ModifyConstant(method = "createBuffers", constant = @Constant(intValue = GL_RGBA8))
    int increaseColorDepth(int value) {
        if (MCOpenXRClient.OPEN_XR_STATE.session != null && MCOpenXRClient.OPEN_XR_STATE.session.swapchain.hdr) {
            return GL_RGBA16;
        } else {
            return GL_RGBA8;
        }
    }*\/
}
*/