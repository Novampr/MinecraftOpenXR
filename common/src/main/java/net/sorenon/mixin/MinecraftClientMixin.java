package net.sorenon.mixin;

import net.minecraft.client.Minecraft;
import net.sorenon.MCOpenXRClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;gameThread:Ljava/lang/Thread;", shift = At.Shift.AFTER, ordinal = 0), method = "run")
    private void onStart(CallbackInfo ci) {
        MCOpenXRClient.MCXR_GAME_RENDERER.initialize((Minecraft) (Object) this);
    }
}
