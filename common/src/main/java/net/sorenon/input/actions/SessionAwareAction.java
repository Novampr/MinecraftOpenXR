package net.sorenon.input.actions;

import net.sorenon.openxr.OpenXRSession;
import net.sorenon.openxr.XrException;

public interface SessionAwareAction {

    void createHandleSession(OpenXRSession session) throws XrException;

    void destroyHandleSession();
}
