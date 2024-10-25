package net.sorenon.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.sorenon.PlayOptions;
import net.sorenon.gui.keyboard.XrChatKeyboard;

public class XrChatScreen extends ChatScreen {

    private XrChatKeyboard _keyboard;

    public XrChatScreen(String string) {
        super(string);

    }

    public void clear() {
        this.clearWidgets();
    }

    public void addRenderWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    protected void init() {

        super.init();
        
        if (!PlayOptions.xrUninitialized) {
            _keyboard = new XrChatKeyboard(this.input, this, 30);
            _keyboard.renderKeyboard(_keyboard.getDefaultCharset(), this.width, this.height, 30);
        }

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
    }
}
