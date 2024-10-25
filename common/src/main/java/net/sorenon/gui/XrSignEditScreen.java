package net.sorenon.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.sorenon.gui.keyboard.XrSignKeyboard;

public class XrSignEditScreen extends Screen {

    private final SignBlockEntity _sign;
    private final XrSignKeyboard _keyboard;
    private final boolean _isFront;

    public XrSignEditScreen(Component title, SignBlockEntity sign, boolean isFront) {
        super(title);
        _sign = sign;
        _isFront = isFront;

        this.width = Minecraft.getInstance().getWindow().getWidth();
        this.height = Minecraft.getInstance().getWindow().getHeight();

        _keyboard = new XrSignKeyboard(this, _isFront);

    }

    public void clear() {
        this.clearWidgets();
    }

    public SignBlockEntity getSign() {
        return _sign;
    }

    public void addRenderWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    protected void init() {

        _keyboard.renderKeyboard(_keyboard.getDefaultCharset(), this.width, this.height, 30);
        super.init();

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        if (_keyboard.getTextField1().isFocused()) {
            _keyboard.setActiveTextField(_keyboard.getTextField1());
        }

        if (_keyboard.getTextField2().isFocused()) {
            _keyboard.setActiveTextField(_keyboard.getTextField2());
        }

        if (_keyboard.getTextField3().isFocused()) {
            _keyboard.setActiveTextField(_keyboard.getTextField3());
        }

        if (_keyboard.getTextField4().isFocused()) {
            _keyboard.setActiveTextField(_keyboard.getTextField4());
        }
    }
}
