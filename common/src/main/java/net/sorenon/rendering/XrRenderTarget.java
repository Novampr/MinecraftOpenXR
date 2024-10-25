package net.sorenon.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.sorenon.mixin.accessor.RenderTargetAcc;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

public class XrRenderTarget extends RenderTarget {
    private final int index;

    public XrRenderTarget(int width, int height, int color, int index) {
        super(true);

        this.index = index;

        this.colorTextureId = color;

        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(width, height, Minecraft.ON_OSX);
        //MCOpenXRNativeLoader.renderImage(color, index);

        this.setClearColor(sRGBToLinear(239 / 255f), sRGBToLinear(50 / 255f), sRGBToLinear(61 / 255f), 255 / 255f);
    }

    private float sRGBToLinear(float f) {
        if (f < 0.04045f) {
            return f / 12.92f;
        } else {
            return (float) Math.pow((f + 0.055f) / 1.055f, 2.4f);
        }
    }

    @Override
    public void createBuffers(int width, int height, boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.frameBufferId = GlStateManager.glGenFramebuffers();
            if (this.useDepth) {
                this.depthBufferId = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(this.depthBufferId);
                GlStateManager._texParameter(3553, 10241, 9728);
                GlStateManager._texParameter(3553, 10240, 9728);
                GlStateManager._texParameter(3553, 34892, 0);
                GlStateManager._texParameter(3553, 10242, 33071);
                GlStateManager._texParameter(3553, 10243, 33071);
                GlStateManager._texImage2D(3553, 0, 6402, this.width, this.height, 0, 6402, 5126, null);
            }

            ((RenderTargetAcc) this).setFilterMode(9728, true);
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
            GlStateManager._texImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, null);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, index);
            if (this.useDepth) {
                GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, index);
            }
            GlStateManager._glBindFramebuffer(36160, this.frameBufferId);

            this.checkStatus();
            this.clear(getError);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }
}
