package net.sorenon.config;

public class MCOpenXRConfigImpl implements MCOpenXRConfig {

    public boolean xrEnabled;

    @Override
    public boolean supportsMCXR() {
        return xrEnabled;
    }

    @Override
    public boolean dynamicPlayerHeight() {
        return xrEnabled;
    }

    @Override
    public boolean dynamicPlayerEyeHeight() {
        return xrEnabled;
    }

    @Override
    public boolean thinnerPlayerBoundingBox() {
        return xrEnabled;
    }

    @Override
    public boolean controllerRaytracing() {
        return true;
    }

    @Override
    public boolean roomscaleMovement() {
        return true;
    }

    @Override
    public boolean handBasedItemUsage() {
        return xrEnabled;
    }
}
