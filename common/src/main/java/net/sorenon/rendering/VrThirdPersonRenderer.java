/*package net.sorenon.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.sorenon.MCOpenXRClient;
import net.sorenon.PlayOptions;
import net.sorenon.Pose;
import net.sorenon.input.XrInput;
import net.tr7zw.MapRenderer;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
import static net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
import static net.sorenon.JOMLUtil.convert;

public class VrThirdPersonRenderer {
    public static void transformToHand(PoseStack matrices, int hand, float tickDelta) {
        Pose pose = XrInput.handsActionSet.gripPoses[hand].getMinecraftPose();
        Vec3 gripPos = convert(pose.getPos());
        Vector3f eyePos = ((RenderPass.XrWorld) XR_RENDERER.renderPass).eyePoses.getMinecraftPose().getPos();

        //Transform to controller
        matrices.translate(gripPos.x - eyePos.x(), gripPos.y - eyePos.y(), gripPos.z - eyePos.z());
        matrices.mulPose(pose.getOrientation());

        //Apply adjustments
        matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
        matrices.scale(0.4f, 0.4f, 0.4f);

        float scale = MCOpenXRClient.getCameraScale(tickDelta);
        matrices.scale(scale, scale, scale);

        matrices.translate(0, 1 / 16f, -1.5f / 16f);
        matrices.mulPose(Axis.XP.rotationDegrees(PlayOptions.handPitchAdjust));

        if (hand == MCOpenXRClient.getMainHand()) {
            float swing = -0.1f * Mth.sin((float) (Math.sqrt(Minecraft.getInstance().player.getAttackAnim(tickDelta)) * Math.PI * 2));
            matrices.translate(0, 0, swing);
        }
    }

    public static void render(ServerPlayer player,
                              int light,
                              PoseStack matrices,
                              MultiBufferSource consumers,
                              float deltaTick) {
        //Render held items
        for (int handIndex = 0; handIndex < 2; handIndex++) {
            if (!XrInput.handsActionSet.grip.isActive[handIndex]) {
                continue;
            }

            ItemStack stack = handIndex == 0 ? player.getOffhandItem() : player.getMainHandItem();
            if (player.getMainArm() == HumanoidArm.LEFT) {
                stack = handIndex == 1 ? player.getOffhandItem() : player.getMainHandItem();
            }

            if (!stack.isEmpty()) {
                matrices.pushPose();
                transformToHand(matrices, handIndex, deltaTick);

                if (handIndex == MCOpenXRClient.getMainHand()) {
                    float swing = -0.6f * Mth.sin((float) (Math.sqrt(player.getAttackAnim(deltaTick)) * Math.PI * 2));
                    matrices.mulPose(Axis.XP.rotation(swing));
                }

                if (stack.getItem() == Items.CROSSBOW) {
                    float f = handIndex == 0 ? -1 : 1;
                    matrices.translate(f * -1.5 / 16f, 0, 0);

                    matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(0)
                            .m10(0).m11(1).m12(0).m13(f * Math.toRadians(15))
                            .m20(0).m21(0).m22(1).m23(0)
                            .m30(0).m31(0).m32(0).m33(1));
                }

                if (stack.getItem() == Items.TRIDENT && player.getUseItem() == stack) {
                    float k = (float) stack.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - deltaTick + 1);
                    float l = Math.min(k / 10, 1);
                    if (l > 0.1F) {
                        float m = Mth.sin((k - 0.1f) * 1.3f);
                        float n = l - 0.1f;
                        float o = m * n;
                        matrices.translate(0, o * 0.004, 0);
                    }
                    matrices.translate(0, 0, l * 0.2);
                    matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(Math.toRadians(90))
                            .m10(0).m11(1).m12(0).m13(0)
                            .m20(0).m21(0).m22(1).m23(0)
                            .m30(0).m31(0).m32(0).m33(1));
                }

                if (stack.getItem() == Items.FILLED_MAP) {
                    MapRenderer.renderFirstPersonMap(matrices, consumers, light, stack, false, handIndex== 0);
                }
                else {
                    matrices.scale(1.5f,1.5f,1.5f);
                    //held item animations
                    InteractionHand curHand= handIndex == 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    if(player.getUsedItemHand()==curHand && player.getUseItemRemainingTicks() > 0 && player.isUsingItem()) {
                        switch (stack.getUseAnimation()) {
                            case EAT:
                            case DRINK:
                                float f = (float)player.getUseItemRemainingTicks() - deltaTick + 1.0F;
                                float g = f / (float)stack.getUseDuration(player);
                                if (g < 0.8F) {
                                    float h = Mth.abs(Mth.cos(f / 4.0F * 3.1415927F) * -0.07F);
                                    matrices.translate(0.0, h, 0.0);
                                }
                                matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(Math.toRadians(20))
                                        .m10(0).m11(1).m12(0).m13(0)
                                        .m20(0).m21(0).m22(1).m23(0)
                                        .m30(0).m31(0).m32(0).m33(1));
                                break;
                            case BLOCK: //hacky shield pose fix
                                matrices.translate((2 * handIndex - 1) * -0.2 - 0.0465, 0.06 * (1 - handIndex), 0);
                                matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(0)
                                        .m10(0).m11(1).m12(0).m13(0)
                                        .m20(0).m21(0).m22(1).m23(Math.toRadians((2 * handIndex - 1) * -3))
                                        .m30(0).m31(0).m32(0).m33(1));
                                matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(0)
                                        .m10(0).m11(1).m12(0).m13(Math.toRadians((2 * handIndex - 1) * 45))
                                        .m20(0).m21(0).m22(1).m23(0)
                                        .m30(0).m31(0).m32(0).m33(1));
                                matrices.mulPose((new Matrix4f()).m00(1).m01(0).m02(0).m03(Math.toRadians(-50))
                                        .m10(0).m11(1).m12(0).m13(0)
                                        .m20(0).m21(0).m22(1).m23(0)
                                        .m30(0).m31(0).m32(0).m33(1));
                        }
                    }

                    Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                            player,
                            stack,
                            handIndex == 0 ? THIRD_PERSON_LEFT_HAND : THIRD_PERSON_RIGHT_HAND,
                            handIndex == 0,
                            matrices,
                            consumers,
                            light
                    );
                }

                matrices.popPose();
            }

            //Draw hand
            matrices.pushPose();

            transformToHand(matrices, handIndex, deltaTick);

            matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(180.0F));

            matrices.translate(-2 / 16f, -12 / 16f, 0);

            matrices.pushPose();
            ModelPart armModel;
            if (player.getGameProfile().getProperties().) {
                armModel = this.slimArmModel[handIndex];
            } else {
                armModel = this.armModel[handIndex];
            }

            VertexConsumer consumer = consumers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
            armModel.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
            matrices.popPose();

            matrices.popPose();

            consumers.getBuffer(RenderType.LINES); //Hello I'm a hack ;)
        }
    }
}
*/