package net.sorenon.mixin;

import com.mojang.blaze3d.platform.GLX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GLX.class)
public class GLXMixin {
    /**
     * @author Nova
     * @reason Seems to have issues for some reason.
     */
    @Overwrite(remap = false)
    public static String getOpenGLVersionString() {
        return "VERSION HERE";
    }
}
