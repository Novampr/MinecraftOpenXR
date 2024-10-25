package net.sorenon.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuickActions extends ChatScreen {

    private final static Logger LOGGER = LogManager.getLogger("MCOpenXR");

    public QuickActions(String string) {
        super(string);
    }

    @Override
    protected void init() {

        super.init();

        Minecraft.getInstance().gui.getChat().clearMessages(true);

        String[] quickChat;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("quickchat.conf"));
            ArrayList<String> quicklist = new ArrayList<>();
            String line;
            while (null != (line = reader.readLine())) {
                quicklist.add(line);
            }

            reader.close();
            quickChat = quicklist.toArray(new String[]{});
        } catch (IOException e) {
            LOGGER.error(e);
            quickChat = new String[] {
                    "Hey there!", "How are you?", "I'm good",
                    "Please sleep!", "I have Phantoms!",
                    "Ready to play?", "Ready when you are!",
                    "MinecraftOpenXR", "Which VR headset do you use?",

                    "Meta Quest 1", "Meta Quest 2", "Meta Quest 3", "Meta Quest Pro",
                    "Valve Index", "HTC Vive", "Meta Rift",

                    "I'm lagging!", "Wait for me!", "Have fun!", "Where are you?",
                    "AFK", "BRB", "I'm back", "/home", "/sethome",
                    "/spawn", "/gamemode creative", "/gamemode survival"
            };
        }

        List<ButtonSupplier> buttonSuppliers = new ArrayList<>();
        List<String> words = new ArrayList<>();

        for (String quickChatThing : quickChat) {
            buttonSuppliers.add((buttonX, buttonY, buttonWidth, buttonHeight) ->
                    new Button(buttonX, buttonY, buttonWidth, buttonHeight, Component.literal(quickChatThing), (button -> {
                if (quickChatThing.startsWith("/")) {
                    Minecraft.getInstance().player.connection.sendCommand(quickChatThing.substring(1));
                } else {
                    Minecraft.getInstance().player.connection.sendChat(quickChatThing);
                }
            }), (mutableComponentSupplier) -> Component.empty()));
            words.add(quickChatThing);
        }

        /*
        addActionButton(buttonSuppliers, words, "", (button -> {

        }));
        */

        addActionButton(buttonSuppliers, words, "Send Coordinates", (button -> {
            LocalPlayer player = Minecraft.getInstance().player;
            player.connection.sendChat("My coordinates are " + player.getOnPos().toShortString());
        }));

        addActionButton(buttonSuppliers, words, "Clear Chat", (button -> {
            Minecraft.getInstance().gui.getChat().clearMessages(false);
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Successfully cleared chat!").withColor(5635925));
        }));

        for (int i = 0, j = 0, k = 0; i < buttonSuppliers.size(); i++) {
            ButtonSupplier buttonSupplier = buttonSuppliers.get(i);
            String word = words.get(i);
            int buttonX = 170 * j;
            int buttonY = 25 * k + 10;
            int buttonWidth = 8 * word.length();
            int buttonHeight = 20;

            this.addRenderableWidget(buttonSupplier.get(buttonX, buttonY, buttonWidth, buttonHeight));

            if (i % 8 == 0 && i != 0) {
                j++;
                k = 0;
            } else {
                k++;
            }
        }
    }

    private void addActionButton(List<ButtonSupplier> buttonSuppliers, List<String> words, String name, Button.OnPress callback) {
        buttonSuppliers.add((buttonX, buttonY, buttonWidth, buttonHeight) ->
                new Button(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight,
                        Component.literal("ACTION: " + name),
                        callback,
                        (mutableComponentSupplier) -> Component.empty()
                )
        );
        words.add("ACTION: " + name);
    }

    private interface ButtonSupplier {
        Button get(int buttonX, int buttonY, int buttonWidth, int buttonHeight);
    }
}
