/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Predicates;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render3DEvent;
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
	private void renderWorldPass(final int pass, final float partialTicks, final long finishTimeNano, final CallbackInfo callbackInfo)
	{
		LiquidBounce.eventManager.callEvent(new Render3DEvent(partialTicks));
	}

	@Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
	private void injectHurtCameraEffect(final CallbackInfo callbackInfo)
	{
		final HurtCam hurtCam = (HurtCam) LiquidBounce.moduleManager.getModule(HurtCam.class);
		if (hurtCam.getState() && hurtCam.getNoHurtCam().get())
			callbackInfo.cancel();
	}

	@Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"), cancellable = true)
	private void cameraClip(final float partialTicks, final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.getModule(CameraClip.class).getState())
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
	private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.getModule(Tracers.class).getState())
			GL11.glPushMatrix();
	}

	@Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = Shift.AFTER))
	private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.getModule(Tracers.class).getState())
			GL11.glPopMatrix();
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	public void getMouseOver(final float p_getMouseOver_1_)
	{
		final Entity entity = mc.getRenderViewEntity();
		if (entity != null && mc.theWorld != null)
		{
			mc.mcProfiler.startSection("pick");
			mc.pointedEntity = null;

			final Reach reach = (Reach) LiquidBounce.moduleManager.getModule(Reach.class);

			double d0 = reach.getState() ? reach.getMaxRange() : mc.playerController.getBlockReachDistance();
			mc.objectMouseOver = entity.rayTrace(reach.getState() ? reach.getBuildReachValue().get() : d0, p_getMouseOver_1_);
			double d1 = d0;
			final Vec3 vec3 = entity.getPositionEyes(p_getMouseOver_1_);
			boolean flag = false;
			if (mc.playerController.extendedReach())
			{
				d0 = 6.0D;
				d1 = 6.0D;
			}
			else if (d0 > 3.0D)
				flag = true;

			if (mc.objectMouseOver != null)
				d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);

			if (reach.getState())
			{
				d1 = reach.getCombatReachValue().get();

				final MovingObjectPosition movingObjectPosition = entity.rayTrace(d1, p_getMouseOver_1_);

				if (movingObjectPosition != null)
					d1 = movingObjectPosition.hitVec.distanceTo(vec3);
			}

			final Vec3 vec31 = entity.getLook(p_getMouseOver_1_);
			final Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
			pointedEntity = null;
			Vec3 vec33 = null;
			final float f = 1.0F;
			final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith()));
			double d2 = d1;

			for (final Entity entity1 : list)
			{
				final float f1 = entity1.getCollisionBorderSize();
				final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
				final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
				if (axisalignedbb.isVecInside(vec3))
				{
					if (d2 >= 0.0D)
					{
						pointedEntity = entity1;
						vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
						d2 = 0.0D;
					}
				}
				else if (movingobjectposition != null)
				{
					final double d3 = vec3.distanceTo(movingobjectposition.hitVec);
					if (d3 < d2 || d2 == 0.0D)
						if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
							if (d2 == 0.0D) {
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

			if (pointedEntity != null && flag && vec3.distanceTo(vec33) > (reach.getState() ? reach.getCombatReachValue().get() : 3.0D))
			{
				pointedEntity = null;
				mc.objectMouseOver = new MovingObjectPosition(MovingObjectType.MISS, Objects.requireNonNull(vec33), null, new BlockPos(vec33));
			}

			if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null))
			{
				mc.objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
				if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
					mc.pointedEntity = pointedEntity;
			}

			mc.mcProfiler.endSection();
		}
	}
}
