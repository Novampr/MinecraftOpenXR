package net.sorenon.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.sorenon.MCOpenXR;
import net.sorenon.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
    void overrideEyeHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
        if (!MCOpenXR.getCoreConfig().dynamicPlayerEyeHeight()) {
            return;
        }

        if (this instanceof PlayerExt acc && acc.isXR()) {
            cir.setReturnValue(acc.getHeadPose().pos.y);
        }
    }
}
