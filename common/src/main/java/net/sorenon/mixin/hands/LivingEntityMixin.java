package net.sorenon.mixin.hands;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorenon.MCOpenXR;
import net.sorenon.MCOpenXRClient;
import net.sorenon.PlayOptions;
import net.sorenon.accessor.PlayerExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow public abstract InteractionHand getUsedItemHand();

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "releaseUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    void preReleaseUsing(CallbackInfo ci) {
        if (this instanceof PlayerExt playerExt && playerExt.isXR() && MCOpenXR.getCoreConfig().handBasedItemUsage()) {
            playerExt.getOverrideTransform().set(MCOpenXR.handToArm((LivingEntity)(Object)this, this.getUsedItemHand()));
        }
    }

    @Inject(method = "releaseUsingItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    void postReleaseUsing(CallbackInfo ci) {
        if (this instanceof PlayerExt playerExt && playerExt.isXR()) {
            playerExt.getOverrideTransform().set(null);
        }
    }

    @Shadow
    public abstract boolean isAlive();

    @Shadow protected abstract int increaseAirSupply(int air);

    @Unique
    private boolean isActive() {
        //noinspection ConstantConditions
        return MCOpenXRClient.MCXR_GAME_RENDERER.isXrMode() && (LivingEntity) (Object) this instanceof LocalPlayer;
    }

    @Redirect(method = "handleRelativeFrictionAndCalculateMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    public void moveRelativeLand(LivingEntity instance, float speed, Vec3 move) {
        if (isActive()) {
            Optional<Float> val = PlayOptions.walkDirection.getMCYaw();
            if (val.isPresent()) {
                Vec3 inputVector = getInputVector(move, speed, val.get());
                this.setDeltaMovement(this.getDeltaMovement().add(inputVector));
                return;
            }
        }
        this.moveRelative(speed, move);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    public void moveRelativeLiquid(LivingEntity instance, float speed, Vec3 move) {
        if (isActive() && this.isSwimming()) {
            Optional<Float> val = PlayOptions.swimDirection.getMCYaw();
            if (val.isPresent()) {
                Vec3 inputVector = getInputVector(move, speed, val.get());
                this.setDeltaMovement(this.getDeltaMovement().add(inputVector));
            } else {
                this.moveRelative(speed, move);
            }
        } else {
            this.moveRelativeLand(instance, speed, move);
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 getLookAngleFlying(LivingEntity instance) {
        if (isActive()) {
            Vec3 result = PlayOptions.flyDirection.getLookDirection();
            if (result != null) {
                return result;
            }
        }
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    public float getXRotFlying(LivingEntity instance) {
        if (isActive()) {
            Optional<Float> val = PlayOptions.flyDirection.getMCPitch();
            if (val.isPresent()) {
                return val.get();
            }
        }
        return this.getXRot();
    }

    @Unique
    private static Vec3 getInputVector(Vec3 vec3, float f, float g) {
        double d = vec3.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec32 = (d > 1.0 ? vec3.normalize() : vec3).scale((double) f);
            float h = Mth.sin(g * (float) (Math.PI / 180.0));
            float i = Mth.cos(g * (float) (Math.PI / 180.0));
            return new Vec3(vec32.x * (double) i - vec32.z * (double) h, vec32.y, vec32.z * (double) i + vec32.x * (double) h);
        }
    }

    @Inject(method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", at = @At("HEAD"), cancellable = true)
    private void overrideDims(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (((LivingEntity) (Object) this) instanceof Player player) {
            cir.setReturnValue(((PlayerExt) player).overrideDims(pose, cir.getReturnValue()));
        }
    }
}
