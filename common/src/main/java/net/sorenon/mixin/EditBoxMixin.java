package net.sorenon.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.sorenon.PlayOptions;
import net.sorenon.gui.XrEditBoxScreen;
import net.sorenon.gui.XrSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {

    @Redirect(method = "moveCursor(IZ)V", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(IZ)V"))
    public void mouseClickedMixin(EditBox instance, int cursor, boolean isSelection) {
        assert Minecraft.getInstance().screen != null;

        if (Minecraft.getInstance().screen.getClass() == ChatScreen.class || PlayOptions.xrUninitialized || Minecraft.getInstance().screen.getClass() == XrSignEditScreen.class) {
            instance.setCursorPosition(cursor);
            return;
        }

        instance.setCursorPosition(cursor);
        Minecraft.getInstance().setScreen(new XrEditBoxScreen(Component.translatable("Keyboard"), Minecraft.getInstance().screen, instance));
    }
}
