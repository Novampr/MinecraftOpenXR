package net.sorenon.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sorenon.MCOpenXROptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init()V", at = @At("HEAD"))
    void init(CallbackInfo ci) {
        int y = this.height / 4 + 48 + -16;
        this.addRenderableWidget(new Button(
                this.width / 2 + 104,
                y,
                90,
                20,
                Component.translatable("mcxr.options.title"),
                button -> this.minecraft.setScreen(new MCOpenXROptionsScreen(this)),
                (mutableComponentSupplier) -> Component.empty()));
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    void render(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int y = this.height / 4 + 48 + -16 + 12;
        int x = this.width / 2 + 104;

        MCOpenXROptionsScreen.renderStatus(this, this.font, graphics, mouseX, mouseY, x, y, 0, 20);
    }
}
