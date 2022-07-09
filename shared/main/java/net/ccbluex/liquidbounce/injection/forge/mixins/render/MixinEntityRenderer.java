/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.List;

import com.google.common.base.Predicates;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.modules.player.ExtendedReach;
import net.ccbluex.liquidbounce.features.module.modules.player.Reach;
import net.ccbluex.liquidbounce.features.module.modules.render.CameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.HurtCam;
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
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityRenderer
{

    @Shadow
    public abstract void loadShader(ResourceLocation resourceLocationIn);

    @Shadow
    public abstract void setupCameraTransform(float partialTicks, int pass);

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

    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = Shift.BEFORE))
    private void handleRender3DEvent(final int pass, final float partialTicks, final long finishTimeNano, final CallbackInfo callbackInfo)
    {
        mc.mcProfiler.endStartSection("LiquidBounce-Render3DEvent");
        LiquidBounce.eventManager.callEvent(new Render3DEvent(partialTicks), true);
        mc.mcProfiler.endStartSection("hand");
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void injectNoHurtCam(final CallbackInfo callbackInfo)
    {
        final HurtCam hurtCam = (HurtCam) LiquidBounce.moduleManager.get(HurtCam.class);
        if (hurtCam.getState() && hurtCam.getNoHurtCam().get())
            callbackInfo.cancel();
    }

    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"), cancellable = true)
    private void injectCameraClip(final float partialTicks, final CallbackInfo callbackInfo)
    {
        if (LiquidBounce.moduleManager.get(CameraClip.class).getState())
        {
            callbackInfo.cancel();

            final Entity entity = mc.getRenderViewEntity();
            float f = entity.getEyeHeight();

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping())
            {
                f += 1.0F;
                GlStateManager.translate(0.0F, 0.3F, 0.0F);

                if (!mc.gameSettings.debugCamEnable)
                {
                    final BlockPos blockpos = new BlockPos(entity);
                    final IBlockState iblockstate = mc.theWorld.getBlockState(blockpos);
                    ForgeHooksClient.orientBedCamera(mc.theWorld, blockpos, iblockstate, entity);

                    GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
                }
            }
            else if (mc.gameSettings.thirdPersonView > 0)
            {
                final double d3 = thirdPersonDistanceTemp + (thirdPersonDistance - thirdPersonDistanceTemp) * partialTicks;

                if (mc.gameSettings.debugCamEnable)
                    GlStateManager.translate(0.0F, 0.0F, (float) -d3);
                else
                {
                    final float f1 = entity.rotationYaw;
                    float f2 = entity.rotationPitch;

                    if (mc.gameSettings.thirdPersonView == 2)
                        f2 += 180.0F;

                    if (mc.gameSettings.thirdPersonView == 2)
                        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.0F, (float) -d3);
                    GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
                }
            }
            else
                GlStateManager.translate(0.0F, 0.0F, -0.1F);

            if (!mc.gameSettings.debugCamEnable)
            {
                float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
                final float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                final float roll = 0.0F;
                if (entity instanceof EntityAnimal)
                {
                    final EntityAnimal entityanimal = (EntityAnimal) entity;
                    yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
                }

                final Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);
                final CameraSetup event = new CameraSetup((EntityRenderer) (Object) this, entity, block, partialTicks, yaw, pitch, roll);
                MinecraftForge.EVENT_BUS.post(event);
                GlStateManager.rotate(event.roll, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(event.pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(event.yaw, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(0.0F, -f, 0.0F);
            final double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            final double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + f;
            final double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = Shift.BEFORE))
    private void injectTracersPre(final CallbackInfo callbackInfo)
    {
        if (LiquidBounce.moduleManager.get(Tracers.class).getState())
            GL11.glPushMatrix();
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = Shift.AFTER))
    private void injectTracersPost(final CallbackInfo callbackInfo)
    {
        if (LiquidBounce.moduleManager.get(Tracers.class).getState())
            GL11.glPopMatrix();
    }

    /**
     * @author CCBlueX
     * @reason Reach, ExtendedReach
     */
    @Overwrite
    public void getMouseOver(final float partialTicks)
    {
        final Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.theWorld != null)
        {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;

            final Reach reach = (Reach) LiquidBounce.moduleManager.get(Reach.class);
            final ExtendedReach extendedReach = (ExtendedReach) LiquidBounce.moduleManager.get(ExtendedReach.class);

            double blockReach = extendedReach.getState() ? extendedReach.buildReach.get() : reach.getState() ? reach.getMaxRange() : mc.playerController.getBlockReachDistance();

            mc.objectMouseOver = entity.rayTrace(reach.getState() && !extendedReach.getState() ? reach.getBuildReachValue().get() : blockReach, partialTicks);

            double hitvecDistance = blockReach;
            final Vec3 eyePos = entity.getPositionEyes(partialTicks);
            boolean creativeReach = false;

            if (mc.playerController.extendedReach())
            {
                blockReach = 6.0D;
                hitvecDistance = 6.0D;
            }
            else if (blockReach > 3.0D)
                creativeReach = true;

            if (mc.objectMouseOver != null)
                hitvecDistance = mc.objectMouseOver.hitVec.distanceTo(eyePos);

            if (reach.getState())
            {
                hitvecDistance = reach.getCombatReachValue().get();

                final MovingObjectPosition movingObjectPosition = entity.rayTrace(hitvecDistance, partialTicks);

                if (movingObjectPosition != null)
                    hitvecDistance = movingObjectPosition.hitVec.distanceTo(eyePos);
            }

            final Vec3 look = entity.getLook(partialTicks);
            final Vec3 blockReachPos = eyePos.addVector(look.xCoord * blockReach, look.yCoord * blockReach, look.zCoord * blockReach);

            pointedEntity = null;
            Vec3 interceptPos = null;

            final float expandSize = 1.0F;
            final List<Entity> entitiesInRay = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(look.xCoord * blockReach, look.yCoord * blockReach, look.zCoord * blockReach).expand(expandSize, expandSize, expandSize), Predicates.and(EntitySelectors.NOT_SPECTATING, e -> e != null && e.canBeCollidedWith()));
            double interceptPosDistance = hitvecDistance;

            for (final Entity entityInRay : entitiesInRay)
            {
                final float borderSize = entityInRay.getCollisionBorderSize();
                final AxisAlignedBB expandedBB = entityInRay.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
                final MovingObjectPosition intercept = expandedBB.calculateIntercept(eyePos, blockReachPos);

                if (expandedBB.isVecInside(eyePos))
                {
                    if (interceptPosDistance >= 0.0D)
                    {
                        pointedEntity = entityInRay;
                        interceptPos = intercept == null ? eyePos : intercept.hitVec;
                        interceptPosDistance = 0.0D;
                    }
                }
                else if (intercept != null)
                {
                    final double _interceptPosDistance = eyePos.distanceTo(intercept.hitVec);
                    if (_interceptPosDistance < interceptPosDistance || interceptPosDistance == 0.0D)
                        if (entityInRay == entity.ridingEntity && !entity.canRiderInteract())
                        {
                            if (interceptPosDistance == 0.0D)
                            {
                                pointedEntity = entityInRay;
                                interceptPos = intercept.hitVec;
                            }
                        }
                        else
                        {
                            pointedEntity = entityInRay;
                            interceptPos = intercept.hitVec;
                            interceptPosDistance = _interceptPosDistance;
                        }
                }
            }

            if (pointedEntity != null && creativeReach && eyePos.distanceTo(interceptPos) > (reach.getState() ? reach.getCombatReachValue().get() : 3.0D))
            {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectType.MISS, interceptPos, null, new BlockPos(interceptPos));
            }

            if (pointedEntity != null && (interceptPosDistance < hitvecDistance || mc.objectMouseOver == null))
            {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, interceptPos);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
                    mc.pointedEntity = pointedEntity;
            }

            mc.mcProfiler.endSection();
        }
    }
}
