package net.sorenon.gui.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import net.sorenon.gui.XrSignEditScreen;

public class XrSignKeyboard extends XrAbstract2DKeyboard {

    private final EditBox _textField1;
    private final EditBox _textField2;
    private final EditBox _textField3;
    private final EditBox _textField4;

    private EditBox _activeTextField;

    private final XrSignEditScreen _signEditScreen;

    private boolean _shift, _caps;

    private final boolean _isFront;

    public XrSignKeyboard(XrSignEditScreen signEditScreen, boolean isFront) {

        _signEditScreen = signEditScreen;
        _textField1 = new EditBox(Minecraft.getInstance().font,
                (_signEditScreen.width /3) - 160, 0, 160, 10, Component.translatable(""));
        _textField2 = new EditBox(Minecraft.getInstance().font,
                (_signEditScreen.width /3) - 160, 10, 160, 10, Component.translatable(""));
        _textField3 = new EditBox(Minecraft.getInstance().font,
                (_signEditScreen.width /3) - 160, 20, 160, 10, Component.translatable(""));
        _textField4 = new EditBox(Minecraft.getInstance().font,
                (_signEditScreen.width /3) - 160, 30, 160, 10, Component.translatable(""));

        _activeTextField = _textField1;

        _isFront = isFront;
    }

    @Override
    public void returnButton(Button instance) {
        Component[] components = new Component[]{
                Component.literal(_textField1.getValue()),
                Component.literal(_textField2.getValue()),
                Component.literal(_textField3.getValue()),
                Component.literal(_textField4.getValue())
        };

        _signEditScreen.getSign().setText(
                new SignText(
                        components,
                        components,
                        _signEditScreen.getSign().getText(_isFront).getColor(),
                        _signEditScreen.getSign().getText(_isFront).hasGlowingText()
                ),
                _isFront
        );

        _signEditScreen.onClose();
    }

    public EditBox getTextField1() {
        return _textField1;
    }

    public EditBox getTextField2() {
        return _textField2;
    }

    public EditBox getTextField3() {
        return _textField3;
    }

    public EditBox getTextField4() {
        return _textField4;
    }

    public EditBox getActiveTextField() {
        return _activeTextField;
    }

    public void setActiveTextField(EditBox _activeTextField) {
        this._activeTextField = _activeTextField;
    }

    @Override
    public void backSpaceButton(Button instance) {
        _activeTextField.setValue(removeLastChar(_activeTextField.getValue()));
    }

    @Override
    public void spaceButton(Button button) {
        _activeTextField.setValue(_activeTextField.getValue() + " ");
    }

    @Override
    public void tabButton(Button button) {
        _activeTextField.setValue(_activeTextField.getValue() + "    ");
    }

    @Override
    public void shiftButton(Button button) {
        _shift = !_shift;
        this._signEditScreen.clear();
        renderKeyboard(_shift? this.getShiftCharset() : _caps? this.getCapsCharset() : this.getDefaultCharset(), _signEditScreen.width, _signEditScreen.height, 30);
    }

    @Override
    public void capsButton(Button button) {
        _caps = !_caps;
        this._signEditScreen.clear();
        renderKeyboard(_shift? this.getShiftCharset() : _caps? this.getCapsCharset() : this.getDefaultCharset(), _signEditScreen.width, _signEditScreen.height, 30);
    }

    @Override
    public void letterButton(Button instance) {
        String stringText = _activeTextField.getValue() + instance.getMessage().getString();
        _activeTextField.setValue(stringText);
    }

    @Override
    public void renderKey(Button key) {
        _signEditScreen.addRenderWidget(key);
    }

    @Override
    public void afterRender() {
        _signEditScreen.addRenderWidget(_textField1);
        _signEditScreen.addRenderWidget(_textField2);
        _signEditScreen.addRenderWidget(_textField3);
        _signEditScreen.addRenderWidget(_textField4);
    }
}
