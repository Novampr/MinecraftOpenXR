package net.sorenon;

public class MCOpenXRNativeLoader {
    static {
        try {
            System.loadLibrary("mcxr_loader");
        } catch (Throwable t) {
            System.loadLibrary("mcxr-loader");
        }
    }

    public static native long getJVMPtr();
    public static native long getApplicationActivityPtr();
    public static native void renderImage(int colorAttachment, int index);
}