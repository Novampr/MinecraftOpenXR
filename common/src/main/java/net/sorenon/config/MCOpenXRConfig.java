package net.sorenon.config;

public interface MCOpenXRConfig {
    boolean supportsMCXR();

    boolean dynamicPlayerHeight();

    boolean dynamicPlayerEyeHeight();

    boolean thinnerPlayerBoundingBox();

    boolean controllerRaytracing();

    boolean roomscaleMovement();

    boolean handBasedItemUsage();
}
