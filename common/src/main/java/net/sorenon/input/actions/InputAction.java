package net.sorenon.input.actions;

import net.sorenon.openxr.OpenXRSession;

public interface InputAction {
    void sync(OpenXRSession session);
}
