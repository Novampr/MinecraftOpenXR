package net.sorenon.mixin.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.sorenon.MCOpenXRClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    void cancelBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode()) {
            ci.cancel();
        }
    }
}
