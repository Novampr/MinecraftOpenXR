package net.sorenon.openxr;

import net.minecraft.client.Minecraft;
import net.sorenon.MCOpenXRClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.*;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

public class OpenXRSystem {
    private static final Logger LOGGER = LogManager.getLogger("MCOpenXR");

    public final OpenXRInstance instance;
    public final int formFactor;
    public final long handle;

    public final String systemName;
    public final int vendor;
    public final boolean orientationTracking;
    public final boolean positionTracking;
    public final int maxWidth;
    public final int maxHeight;
    public final int maxLayerCount;

    public OpenXRSystem(OpenXRInstance instance, int formFactor, long handle) {
        this.instance = instance;
        this.formFactor = formFactor;
        this.handle = handle;

        try (var stack = stackPush()) {
            if (MCOpenXRClient.android) {
                XrGraphicsRequirementsOpenGLESKHR graphicsRequirements = XrGraphicsRequirementsOpenGLESKHR.calloc(stack).type(KHROpenGLESEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_ES_KHR);
                instance.checkPanic(KHROpenGLESEnable.xrGetOpenGLESGraphicsRequirementsKHR(instance.handle, handle, graphicsRequirements), "xrGetOpenGLGraphicsRequirementsKHR");
            } else {
                XrGraphicsRequirementsOpenGLKHR graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.calloc(stack).type(KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR);
                instance.checkPanic(KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR(instance.handle, handle, graphicsRequirements), "xrGetOpenGLGraphicsRequirementsKHR");
            }

            XrSystemProperties systemProperties = XrSystemProperties.calloc(stack).type(XR10.XR_TYPE_SYSTEM_PROPERTIES);
            instance.checkPanic(XR10.xrGetSystemProperties(instance.handle, handle, systemProperties), "xrGetSystemProperties");
            XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
            XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();

            systemName = memUTF8(memAddress(systemProperties.systemName()));
            vendor = systemProperties.vendorId();
            orientationTracking = trackingProperties.orientationTracking();
            positionTracking = trackingProperties.positionTracking();
            maxWidth = graphicsProperties.maxSwapchainImageWidth();
            maxHeight = graphicsProperties.maxSwapchainImageHeight();
            maxLayerCount = graphicsProperties.maxLayerCount();

            LOGGER.info(String.format("Found device with id: %d", handle));
            LOGGER.info(String.format("Headset Name:%s Vendor:%d ", systemName, vendor));
            LOGGER.info(String.format("Headset Orientation Tracking:%b Position Tracking:%b ", orientationTracking, positionTracking));
            LOGGER.info(String.format("Headset Max Width:%d Max Height:%d Max Layer Count:%d ", maxWidth, maxHeight, maxLayerCount));
        }
    }

    public Struct createOpenGLBinding(MemoryStack stack) {
        try {
            long window = Minecraft.getInstance().getWindow().getWindow();
            long eglDisplayPtr = GLFWNativeEGL.glfwGetEGLDisplay();
            long eglConfigPtr = GLFWNativeEGL.glfwGetEGLConfig(window);
            long eglContextPtr = GLFWNativeEGL.glfwGetEGLContext(window);

            if (MCOpenXRClient.android) {
                return XrGraphicsBindingOpenGLESAndroidKHR.calloc(stack).set(
                        KHROpenGLESEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_ES_ANDROID_KHR,
                        NULL,
                        eglDisplayPtr,
                        eglConfigPtr,
                        eglContextPtr
                );
            } else {
                return XrGraphicsBindingOpenGLWin32KHR.calloc(stack).set(
                        KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR,
                        NULL,
                        eglDisplayPtr,
                        eglConfigPtr
                );
            }
        } catch(Exception e)  {
            e.printStackTrace();
        }
        throw new IllegalStateException("Could not get the classes needed by reflection!");
    }
}
