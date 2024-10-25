package net.sorenon.mixin.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.sorenon.MCOpenXRClient;
import net.sorenon.openxr.MCOpenXRGameRenderer;
import net.sorenon.rendering.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final MCOpenXRGameRenderer XR_RENDERER = MCOpenXRClient.MCXR_GAME_RENDERER;

    /**
     * Skip rendering the vignette then setup the GUI rendering state
     */
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    void cancelRenderVignette(GuiGraphics graphics, Entity entity, CallbackInfo ci) {
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    void cancelRenderCrosshair(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {

        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    void cancelRenderPortal(GuiGraphics graphics, float nauseaStrength, CallbackInfo ci) {
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    void cancelRenderTex(GuiGraphics graphics, ResourceLocation texture, float opacity, CallbackInfo ci) {
        if (XR_RENDERER.renderPass != RenderPass.VANILLA) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    void irisGuiFix(CallbackInfo ci) {
        RenderSystem.enableDepthTest();
    }
}
