package net.sorenon.mixin.hands.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.sorenon.MCOpenXR;
import net.sorenon.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    void preUse(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerExt playerExt = ((PlayerExt) localPlayer);
        if (playerExt.isXR() && MCOpenXR.getCoreConfig().handBasedItemUsage()) {
            playerExt.getOverrideTransform().set(MCOpenXR.handToArm(localPlayer, interactionHand));
        }
    }

    @Inject(method = "performUseItemOn", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    void postUse(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerExt playerExt = ((PlayerExt) localPlayer);
        if (playerExt.isXR()) {
            playerExt.getOverrideTransform().set(null);
        }
    }
}
