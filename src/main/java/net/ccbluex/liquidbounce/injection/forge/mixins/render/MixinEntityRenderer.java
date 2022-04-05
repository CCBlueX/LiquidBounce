/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import com.google.common.base.Predicates;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.modules.player.Reach;
import net.ccbluex.liquidbounce.features.module.modules.render.CameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.NoHurtCam;
import net.ccbluex.liquidbounce.features.module.modules.render.Tracers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

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
        LiquidBounce.eventManager.callEvent(new Render3DEvent(partialTicks));
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void injectHurtCameraEffect(CallbackInfo callbackInfo) {
        if (LiquidBounce.moduleManager.getModule(NoHurtCam.class).getState()) callbackInfo.cancel();
    }

    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"), cancellable = true)
    private void cameraClip(float partialTicks, CallbackInfo callbackInfo) {
        if (LiquidBounce.moduleManager.getModule(CameraClip.class).getState()) {
            callbackInfo.cancel();

            Entity entity = this.mc.getRenderViewEntity();
            float f = entity.getEyeHeight();

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
                f = (float) ((double) f + 1D);
                GlStateManager.translate(0F, 0.3F, 0.0F);

                if (!this.mc.gameSettings.debugCamEnable) {
                    BlockPos blockPos = new BlockPos(entity);
                    IBlockState blockState = this.mc.theWorld.getBlockState(blockPos);
                    net.minecraftforge.client.ForgeHooksClient.orientBedCamera(this.mc.theWorld, blockPos, blockState, entity);

                    GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
                }
            } else if (this.mc.gameSettings.thirdPersonView > 0) {
                double d3 = this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks;

                if (this.mc.gameSettings.debugCamEnable) {
                    GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                } else {
                    float f1 = entity.rotationYaw;
                    float f2 = entity.rotationPitch;

                    if (this.mc.gameSettings.thirdPersonView == 2) f2 += 180.0F;

                    if (this.mc.gameSettings.thirdPersonView == 2) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                    GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
                }
            } else GlStateManager.translate(0.0F, 0.0F, -0.1F);

            if (!this.mc.gameSettings.debugCamEnable) {
                float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
                float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                float roll = 0.0F;
                if (entity instanceof EntityAnimal) {
                    EntityAnimal entityanimal = (EntityAnimal) entity;
                    yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
                }

                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
                net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event = new net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup((EntityRenderer) (Object) this, entity, block, partialTicks, yaw, pitch, roll);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                GlStateManager.rotate(event.roll, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(event.pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(event.yaw, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(0.0F, -f, 0.0F);
            double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
            double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
            double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
            this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.BEFORE))
    private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo) {
        if (LiquidBounce.moduleManager.getModule(Tracers.class).getState()) GL11.glPushMatrix();
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.AFTER))
    private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo) {
        if (LiquidBounce.moduleManager.getModule(Tracers.class).getState()) GL11.glPopMatrix();
    }

    /**
     * @author CCBlueX
     */
    @Inject(method = "getMouseOver", at = @At("HEAD"), cancellable = true)
    private void getMouseOver(float p_getMouseOver_1_, CallbackInfo ci) {
        Entity entity = this.mc.getRenderViewEntity();
        if (entity != null && this.mc.theWorld != null) {
            this.mc.mcProfiler.startSection("pick");
            this.mc.pointedEntity = null;

            final Reach reach = (Reach) LiquidBounce.moduleManager.getModule(Reach.class);

            double d0 = reach.getState() ? reach.getMaxRange() : (double) this.mc.playerController.getBlockReachDistance();
            this.mc.objectMouseOver = entity.rayTrace(reach.getState() ? reach.getBuildReachValue().get() : d0, p_getMouseOver_1_);
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(p_getMouseOver_1_);
            boolean flag = false;
            if (this.mc.playerController.extendedReach()) {
                d0 = 6.0D;
                d1 = 6.0D;
            } else if (d0 > 3.0D) {
                flag = true;
            }

            if (this.mc.objectMouseOver != null) {
                d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            if (reach.getState()) {
                d1 = reach.getCombatReachValue().get();

                final MovingObjectPosition movingObjectPosition = entity.rayTrace(d1, p_getMouseOver_1_);

                if (movingObjectPosition != null) d1 = movingObjectPosition.hitVec.distanceTo(vec3);
            }

            Vec3 vec31 = entity.getLook(p_getMouseOver_1_);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            this.pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = this.mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith()));
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        this.pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                this.pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            this.pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (this.pointedEntity != null && flag && vec3.distanceTo(vec33) > (reach.getState() ? reach.getCombatReachValue().get() : 3.0D)) {
                this.pointedEntity = null;
                this.mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, Objects.requireNonNull(vec33), null, new BlockPos(vec33));
            }

            if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
                this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec33);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    this.mc.pointedEntity = this.pointedEntity;
                }
            }

            this.mc.mcProfiler.endSection();
        }

        ci.cancel();
    }
}