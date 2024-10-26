package net.sorenon.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.sorenon.JOMLUtil;
import net.sorenon.Pose;
import net.sorenon.MCOpenXRGuiManager;
import net.sorenon.MCOpenXRClient;
import net.sorenon.PlayOptions;
import net.sorenon.gui.QuickMenu;
import net.sorenon.input.actions.Action;
import net.sorenon.input.actions.SessionAwareAction;
import net.sorenon.input.actionsets.GuiActionSet;
import net.sorenon.input.actionsets.HandsActionSet;
import net.sorenon.input.actionsets.VanillaGameplayActionSet;
import net.sorenon.mixin.accessor.MouseHandlerAcc;
import net.sorenon.openxr.OpenXRInstance;
import net.sorenon.openxr.OpenXRSession;
import net.sorenon.openxr.XrException;
import net.sorenon.openxr.XrRuntimeException;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionSuggestedBinding;
import org.lwjgl.openxr.XrInteractionProfileSuggestedBinding;
import org.lwjgl.openxr.XrSessionActionSetsAttachInfo;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPointers;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class XrInput {
    public static final HandsActionSet handsActionSet = new HandsActionSet();
    public static final VanillaGameplayActionSet vanillaGameplayActionSet = new VanillaGameplayActionSet();
    public static final GuiActionSet guiActionSet = new GuiActionSet();

    private static long lastPollTime = 0;

    public static boolean teleport = false;


    private XrInput() {
    }

    //TODO registryify this
    public static void reinitialize(OpenXRSession session) throws XrException {
        OpenXRInstance instance = session.instance;

        handsActionSet.createHandle(instance);
        vanillaGameplayActionSet.createHandle(instance);
        guiActionSet.createHandle(instance);

        HashMap<String, List<Pair<Action, String>>> defaultBindings = new HashMap<>();
        handsActionSet.getDefaultBindings(defaultBindings);
        vanillaGameplayActionSet.getDefaultBindings(defaultBindings);
        guiActionSet.getDefaultBindings(defaultBindings);

        try (var stack = stackPush()) {
            for (var entry : defaultBindings.entrySet()) {
                var bindingsSet = entry.getValue();

                XrActionSuggestedBinding.Buffer bindings = XrActionSuggestedBinding.calloc(bindingsSet.size(), stack);

                for (int i = 0; i < bindingsSet.size(); i++) {
                    var binding = bindingsSet.get(i);
                    bindings.get(i).set(
                            binding.getA().getHandle(),
                            instance.getPath(binding.getB())
                    );
                }

                XrInteractionProfileSuggestedBinding suggested_binds = XrInteractionProfileSuggestedBinding.calloc(stack).set(
                        XR10.XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING,
                        NULL,
                        instance.getPath(entry.getKey()),
                        bindings
                );

                try {
                    instance.checkPanic(XR10.xrSuggestInteractionProfileBindings(instance.handle, suggested_binds), "xrSuggestInteractionProfileBindings");
                } catch (XrRuntimeException e) {
                    StringBuilder out = new StringBuilder(e.getMessage() + "\ninteractionProfile: " + entry.getKey());
                    for (var pair : bindingsSet) {
                        out.append("\n").append(pair.getB());
                    }
                    throw new XrRuntimeException(e.result, out.toString());
                }
            }

            XrSessionActionSetsAttachInfo attach_info = XrSessionActionSetsAttachInfo.calloc(stack).set(
                    XR10.XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO,
                    NULL,
                    stackPointers(vanillaGameplayActionSet.getHandle().address(), guiActionSet.getHandle().address(), handsActionSet.getHandle().address())
            );
            // Attach the action set we just made to the session
            instance.checkPanic(XR10.xrAttachSessionActionSets(session.handle, attach_info), "xrAttachSessionActionSets");
        }

        for (var action : handsActionSet.actions()) {
            if (action instanceof SessionAwareAction sessionAwareAction) {
                sessionAwareAction.createHandleSession(session);
            }
        }
    }

    /**
     * Pre-tick + Pre-render, called once every frame
     */
    public static void pollActions() {
        long time = System.nanoTime();
        if (lastPollTime == 0) {
            lastPollTime = time;
        }

        if (MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.isScreenOpen()) {
            if (guiActionSet.exit.changedSinceLastSync) {
                if (guiActionSet.exit.currentState) {
                    if (Minecraft.getInstance().screen != null) {
                        Minecraft.getInstance().screen.keyPressed(256, 0, 0);
                    }
                }
            }
        }

        if (MCOpenXRClient.INSTANCE.MCOpenXRGuiManager.isScreenOpen()) {
            return;
        }

        VanillaGameplayActionSet actionSet = vanillaGameplayActionSet;

        if (actionSet.resetPos.changedSinceLastSync) {
            if (actionSet.resetPos.currentState) {
                MCOpenXRClient.resetView();
            }
        }

        if (actionSet.teleport.changedSinceLastSync && !actionSet.teleport.currentState) {
            XrInput.teleport = true;
        }

        if (PlayOptions.smoothTurning) {
            if (Math.abs(actionSet.turn.currentState) > 0.4) {
                float delta = (time - lastPollTime) / 1_000_000_000f;

                MCOpenXRClient.stageTurn += Math.toRadians(PlayOptions.smoothTurnRate) * -Math.signum(actionSet.turn.currentState) * delta;
                Vector3f newPos = new Quaternionf().rotateLocalY(MCOpenXRClient.stageTurn).transform(MCOpenXRClient.viewSpacePoses.getStagePose().getPos(), new Vector3f());
                Vector3f wantedPos = new Vector3f(MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos());

                MCOpenXRClient.stagePosition = wantedPos.sub(newPos).mul(1, 0, 1);
            }
        } else {
            if (actionSet.turn.changedSinceLastSync) {
                float value = actionSet.turn.currentState;
                if (actionSet.turnActivated) {
                    actionSet.turnActivated = Math.abs(value) > 0.15f;
                } else if (Math.abs(value) > 0.7f) {
                    MCOpenXRClient.stageTurn += Math.toRadians(PlayOptions.snapTurnAmount) * -Math.signum(value);
                    Vector3f newPos = new Quaternionf().rotateLocalY(MCOpenXRClient.stageTurn).transform(MCOpenXRClient.viewSpacePoses.getStagePose().getPos(), new Vector3f());
                    Vector3f wantedPos = new Vector3f(MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos());

                    MCOpenXRClient.stagePosition = wantedPos.sub(newPos).mul(1, 0, 1);

                    actionSet.turnActivated = true;
                }
            }
        }

        if (actionSet.hotbar.changedSinceLastSync) {
            var value = actionSet.hotbar.currentState;
            if (actionSet.hotbarActivated) {
                actionSet.hotbarActivated = Math.abs(value) > 0.15f;
            } else if (Math.abs(value) >= 0.7f) {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance().player.getInventory().swapPaint(-value);
                actionSet.hotbarActivated = true;
            }
        }
        if (actionSet.hotbarLeft.currentState && actionSet.hotbarLeft.changedSinceLastSync) {
            if (Minecraft.getInstance().player != null) {
                int selected = Minecraft.getInstance().player.getInventory().selected;
                selected += 1;
                while (selected < 0) {
                    selected += 9;
                }
                while (selected >= 9) {
                    selected -= 9;
                }
            }
        }
        if (actionSet.hotbarRight.currentState && actionSet.hotbarRight.changedSinceLastSync) {
            if (Minecraft.getInstance().player != null) {
                int selected = Minecraft.getInstance().player.getInventory().selected;
                selected -= 1;
                while (selected < 0) {
                    selected += 9;
                }
                while (selected >= 9) {
                    selected -= 9;
                }
            }
        }

        if (actionSet.turnLeft.currentState && actionSet.turnLeft.changedSinceLastSync) {
            MCOpenXRClient.stageTurn += Math.toRadians(22);
            Vector3f newPos = new Quaternionf().rotateLocalY(MCOpenXRClient.stageTurn).transform(MCOpenXRClient.viewSpacePoses.getStagePose().getPos(), new Vector3f());
            Vector3f wantedPos = new Vector3f(MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos());

            MCOpenXRClient.stagePosition = wantedPos.sub(newPos).mul(1, 0, 1);
        }
        if (actionSet.turnRight.currentState && actionSet.turnRight.changedSinceLastSync) {
            MCOpenXRClient.stageTurn -= Math.toRadians(22);
            Vector3f newPos = new Quaternionf().rotateLocalY(MCOpenXRClient.stageTurn).transform(MCOpenXRClient.viewSpacePoses.getStagePose().getPos(), new Vector3f());
            Vector3f wantedPos = new Vector3f(MCOpenXRClient.viewSpacePoses.getPhysicalPose().getPos());

            MCOpenXRClient.stagePosition = wantedPos.sub(newPos).mul(1, 0, 1);
        }
        if (actionSet.menu.currentState && actionSet.menu.changedSinceLastSync) {
            Minecraft.getInstance().pauseGame(false);
        }

        if (actionSet.inventory.changedSinceLastSync) {
            if (!actionSet.inventory.currentState) {
                Minecraft client = Minecraft.getInstance();
                if (client.screen == null) {
                    if (client.player != null && client.gameMode != null) {
                        if (client.gameMode.isServerControlledInventory()) {
                            client.player.sendOpenInventory();
                        } else {
                            client.getTutorial().onOpenInventory();
                            client.setScreen(new InventoryScreen(client.player));
                        }
                    }
                }
            }
        }

        if (actionSet.quickmenu.changedSinceLastSync) {
            if (!actionSet.quickmenu.currentState) {
                Minecraft client = Minecraft.getInstance();
                if (client.screen == null) {
                    client.setScreen(new QuickMenu(Component.literal("Quick Menu")));
                }
            }
        }
        if (actionSet.sprint.changedSinceLastSync) {
            Minecraft client = Minecraft.getInstance();
            if (actionSet.sprint.currentState) {
                client.options.keySprint.setDown(true);
            } else {
                client.options.keySprint.setDown(false);
                if (client.player != null) {
                    client.player.setSprinting(false);
                }
            }
        }
        if (actionSet.sneak.changedSinceLastSync) {
            Minecraft client = Minecraft.getInstance();
            client.options.keyShift.setDown(actionSet.sneak.currentState);
            if (client.player != null) {
                client.player.setShiftKeyDown(actionSet.sneak.currentState);
            }
        }
        if (actionSet.attack.changedSinceLastSync) {
            Minecraft client = Minecraft.getInstance();
            client.options.keyAttack.setDown(actionSet.attack.currentState);
            if (client.player != null) {
                client.player.setSprinting(actionSet.attack.currentState);
            }
        }
        if (actionSet.use.changedSinceLastSync) {
            Minecraft client = Minecraft.getInstance();
            InputConstants.Key key = client.options.keyUse.getDefaultKey();
            if (actionSet.use.currentState) {
                KeyMapping.click(key);
                KeyMapping.set(key, true);
            } else {
                KeyMapping.set(key, false);
            }
        }

        lastPollTime = time;
    }

    /**
     * Post-tick + Pre-render, called once every frame
     */
    public static void postTick(long predictedDisplayTime) {
        MCOpenXRGuiManager FGM = MCOpenXRClient.INSTANCE.MCOpenXRGuiManager;
        MouseHandlerAcc mouseHandler = (MouseHandlerAcc) Minecraft.getInstance().mouseHandler;
        if (FGM.isScreenOpen()) {
            Pose pose = handsActionSet.gripPoses[MCOpenXRClient.getMainHand()].getUnscaledPhysicalPose();
            Vector3d pos = new Vector3d(pose.getPos());
            Vector3f dir = pose.getOrientation().rotateX((float) Math.toRadians(PlayOptions.handPitchAdjust), new Quaternionf()).transform(new Vector3f(0, -1, 0));
            Vector3d result = FGM.guiRaycast(pos, new Vector3d(dir));
            if (result != null) {
                Vector3d vec = result.sub(JOMLUtil.convert(FGM.position));
                FGM.orientation.invert(new Quaterniond()).transform(vec);
                vec.y *= ((double) FGM.guiFramebufferWidth / FGM.guiFramebufferHeight);

                vec.x /= FGM.size;
                vec.y /= FGM.size;

                mouseHandler.callOnMove(
                        Minecraft.getInstance().getWindow().getWindow(),
                        FGM.guiFramebufferWidth * (0.5 - vec.x),
                        FGM.guiFramebufferHeight * (1 - vec.y)
                );
            }
            GuiActionSet actionSet = guiActionSet;
            if (actionSet.pickup.changedSinceLastSync || actionSet.quickMove.changedSinceLastSync) {
                if (actionSet.pickup.currentState || actionSet.quickMove.currentState) {
                    mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
                } else {
                    mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
                }
            }

            if (actionSet.split.changedSinceLastSync) {
                if (actionSet.split.currentState) {
                    mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);
                } else {
                    mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0);
                }
            }
            if (actionSet.resetGUI.changedSinceLastSync && actionSet.resetGUI.currentState) {
                FGM.needsReset = true;
            }
            var scrollState = actionSet.scroll.currentState;
            //TODO replace with a better acc alg
            double sensitivity = 0.25;
            if (Math.abs(scrollState.y()) > 0.9 && scrollState.length() > 0.95) {
                mouseHandler.callOnScroll(Minecraft.getInstance().getWindow().getWindow(),
                        -scrollState.x() * sensitivity, 1.5 * Math.signum(scrollState.y()));
            } else if (Math.abs(scrollState.y()) > 0.1) {
                mouseHandler.callOnScroll(Minecraft.getInstance().getWindow().getWindow(),
                        -scrollState.x() * sensitivity, 0.1 * Math.signum(scrollState.y()));
            }
        } else {
            VanillaGameplayActionSet actionSet = vanillaGameplayActionSet;
            if (actionSet.attack.changedSinceLastSync) {
                if (actionSet.attack.currentState) {
                    mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
                }
            }
            if (!actionSet.attack.currentState) {
                mouseHandler.callOnPress(Minecraft.getInstance().getWindow().getWindow(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
            }
            if (actionSet.inventory.currentState) {
                long heldTime = predictedDisplayTime - actionSet.inventory.lastChangeTime;
                if (heldTime * 1E-09 > 1) {
                    Minecraft.getInstance().pauseGame(false);
                }
            }
        }
    }
}
