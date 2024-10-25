package net.sorenon.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderTarget.class)
public interface RenderTargetAcc {
    @Accessor
    void setColorTextureId(int colorAttachment);

    @Invoker("setFilterMode")
    void setFilterMode(int filter, boolean forceApply);
}
