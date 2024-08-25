/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.render;

import com.google.common.base.Predicates;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack;
import net.ccbluex.liquidbounce.features.module.modules.misc.OverrideRaycast;
import net.ccbluex.liquidbounce.features.module.modules.player.Reach;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.CameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.NoHurtCam;
import net.ccbluex.liquidbounce.features.module.modules.render.Tracers;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.render.ActiveRenderInfo;
import net.minecraft.client.render.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.client.render.GlStateManager.rotate;
import static net.minecraft.client.render.GlStateManager.translate;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

@Mixin(EntityRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityRenderer {

    @Shadow
    private Entity pointedEntity;

    @Shadow
    private Minecraft mc;

    @Shadow
    private float thirdPersonDistanceTemp;

    @Shadow
    private float thirdPersonDistance;

    @Shadow
    private boolean cloudFog;

    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new Render3DEvent(partialTicks));
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void injectHurtCameraEffect(CallbackInfo callbackInfo) {
        if (NoHurtCam.INSTANCE.handleEvents()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3d;distanceTo(Lnet/minecraft/util/Vec3d;)D"), cancellable = true)
    private void cameraClip(float partialTicks, CallbackInfo callbackInfo) {
        if (CameraClip.INSTANCE.handleEvents()) {
            callbackInfo.cancel();

            Entity entity = mc.getRenderViewEntity();
            float f = entity.getEyeHeight();

            if (entity instanceof LivingEntity && ((LivingEntity) entity).isPlayerSleeping()) {
                f += 1;
                translate(0F, 0.3F, 0f);

                if (!mc.gameSettings.debugCamEnable) {
                    BlockPos blockPos = new BlockPos(entity);
                    IBlockState blockState = mc.world.getBlockState(blockPos);
                    ForgeHooksClient.orientBedCamera(mc.world, blockPos, blockState, entity);

                    rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180f, 0f, -1f, 0f);
                    rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1f, 0f, 0f);
                }
            } else if (mc.gameSettings.thirdPersonView > 0) {
                double d3 = thirdPersonDistanceTemp + (thirdPersonDistance - thirdPersonDistanceTemp) * partialTicks;

                if (mc.gameSettings.debugCamEnable) {
                    translate(0f, 0f, (float) (-d3));
                } else {
                    float f1 = entity.rotationYaw;
                    float f2 = entity.rotationPitch;

                    if (mc.gameSettings.thirdPersonView == 2) f2 += 180f;

                    if (mc.gameSettings.thirdPersonView == 2) rotate(180f, 0f, 1f, 0f);

                    rotate(entity.rotationPitch - f2, 1f, 0f, 0f);
                    rotate(entity.rotationYaw - f1, 0f, 1f, 0f);
                    translate(0f, 0f, (float) (-d3));
                    rotate(f1 - entity.rotationYaw, 0f, 1f, 0f);
                    rotate(f2 - entity.rotationPitch, 1f, 0f, 0f);
                }
            } else translate(0f, 0f, -0.1F);

            if (!mc.gameSettings.debugCamEnable) {
                float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180f;
                float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                float roll = 0f;
                if (entity instanceof EntityAnimal) {
                    EntityAnimal entityanimal = (EntityAnimal) entity;
                    yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180f;
                }

                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.world, entity, partialTicks);
                EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup((EntityRenderer) (Object) this, entity, block, partialTicks, yaw, pitch, roll);
                MinecraftForge.EVENT_BUS.post(event);
                rotate(event.roll, 0f, 0f, 1f);
                rotate(event.pitch, 1f, 0f, 0f);
                rotate(event.yaw, 0f, 1f, 0f);
            }

            translate(0f, -f, 0f);
            double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double d1 = entity.prevY + (entity.posY - entity.prevY) * partialTicks + f;
            double d2 = entity.prevZ + (entity.posZ - entity.prevZ) * partialTicks;
            cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.BEFORE))
    private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo) {
        if (Tracers.INSTANCE.handleEvents()) {
            glPushMatrix();
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.AFTER))
    private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo) {
        if (Tracers.INSTANCE.handleEvents()) {
            glPopMatrix();
        }
    }

    /**
     * @author CCBlueX
     */
    @Inject(method = "getMouseOver", at = @At("HEAD"), cancellable = true)
    private void getMouseOver(float p_getMouseOver_1_, CallbackInfo ci) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.world != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;

            final Reach reach = Reach.INSTANCE;

            double d0 = reach.handleEvents() ? reach.getMaxRange() : mc.interactionManager.getBlockReachDistance();
            Vec3d Vec3d = entity.getPositionEyes(p_getMouseOver_1_);
            Rotation rotation = new Rotation(mc.player.yaw, mc.player.pitch);
            Vec3d Vec3d1 = RotationUtils.INSTANCE.getVectorForRotation(RotationUtils.INSTANCE.getCurrentRotation() != null && OverrideRaycast.INSTANCE.shouldOverride() ? RotationUtils.INSTANCE.getCurrentRotation() : rotation);
            double p_rayTrace_1_ = (reach.handleEvents() ? reach.getBuildReach() : d0);
            Vec3d Vec3d2 = Vec3d.addVector(Vec3d1.xCoord * p_rayTrace_1_, Vec3d1.yCoord * p_rayTrace_1_, Vec3d1.zCoord * p_rayTrace_1_);
            mc.objectMouseOver = entity.world.rayTrace(Vec3d, Vec3d2, false, false, true);
            double d1 = d0;
            boolean flag = false;
            if (mc.interactionManager.extendedReach()) {
                // d0 = 6;
                d1 = 6;
            } else if (d0 > 3) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(Vec3d);
            }

            if (reach.handleEvents()) {
                double p_rayTrace_1_2 = reach.getBuildReach();
                Vec3d Vec3d22 = Vec3d.addVector(Vec3d1.xCoord * p_rayTrace_1_2, Vec3d1.yCoord * p_rayTrace_1_2, Vec3d1.zCoord * p_rayTrace_1_2);
                final MovingObjectPosition movingObjectPosition = entity.world.rayTrace(Vec3d, Vec3d22, false, false, true);

                if (movingObjectPosition != null) d1 = movingObjectPosition.hitVec.distanceTo(Vec3d);
            }

            pointedEntity = null;
            Vec3d Vec3d3 = null;
            List<Entity> list = mc.world.getEntities(Entity.class, Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith() && p_apply_1_ != entity));
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.getCollisionBorderSize();

                final ArrayList<Box> boxes = new ArrayList<>();
                boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));

                Backtrack.INSTANCE.loopThroughBacktrackData(entity1, () -> {
                    boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));
                    return false;
                });

                for (final Box Box : boxes) {
                    MovingObjectPosition movingobjectposition = Box.calculateIntercept(Vec3d, Vec3d2);
                    if (Box.isVecInside(Vec3d)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1;
                            Vec3d3 = movingobjectposition == null ? Vec3d : movingobjectposition.hitVec;
                            d2 = 0;
                        }
                    } else if (movingobjectposition != null) {
                        double d3 = Vec3d.distanceTo(movingobjectposition.hitVec);
                        if (d3 < d2 || d2 == 0) {
                            if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0) {
                                    pointedEntity = entity1;
                                    Vec3d3 = movingobjectposition.hitVec;
                                }
                            } else {
                                pointedEntity = entity1;
                                Vec3d3 = movingobjectposition.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && Vec3d.distanceTo(Vec3d3) > (reach.handleEvents() ? reach.getCombatReach() : 3)) {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, Objects.requireNonNull(Vec3d3), null, new BlockPos(Vec3d3));
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, Vec3d3);
                if (pointedEntity instanceof LivingEntity || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }

            mc.mcProfiler.endSection();
        }

        ci.cancel();
    }

    /**
     * Properly implement the confusion option from AntiBlind module
     */
    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean injectAntiBlindA(EntityPlayerSP instance, Potion potion) {
        AntiBlind module = AntiBlind.INSTANCE;

        return (!module.handleEvents() || !module.getConfusionEffect()) && instance.isPotionActive(potion);
    }

    @Redirect(method = {"setupFog", "updateFogColor"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean injectAntiBlindB(LivingEntity instance, Potion potion) {
        if (instance != mc.player) {
            return instance.isPotionActive(potion);
        }

        AntiBlind module = AntiBlind.INSTANCE;

        return (!module.handleEvents() || !module.getConfusionEffect()) && instance.isPotionActive(potion);
    }
}
