/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.awt.*;
import java.nio.FloatBuffer;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.injection.backend.EntityLivingBaseImplKt;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import co.uk.hexeption.utils.OutlineUtils;

@Mixin(RendererLivingEntity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRendererLivingEntity extends MixinRender
{
	@Shadow
	@Final
	private static DynamicTexture textureBrightness;
	@Shadow
	protected final boolean renderOutlines = false;
	@Shadow
	protected ModelBase mainModel;
	@Shadow
	protected FloatBuffer brightnessBuffer;

	@Shadow
	protected abstract float interpolateRotation(float prevRotation, float rotation, float partialTicks);

	@Shadow
	protected abstract float getSwingProgress(EntityLivingBase livingBase, float partialTicks);

	@Shadow
	protected abstract void renderLayers(EntityLivingBase entitylivingbaseIn, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_);

	@Shadow
	protected abstract void renderLivingAt(EntityLivingBase entityLivingBaseIn, double x, double y, double z);

	@Shadow
	protected abstract void rotateCorpse(EntityLivingBase bat, float p_77043_2_, float p_77043_3_, float partialTicks);

	@Shadow
	protected abstract boolean setScoreTeamColor(EntityLivingBase entityLivingBaseIn);

	@Shadow
	protected abstract void unsetScoreTeamColor();

	@Shadow
	protected abstract boolean setDoRenderBrightness(EntityLivingBase entityLivingBaseIn, float partialTicks);

	@Shadow
	protected abstract void unsetBrightness();

	@Shadow
	protected abstract float handleRotationFloat(EntityLivingBase livingBase, float partialTicks);

	@Shadow
	protected abstract void preRenderCallback(EntityLivingBase entitylivingbaseIn, float partialTickTime);

	@Shadow
	protected abstract int getColorMultiplier(EntityLivingBase entitylivingbaseIn, float lightBrightness, float partialTickTime);

	/**
	 * @author CCBlueX
	 * @reason Chams, Rotations - Body
	 */
	@Overwrite
	public void doRender(final EntityLivingBase entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks)
	{
		if (MinecraftForge.EVENT_BUS.post(new Pre(entity, (RendererLivingEntity) (Object) this, x, y, z)))
			return;
		final Chams chams = (Chams) LiquidBounce.moduleManager.getModule(Chams.class);
		final Rotations rotations = (Rotations) LiquidBounce.moduleManager.getModule(Rotations.class);

		// Chams Pre
		if (chams.getState() && chams.getTargetsValue().get() && EntityUtils.isSelected(EntityLivingBaseImplKt.wrap(entity), false))
		{
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glPolygonOffset(1.0F, -1000000.0F);
		}

		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		mainModel.swingProgress = getSwingProgress(entity, partialTicks);
		final boolean shouldSit = entity.isRiding() && entity.ridingEntity != null && entity.ridingEntity.shouldRiderSit();
		mainModel.isRiding = shouldSit;
		mainModel.isChild = entity.isChild();

		try
		{
			float interpolatedYawOffset = interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
			final float interpolatedYawHead = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
			float yawDelta = interpolatedYawHead - interpolatedYawOffset;

			if (shouldSit && entity.ridingEntity instanceof EntityLivingBase)
			{
				final EntityLivingBase entitylivingbase = (EntityLivingBase) entity.ridingEntity;
				interpolatedYawOffset = interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
				yawDelta = interpolatedYawHead - interpolatedYawOffset;
				float clampedYawDelta = MathHelper.wrapAngleTo180_float(yawDelta);

				if (clampedYawDelta < -85.0f)
					clampedYawDelta = -85.0f;

				if (clampedYawDelta >= 85.0F)
					clampedYawDelta = 85.0F;

				interpolatedYawOffset = interpolatedYawHead - clampedYawDelta;

				if (clampedYawDelta * clampedYawDelta > 2500.0F)
					interpolatedYawOffset += clampedYawDelta * 0.2F;
			}

			float interpolatedPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

			if (entity instanceof EntityPlayer && entity == Minecraft.getMinecraft().thePlayer && entityYaw != 0 && rotations.getState() && rotations.getBodyValue().get() && rotations.isRotating() && RotationUtils.serverRotation != null && RotationUtils.lastServerRotation != null)
			{
				final boolean interpolate = rotations.getInterpolateRotationsValue().get();
				interpolatedYawOffset = interpolate ? interpolateRotation(RotationUtils.lastServerRotation.getYaw(), RotationUtils.serverRotation.getYaw(), partialTicks) : RotationUtils.serverRotation.getYaw(); // Body Rotation

				yawDelta = 0;
				interpolatedPitch = interpolate ? interpolateRotation(RotationUtils.lastServerRotation.getPitch(), RotationUtils.serverRotation.getPitch(), partialTicks) : RotationUtils.serverRotation.getPitch(); // Pitch
			}

			renderLivingAt(entity, x, y, z);
			final float f8 = handleRotationFloat(entity, partialTicks);
			rotateCorpse(entity, f8, interpolatedYawOffset, partialTicks);
			GlStateManager.enableRescaleNormal();
			GlStateManager.scale(-1.0f, -1.0f, 1.0F);
			preRenderCallback(entity, partialTicks);
			GlStateManager.translate(0.0F, -1.5078125f, 0.0F);
			float interpolatedLimbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
			float reverseinterpolatedLimbSwingDelta = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

			if (entity.isChild())
				reverseinterpolatedLimbSwingDelta *= 3.0F;

			if (interpolatedLimbSwingAmount > 1.0F)
				interpolatedLimbSwingAmount = 1.0F;

			GlStateManager.enableAlpha();
			mainModel.setLivingAnimations(entity, reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, partialTicks);
			mainModel.setRotationAngles(reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, f8, yawDelta, interpolatedPitch, 0.0625F, entity);

			if (renderOutlines)
			{
				final boolean flag1 = setScoreTeamColor(entity);
				renderModel(entity, reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, f8, yawDelta, interpolatedPitch, 0.0625F);

				if (flag1)
					unsetScoreTeamColor();
			}
			else
			{
				final boolean flag = setDoRenderBrightness(entity, partialTicks);
				renderModel(entity, reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, f8, yawDelta, interpolatedPitch, 0.0625F);

				final ESP esp = (ESP) LiquidBounce.moduleManager.getModule(ESP.class);
				final String mode = esp.modeValue.get();
				if (esp.getState() && ("Fill".equalsIgnoreCase(mode) || "CSGO".equalsIgnoreCase(mode)) && EntityUtils.isSelected(EntityLivingBaseImplKt.wrap(entity), false))
				{
					final Minecraft mc = Minecraft.getMinecraft();
					mc.entityRenderer.disableLightmap();
					RenderUtils.glColor(esp.getColor(EntityLivingBaseImplKt.wrap(entity)));
					GL11.glPushMatrix();
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					if (!"CSGO".equalsIgnoreCase(mode))
						RenderHelper.disableStandardItemLighting();

					GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
					GL11.glPolygonOffset(1.0f, -3900000.0f);
					renderModel(entity, reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, f8, yawDelta, interpolatedPitch, 0.0625F);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_DEPTH_TEST);
					if (!"CSGO".equalsIgnoreCase(mode))
					{
						GlStateManager.enableLighting();
						GlStateManager.enableLight(0);
						GlStateManager.enableLight(1);
						GlStateManager.enableColorMaterial();
					}
					GL11.glPopMatrix();
					mc.entityRenderer.disableLightmap();
					RenderUtils.glColor(-1);
				}

				if (flag)
					unsetBrightness();

				GlStateManager.depthMask(true);

				if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator())
					renderLayers(entity, reverseinterpolatedLimbSwingDelta, interpolatedLimbSwingAmount, partialTicks, f8, yawDelta, interpolatedPitch, 0.0625F);
			}

			GlStateManager.disableRescaleNormal();
		}
		catch (final Exception exception)
		{
			ClientUtils.getLogger().error("Couldn't render entity", exception);
		}

		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.enableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.enableCull();
		GlStateManager.popMatrix();

		if (!renderOutlines)
			doRender(entity, x, y, z, entityYaw, partialTicks, null);

		if (chams.getState() && chams.getTargetsValue().get() && EntityUtils.isSelected(EntityLivingBaseImplKt.wrap(entity), false))
		{
			GL11.glPolygonOffset(1.0F, 1000000.0F);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
		}

		MinecraftForge.EVENT_BUS.post(new Post(entity, (RendererLivingEntity) (Object) this, x, y, z));
	}

	@Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
	private <T extends EntityLivingBase> void canRenderName(final T entity, final CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		if (!ESP.renderNameTags || LiquidBounce.moduleManager.getModule(NameTags.class).getState() && EntityUtils.isSelected(EntityLivingBaseImplKt.wrap(entity), false))
			callbackInfoReturnable.setReturnValue(false);
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	protected <T extends EntityLivingBase> void renderModel(final T entitylivingbaseIn, final float p_77036_2_, final float p_77036_3_, final float p_77036_4_, final float p_77036_5_, final float p_77036_6_, final float scaleFactor)
	{
		final boolean visible = !entitylivingbaseIn.isInvisible();
		final TrueSight trueSight = (TrueSight) LiquidBounce.moduleManager.getModule(TrueSight.class);
		final boolean semiVisible = !visible && (!entitylivingbaseIn.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer) || trueSight.getState() && trueSight.getEntitiesValue().get());

		if (visible || semiVisible)
		{
			if (!bindEntityTexture(entitylivingbaseIn))
				return;

			if (semiVisible)
			{
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 0.15F);
				GlStateManager.depthMask(false);
				GL11.glEnable(GL11.GL_BLEND);
				GlStateManager.blendFunc(770, 771);
				GlStateManager.alphaFunc(516, 0.003921569F);
			}

			final ESP esp = (ESP) LiquidBounce.moduleManager.getModule(ESP.class);
			if (esp.getState() && EntityUtils.isSelected(EntityLivingBaseImplKt.wrap(entitylivingbaseIn), false))
			{
				final Minecraft mc = Minecraft.getMinecraft();
				final boolean fancyGraphics = mc.gameSettings.fancyGraphics;
				mc.gameSettings.fancyGraphics = false;

				final float gamma = mc.gameSettings.gammaSetting;
				mc.gameSettings.gammaSetting = 100000.0F;

				switch (esp.modeValue.get().toLowerCase())
				{
					case "wireframe":
						GL11.glPushMatrix();
						GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
						GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
						GL11.glDisable(GL11.GL_TEXTURE_2D);
						GL11.glDisable(GL11.GL_LIGHTING);
						GL11.glDisable(GL11.GL_DEPTH_TEST);
						GL11.glEnable(GL11.GL_LINE_SMOOTH);
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
						RenderUtils.glColor(esp.getColor(EntityLivingBaseImplKt.wrap(entitylivingbaseIn)));
						GL11.glLineWidth(esp.wireframeWidth.get());
						mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
						GL11.glPopAttrib();
						GL11.glPopMatrix();
						break;
					case "outline":
						ClientUtils.disableFastRender();
						GlStateManager.resetColor();

						final Color color = esp.getColor(EntityLivingBaseImplKt.wrap(entitylivingbaseIn));
						OutlineUtils.setColor(color);
						OutlineUtils.renderOne(esp.outlineWidth.get());
						mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
						OutlineUtils.setColor(color);
						OutlineUtils.renderTwo();
						mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
						OutlineUtils.setColor(color);
						OutlineUtils.renderThree();
						mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
						OutlineUtils.setColor(color);
						OutlineUtils.renderFour(color);
						mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
						OutlineUtils.setColor(color);
						OutlineUtils.renderFive();
						OutlineUtils.setColor(Color.WHITE);
				}
				mc.gameSettings.fancyGraphics = fancyGraphics;
				mc.gameSettings.gammaSetting = gamma;
			}

			mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);

			if (semiVisible)
			{
				GlStateManager.disableBlend();
				GlStateManager.alphaFunc(516, 0.1F);
				GlStateManager.popMatrix();
				GlStateManager.depthMask(true);
			}
		}
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	protected <T extends EntityLivingBase> boolean setBrightness(final T entitylivingbaseIn, final float partialTicks, final boolean combineTextures)
	{
		final HurtCam hurtCam = (HurtCam) LiquidBounce.moduleManager.get(HurtCam.class);

		final float f = entitylivingbaseIn.getBrightness(partialTicks);
		final int i = getColorMultiplier(entitylivingbaseIn, f, partialTicks);
		final boolean flag = (i >> 24 & 255) <= 0;
		final boolean hurtEffect = entitylivingbaseIn.hurtTime > 0 || entitylivingbaseIn.deathTime > 0;

		if (flag && !hurtEffect)
			return false;

		if (flag && !combineTextures)
			return false;

		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.enableTexture2D();
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, OpenGlHelper.GL_COMBINE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_RGB, GL11.GL_MODULATE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.enableTexture2D();
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, OpenGlHelper.GL_COMBINE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND2_RGB, GL11.GL_SRC_ALPHA);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
		brightnessBuffer.position(0);

		if (hurtEffect)
			if (hurtCam.getState() && hurtCam.getCustomHurtEffect().get())
			{
				brightnessBuffer.put(hurtCam.getCustomHurtEffectR().get() / 255.0F);
				brightnessBuffer.put(hurtCam.getCustomHurtEffectG().get() / 255.0F);
				brightnessBuffer.put(hurtCam.getCustomHurtEffectB().get() / 255.0F);
				brightnessBuffer.put(hurtCam.getCustomHurtEffectAlpha().get() / 255.0F);
			}
			else
			{
				brightnessBuffer.put(1.0F);
				brightnessBuffer.put(0.0F);
				brightnessBuffer.put(0.0F);
				brightnessBuffer.put(0.3F);
			}
		else
		{
			brightnessBuffer.put((i >> 16 & 255) / 255.0F);
			brightnessBuffer.put((i >> 8 & 255) / 255.0F);
			brightnessBuffer.put((i & 255) / 255.0F);
			brightnessBuffer.put(1.0F - (i >> 24 & 255) / 255.0F);
		}

		brightnessBuffer.flip();
		GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, brightnessBuffer);
		GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
		GlStateManager.enableTexture2D();
		GlStateManager.bindTexture(textureBrightness.getGlTextureId());
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, OpenGlHelper.GL_COMBINE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_RGB, GL11.GL_MODULATE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, OpenGlHelper.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		return true;
	}
}
