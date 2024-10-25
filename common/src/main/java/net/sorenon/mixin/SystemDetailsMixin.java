package net.sorenon.mixin;

import net.minecraft.SystemReport;
import net.sorenon.MCOpenXRClient;
import net.sorenon.openxr.OpenXRInstance;
import net.sorenon.openxr.OpenXRSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemReport.class)
public abstract class SystemDetailsMixin {

    @Shadow
    public abstract void setDetail(String name, String value);

    @Inject(method = "<init>", at = @At("RETURN"))
    void appendMCXR(CallbackInfo ci) {
        OpenXRInstance instance = MCOpenXRClient.OPEN_XR_STATE.instance;
        OpenXRSession session = MCOpenXRClient.OPEN_XR_STATE.session;

        if (instance == null) {
            this.setDetail("XR", "No instance");
        } else {
            this.setDetail("XR", "Instance created");
            this.setDetail("Runtime Name", instance.runtimeName);
            this.setDetail("Runtime Version", instance.runtimeVersionString);

            if (session == null) {
                this.setDetail("XR", "Session not running");
            } else {
                this.setDetail("XR", "Session running");
                this.setDetail("Headset Name", session.system.systemName);
                this.setDetail("Headset Vendor", String.valueOf(session.system.vendor));
                this.setDetail("Headset Orientation Tracking", String.valueOf(session.system.orientationTracking));
                this.setDetail("Headset Position Tracking", String.valueOf(session.system.positionTracking));
                this.setDetail("Headset Max Width", String.valueOf(session.system.maxWidth));
                this.setDetail("Headset Max Height", String.valueOf(session.system.maxHeight));
                this.setDetail("Headset Max Layer Count", String.valueOf(session.system.maxLayerCount));
            }
        }
    }
}
