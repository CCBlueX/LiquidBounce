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
import net.ccbluex.liquidbounce.features.module.modules.player.Reach;
import net.ccbluex.liquidbounce.features.module.modules.render.CameraClip;
import net.ccbluex.liquidbounce.features.module.modules.render.HurtCam;
import net.ccbluex.liquidbounce.features.module.modules.render.Tracers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
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
	private Entity pointedEntity;
	@Final
	@Shadow
	private Minecraft mc;
	@Final
	@Shadow
	private float thirdPersonDistance;
	@Shadow
	private boolean cloudFog;
	@Shadow
	private float thirdPersonDistancePrev;

	@Shadow
	public abstract void loadShader(ResourceLocation resourceLocationIn);

	@Shadow
	public abstract void setupCameraTransform(float partialTicks, int pass);

	@Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = Shift.BEFORE))
	private void renderWorldPass(final int pass, final float partialTicks, final long finishTimeNano, final CallbackInfo callbackInfo)
	{
		mc.mcProfiler.endStartSection("LiquidBounce-Render3DEvent");
		LiquidBounce.eventManager.callEvent(new Render3DEvent(partialTicks), true);
		mc.mcProfiler.endStartSection("hand");
	}

	@Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
	private void injectHurtCameraEffect(final CallbackInfo callbackInfo)
	{
		final HurtCam hurtCam = (HurtCam) LiquidBounce.moduleManager.get(HurtCam.class);
		if (hurtCam.getState() && hurtCam.getNoHurtCam().get())
			callbackInfo.cancel();
	}

	@Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;distanceTo(Lnet/minecraft/util/math/Vec3d;)D"), cancellable = true)
	private void cameraClip(final float partialTicks, final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(CameraClip.class).getState())
		{
			callbackInfo.cancel();

			final Entity entity = mc.getRenderViewEntity();
			float f = entity.getEyeHeight();

			if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping())
			{
				f += 1.0D;
				GlStateManager.translate(0.0F, 0.3F, 0.0F);

				if (!mc.gameSettings.debugCamEnable)
				{
					final BlockPos blockpos = new BlockPos(entity);
					final IBlockState iblockstate = mc.world.getBlockState(blockpos);
					ForgeHooksClient.orientBedCamera(mc.world, blockpos, iblockstate, entity);

					GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
					GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
				}
			}
			else if (mc.gameSettings.thirdPersonView > 0)
			{
				final double d3 = thirdPersonDistancePrev + (thirdPersonDistance - thirdPersonDistancePrev) * partialTicks;

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

				final IBlockState block = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, entity, partialTicks);
				final CameraSetup event = new CameraSetup((EntityRenderer) (Object) this, entity, block, partialTicks, yaw, pitch, roll);
				MinecraftForge.EVENT_BUS.post(event);
				GlStateManager.rotate(event.getRoll(), 0.0F, 0.0F, 1.0F);
				GlStateManager.rotate(event.getPitch(), 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(event.getYaw(), 0.0F, 1.0F, 0.0F);
			}

			GlStateManager.translate(0.0F, -f, 0.0F);
			final double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
			final double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + f;
			final double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
			cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
		}
	}

	@Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;applyBobbing(F)V", shift = Shift.BEFORE))
	private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(Tracers.class).getState())
			GL11.glPushMatrix();
	}

	@Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;applyBobbing(F)V", shift = Shift.AFTER))
	private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo)
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

		if (entity != null)
			if (mc.world != null)
			{
				mc.mcProfiler.startSection("pick");
				mc.pointedEntity = null;

				final Reach reach = (Reach) LiquidBounce.moduleManager.get(Reach.class);

				double d0 = reach.getState() ? reach.getMaxRange() : mc.playerController.getBlockReachDistance();
				mc.objectMouseOver = entity.rayTrace(reach.getState() ? reach.getBuildReachValue().get() : d0, partialTicks);

				final Vec3d vec3d = entity.getPositionEyes(partialTicks);
				boolean flag = false;
				final int i = 3;
				double d1 = d0;

				if (mc.playerController.extendedReach())
				{
					d1 = 6.0D;
					d0 = d1;
				}
				else if (d0 > 3.0D)
					flag = true;

				if (mc.objectMouseOver != null)
					d1 = mc.objectMouseOver.hitVec.distanceTo(vec3d);

				if (reach.getState())
				{
					d1 = reach.getCombatReachValue().get();

					final RayTraceResult movingObjectPosition = entity.rayTrace(d1, partialTicks);

					if (movingObjectPosition != null)
						d1 = movingObjectPosition.hitVec.distanceTo(vec3d);
				}

				final Vec3d vec3d1 = entity.getLook(1.0F);
				final Vec3d vec3d2 = vec3d.addVector(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
				pointedEntity = null;
				Vec3d vec3d3 = null;
				final float f = 1.0F;
				final List<Entity> list = mc.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith()));
				double d2 = d1;

				for (final Entity entity1 : list)
				{
					final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
					final RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

					if (axisalignedbb.contains(vec3d))
					{
						if (d2 >= 0.0D)
						{
							pointedEntity = entity1;
							vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
							d2 = 0.0D;
						}
					}
					else if (raytraceresult != null)
					{
						final double d3 = vec3d.distanceTo(raytraceresult.hitVec);

						if (d3 < d2 || d2 == 0.0D)
							if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract())
							{
								if (d2 == 0.0D)
								{
									pointedEntity = entity1;
									vec3d3 = raytraceresult.hitVec;
								}
							}
							else
							{
								pointedEntity = entity1;
								vec3d3 = raytraceresult.hitVec;
								d2 = d3;
							}
					}
				}

				if (pointedEntity != null && flag && vec3d.distanceTo(vec3d3) > (reach.getState() ? reach.getCombatReachValue().get() : 3.0D))
				{
					pointedEntity = null;
					mc.objectMouseOver = new RayTraceResult(Type.MISS, vec3d3, null, new BlockPos(vec3d3));
				}

				if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null))
				{
					mc.objectMouseOver = new RayTraceResult(pointedEntity, vec3d3);

					if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame)
						mc.pointedEntity = pointedEntity;
				}

				mc.mcProfiler.endSection();
			}
	}
}
