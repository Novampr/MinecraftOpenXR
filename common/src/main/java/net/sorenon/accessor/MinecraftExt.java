package net.sorenon.accessor;

import net.sorenon.rendering.RenderPass;

public interface MinecraftExt {

    void preRender(boolean tick, Runnable preTick);

    void doRender(boolean tick, long frameStartTime, RenderPass renderPass);

    void postRender();
}
