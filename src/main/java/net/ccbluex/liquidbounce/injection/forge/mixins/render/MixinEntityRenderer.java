/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import com.google.common.base.Predicates;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack;
import net.ccbluex.liquidbounce.features.module.modules.combat.ForwardTrack;
import net.ccbluex.liquidbounce.features.module.modules.misc.OverrideRaycast;
import net.ccbluex.liquidbounce.features.module.modules.player.Reach;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

@Mixin(EntityRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityRenderer {

    @Shadow
    private Entity pointedEntity;

    @Shadow
    private Minecraft mc;

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

    @ModifyConstant(method = "orientCamera", constant = @Constant(intValue = 8))
    private int injectCameraClip(int eight) {
        return CameraClip.INSTANCE.handleEvents() ? 0 : eight;
    }

    @Inject(at = @At("HEAD"), method = "updateCameraAndRender")
    private void injectCameraModifications(float p_updateCameraAndRender_1_, long p_updateCameraAndRender_2_, CallbackInfo ci) {
        FreeCam.INSTANCE.useModifiedPosition();
    }

    @Inject(method = "orientCamera", at = @At(value = "HEAD"))
    private void injectFreeLook(float p_orientCamera_1_, CallbackInfo ci) {
        FreeLook.INSTANCE.useModifiedRotation();
    }

    @Inject(at = @At("TAIL"), method = "updateCameraAndRender")
    private void injectCameraRestorations(float p_updateCameraAndRender_1_, long p_updateCameraAndRender_2_, CallbackInfo ci) {
        FreeLook.INSTANCE.restoreOriginalRotation();
        FreeCam.INSTANCE.restoreOriginalPosition();
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
        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;

            final Reach reach = Reach.INSTANCE;

            double d0 = reach.handleEvents() ? reach.getMaxRange() : mc.playerController.getBlockReachDistance();
            Vec3 vec3 = entity.getPositionEyes(p_getMouseOver_1_);
            Rotation rotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            Vec3 vec31 = RotationUtils.INSTANCE.getVectorForRotation(RotationUtils.INSTANCE.getCurrentRotation() != null && OverrideRaycast.INSTANCE.shouldOverride() ? RotationUtils.INSTANCE.getCurrentRotation() : rotation);
            double p_rayTrace_1_ = (reach.handleEvents() ? reach.getBuildReach() : d0);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * p_rayTrace_1_, vec31.yCoord * p_rayTrace_1_, vec31.zCoord * p_rayTrace_1_);
            mc.objectMouseOver = entity.worldObj.rayTraceBlocks(vec3, vec32, false, false, true);
            double d1 = d0;
            boolean flag = false;
            if (mc.playerController.extendedReach()) {
                // d0 = 6;
                d1 = 6;
            } else if (d0 > 3) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            if (reach.handleEvents()) {
                double p_rayTrace_1_2 = reach.getBuildReach();
                Vec3 vec322 = vec3.addVector(vec31.xCoord * p_rayTrace_1_2, vec31.yCoord * p_rayTrace_1_2, vec31.zCoord * p_rayTrace_1_2);
                final MovingObjectPosition movingObjectPosition = entity.worldObj.rayTraceBlocks(vec3, vec322, false, false, true);

                if (movingObjectPosition != null) d1 = movingObjectPosition.hitVec.distanceTo(vec3);
            }

            pointedEntity = null;
            Vec3 vec33 = null;
            List<Entity> list = mc.theWorld.getEntities(Entity.class, Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith() && p_apply_1_ != entity));
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.getCollisionBorderSize();

                final ArrayList<AxisAlignedBB> boxes = new ArrayList<>();
                boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));

                Backtrack.INSTANCE.loopThroughBacktrackData(entity1, () -> {
                    boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));
                    return false;
                });

                ForwardTrack.INSTANCE.includeEntityTruePos(entity1, () -> {
                    boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));
                    return null;
                });

                for (final AxisAlignedBB axisalignedbb : boxes) {
                    MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                    if (axisalignedbb.isVecInside(vec3)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                            d2 = 0;
                        }
                    } else if (movingobjectposition != null) {
                        double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                        if (d3 < d2 || d2 == 0) {
                            if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0) {
                                    pointedEntity = entity1;
                                    vec33 = movingobjectposition.hitVec;
                                }
                            } else {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > (reach.handleEvents() ? reach.getCombatReach() : 3)) {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, Objects.requireNonNull(vec33), null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
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

    @Redirect(method = {"setupFog", "updateFogColor"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean injectAntiBlindB(EntityLivingBase instance, Potion potion) {
        if (instance != mc.thePlayer) {
            return instance.isPotionActive(potion);
        }

        AntiBlind module = AntiBlind.INSTANCE;

        return (!module.handleEvents() || !module.getConfusionEffect()) && instance.isPotionActive(potion);
    }
}
