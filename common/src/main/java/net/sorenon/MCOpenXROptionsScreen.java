package net.sorenon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.sorenon.input.XrInput;
import net.sorenon.openxr.OpenXRInstance;
import net.sorenon.openxr.OpenXRState;
import net.sorenon.openxr.OpenXRSystem;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openxr.XR10;

import java.util.List;

public class MCOpenXROptionsScreen extends Screen {

    @Nullable
    private final Screen previous;

    private Button reloadButton;

    public MCOpenXROptionsScreen(@Nullable Screen previous) {
        super(Component.translatable("mcxr.options.title"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        PlayOptions.load();
        this.reloadButton = this.addRenderableWidget(new Button(
                this.width / 2 - 155,
                this.height / 6 - 12 - 4 + 24,
                150,
                20,
                Component.translatable("mcxr.menu.reload"),
                button -> MCOpenXRClient.OPEN_XR_STATE.tryInitialize(),
                (mutableComponentSupplier) -> Component.empty()
        ));
        if (PlayOptions.xrUninitialized) {
            reloadButton.active = false;
        }

        this.addRenderableWidget(new Button(
                this.width / 2 + 5,
                this.height / 6 - 12 - 4 + 24,
                150,
                20,
                PlayOptions.xrUninitialized ? Component.translatable("mcxr.options.initialize") : Component.translatable("mcxr.options.uninitialize"),
                button -> {
                    PlayOptions.xrUninitialized = !PlayOptions.xrUninitialized;
                    PlayOptions.save();
                    reloadButton.active = !PlayOptions.xrUninitialized;
                    if (!PlayOptions.xrUninitialized) {
                        MCOpenXRClient.OPEN_XR_STATE.tryInitialize();
                    }
                    button.setMessage(PlayOptions.xrUninitialized ? Component.translatable("mcxr.options.initialize") : Component.translatable("mcxr.options.uninitialize"));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));

        this.addRenderableWidget(new Button(
                this.width / 2 - 100,
                this.height / 6 - 12 - 4,
                200,
                20,
                PlayOptions.xrPaused ? Component.translatable("mcxr.options.unpause") : Component.translatable("mcxr.options.pause"),
                button -> {
                    PlayOptions.xrPaused = !PlayOptions.xrPaused;
                    PlayOptions.save();
                    button.setMessage(PlayOptions.xrPaused ? Component.translatable("mcxr.options.unpause") : Component.translatable("mcxr.options.pause"));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));

        this.addRenderableWidget(new Button(
                this.width / 2 - 155,
                this.height / 6 + 54 + 12,
                150,
                20,
                Component.translatable("mcxr.options.walk_direction", PlayOptions.walkDirection.toComponent()),
                button -> {
                    PlayOptions.walkDirection = PlayOptions.walkDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(Component.translatable("mcxr.options.walk_direction", PlayOptions.walkDirection.toComponent()));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));
        this.addRenderableWidget(new Button(
                this.width / 2 - 155,
                this.height / 6 + 54 + 24 + 12,
                150,
                20,
                Component.translatable("mcxr.options.swim_direction", PlayOptions.swimDirection.toComponent()),
                button -> {
                    PlayOptions.swimDirection = PlayOptions.swimDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(Component.translatable("mcxr.options.swim_direction", PlayOptions.swimDirection.toComponent()));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));
        this.addRenderableWidget(new Button(
                this.width / 2 - 155,
                this.height / 6 + 54 + 24 * 2 + 12,
                150,
                20,
                Component.translatable("mcxr.options.fly_direction", PlayOptions.flyDirection.toComponent()),
                button -> {
                    PlayOptions.flyDirection = PlayOptions.flyDirection.iterate();
                    PlayOptions.save();
                    button.setMessage(Component.translatable("mcxr.options.fly_direction", PlayOptions.flyDirection.toComponent()));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));

        assert this.minecraft != null;
        this.addRenderableWidget(Minecraft.getInstance().options.mainHand().createButton(this.minecraft.options, this.width / 2 - 155 + 160, this.height / 6 + 54 + 12, 150));

        this.addRenderableWidget(new Button(
                this.width / 2 - 155 + 160,
                this.height / 6 + 54 + 24 + 12,
                150,
                20,
                PlayOptions.smoothTurning ? Component.translatable("mcxr.options.enable_snap_turning") : Component.translatable("mcxr.options.enable_smooth_turning"),
                button -> {
                    PlayOptions.smoothTurning = !PlayOptions.smoothTurning;
                    PlayOptions.save();
                    button.setMessage(PlayOptions.smoothTurning ? Component.translatable("mcxr.options.enable_snap_turning") : Component.translatable("mcxr.options.enable_smooth_turning"));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));


        this.addRenderableWidget(new Button(
                this.width / 2 - 155 + 160,
                this.height / 6 + 54 + 24 + 12,
                150,
                20,
                PlayOptions.smoothTurning ? Component.translatable("mcxr.options.enable_snap_turning") : Component.translatable("mcxr.options.enable_smooth_turning"),
                button -> {
                    PlayOptions.smoothTurning = !PlayOptions.smoothTurning;
                    PlayOptions.save();
                    button.setMessage(PlayOptions.smoothTurning ? Component.translatable("mcxr.options.enable_snap_turning") : Component.translatable("mcxr.options.enable_smooth_turning"));
                },
                (mutableComponentSupplier) -> Component.empty()
        ));

        if (MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode() && (
                XrInput.vanillaGameplayActionSet.indexTrackpadRight.isActive ||
                        XrInput.vanillaGameplayActionSet.indexTrackpadLeft.isActive
        )) {
            this.addRenderableWidget(new Button(
                    this.width / 2 - 155 + 160,
                    this.height / 6 + 54 + 24 * 2 + 12,
                    150,
                    20,
                    Component.translatable("mcxr.options.index_touchpad", PlayOptions.indexTouchpadState.toComponent()),
                    button -> {
                        PlayOptions.indexTouchpadState = PlayOptions.indexTouchpadState.iterate();
                        PlayOptions.save();
                        button.setMessage(Component.translatable("mcxr.options.index_touchpad", PlayOptions.indexTouchpadState.toComponent()));
                    },
                    (mutableComponentSupplier) -> Component.empty()
            ));
        }

        this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 6 + 168, 200, 20, CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.previous), (mutableComponentSupplier) -> Component.empty()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 16777215);

        int y = this.height / 6 - 4 + 24;
        int x = this.width / 2 - 155;

        MCOpenXROptionsScreen.renderStatus(this, this.font, graphics, mouseX, mouseY, x, y, 0, 60);
    }

    public static void renderStatus(Screen screen,
                                    Font font,
                                    GuiGraphics graphics,
                                    int mouseX,
                                    int mouseY,
                                    int x,
                                    int y,
                                    int fade,
                                    int wrapLength) {
        if (PlayOptions.xrUninitialized) {
            graphics.drawString(font, "MCXR Disabled", x + 1, y + 12, 16777215 | fade);
            return;
        }

        OpenXRState OPEN_XR = MCOpenXRClient.OPEN_XR_STATE;

        if (OPEN_XR.instance != null) {
            OpenXRInstance instance = OPEN_XR.instance;
            graphics.drawString(font, instance.runtimeName + " " + instance.runtimeVersionString, x + 1, y + 12, 16777215 | fade);
            y += 12;
        }

        if (OPEN_XR.session != null) {
            OpenXRSystem system = OPEN_XR.session.system;
            for (String line : wordWrap(system.systemName, wrapLength)) {
                graphics.drawString(font, line, x + 1, y + 12, 16777215 | fade);
                y += 12;
            }
            graphics.drawString(font, I18n.get("openxr.form_factor." + system.formFactor), x + 1, y + 12, 16777215 | fade);
        } else {
            graphics.drawString(font, I18n.get("mcxr.menu.session_not_created"), x + 1, y + 12, 16777215 | fade);
            y += 12;
            if (OPEN_XR.createException != null) {
                String message = OPEN_XR.createException.getMessage();
                if (OPEN_XR.createException.result == XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE) {
                    message = I18n.get("mcxr.error.form_factor_unavailable");
                }
                for (String line : wordWrap(message, wrapLength)) {
                    graphics.drawString(Minecraft.getInstance().font, line, x + 1, y + 12, 16733525 | fade);
                    y += 12;
                }
                if (mouseX > x && mouseY < y + 10 && mouseY > screen.height / 4 + 48 + 12 + 10) {
                    graphics.renderComponentTooltip(Minecraft.getInstance().font, wordWrapText(message, 40), mouseX + 14, mouseY);
                }
            }
        }
    }

    private static List<String> wordWrap(String string, int wrapLength) {
        return WordUtils.wrap(string, wrapLength, null, true).lines().toList();
    }

    private static List<Component> wordWrapText(String string, int wrapLength) {
        return WordUtils.wrap(string, wrapLength, null, true).lines().map(s -> (Component) (Component.literal(s))).toList();
    }
}
