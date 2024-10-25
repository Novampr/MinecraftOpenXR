package net.sorenon.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class QuickMenu extends Screen {

    public QuickMenu(Component component) {
        super(component);
    }

    private void renderMenuButtons(GuiGraphics graphics, int mouseX, int mouseY, float delta) {

        ArrayList<Button> QuickMenuButtons = new ArrayList<>();

        QuickMenuButtons.add(new Button((this.width/2) - 25, this.height/2, 70, 20, Component.literal("QuickChat"), (button ) -> {
            Minecraft.getInstance().setScreen(new QuickActions("QuickChat"));
        }, (mutableComponentSupplier) -> Component.empty()));

        QuickMenuButtons.add(new Button((this.width/2) - 25, this.height/2, 70, 20, Component.literal("Chat"), (button ) -> {
            Minecraft.getInstance().setScreen(new XrChatScreen(""));
        }, (mutableComponentSupplier) -> Component.empty()));

        for (int i = 0; i < QuickMenuButtons.size(); i++) {
            Button QuickMenuButton = QuickMenuButtons.get(i);

            QuickMenuButton.setX((this.width / 2) - (QuickMenuButton.getWidth()/2));
            QuickMenuButton.setY((this.height / 3) + (i*30));

            addRenderableWidget(QuickMenuButton);
        }

    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderMenuButtons(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
    }
}
