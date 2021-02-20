/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.SwingAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings(
{
		"WeakerAccess", "MethodMayBeStatic", "DesignForExtension", "NumericCastThatLosesPrecision"
})
@Mixin(ItemRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinItemRenderer
{
	@Shadow
	private float prevEquippedProgress;
	@Shadow
	private float equippedProgress;
	@Shadow
	@Final
	private Minecraft mc;
	@Shadow
	private ItemStack itemToRender;

	@Shadow
	protected abstract void rotateArroundXAndY(float angle, float angleY);

	@Shadow
	protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

	@Shadow
	protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

	@Shadow
	protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

	@Shadow
	protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

	@Shadow
	protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

	@Shadow
	protected abstract void doItemUsedTransformations(float swingProgress);

	@Shadow
	public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, TransformType transform);

	@Shadow
	protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

	/**
	 * @author eric0210
	 * @reason SwingAnimation
	 */
	@Overwrite
	private void doBlockTransformations()
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

		GlStateManager.translate(-0.5f, 0.2f, 0.0f);
		GlStateManager.rotate(30.0f, 0.0f, 1.0f, 0.0f);
		GlStateManager.rotate(sa.getState() ? -sa.getSwordBlockRotationAngle().get() : -80.0f, 1.0f, 0.0f, 0.0f);
		GlStateManager.rotate(60.0f, 0.0f, 1.0f, 0.0f);
	}

	/**
	 * Performs transformations prior to the rendering of a held item in first person.
	 *
	 * @author Mojang
	 * @reason SwingAnimation
	 * @see    SwingAnimation
	 */
	@Overwrite
	private void transformFirstPersonItem(final float equipProgress, final float swingProgress)
	{
		final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		final boolean swingAnimationState = swingAnimation.getState();

		final float fixedSwingProgress = swingAnimationState && swingAnimation.getStaticSwingProgress().get() ? swingAnimation.getStaticSwingProgressValue().get() : swingProgress;

		final float sq = getAnimationProgress(fixedSwingProgress, true, true);
		final float sqrt = getAnimationProgress(fixedSwingProgress, false, true);

		translate(swingAnimation, fixedSwingProgress);

		GlStateManager.translate(0.0F, equipProgress * -0.6f, 0.0F);
		GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(sq * -20.0f, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(sqrt * -80.0f, 1.0F, 0.0F, 0.0F);

		final float scale = swingAnimationState ? swingAnimation.getScale().get() : 0.4f;
		GlStateManager.scale(scale, scale, scale);
	}

	private float getAnimationProgress(final float swingProgress, final boolean sq, final boolean isSwing)
	{
		final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

		if (sq)
		{
			if (swingAnimation.getState())
			{
				final float fixedSwingProgress;

				switch ((isSwing ? swingAnimation.getSwingSqSmoothingMethod() : swingAnimation.getBlockSqSwingSmoothingMethod()).get().toLowerCase())
				{
					case "sqrt":
						fixedSwingProgress = MathHelper.sqrt_float(swingProgress);
						break;
					case "sqrtsqrt":
						fixedSwingProgress = MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress));
						break;
					case "sq":
						fixedSwingProgress = swingProgress * swingProgress;
						break;
					case "sqsq":
						fixedSwingProgress = swingProgress * swingProgress * swingProgress;
						break;
					default:
						fixedSwingProgress = swingProgress;
				}

				final Boolean sqSmoothSin = (isSwing ? swingAnimation.getSwingSqSmoothingSin() : swingAnimation.getBlockSqSmoothingSin()).get();
				return sqSmoothSin ? MathHelper.sin(fixedSwingProgress * WMathHelper.PI) : fixedSwingProgress;
			}

			return MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
		}

		if (swingAnimation.getState())
		{
			final float fixedSwingProgress;

			switch ((isSwing ? swingAnimation.getSwingSqrtSmoothingMethod() : swingAnimation.getBlockSqrtSwingSmoothingMethod()).get().toLowerCase())
			{
				case "sqrt":
					fixedSwingProgress = MathHelper.sqrt_float(swingProgress);
					break;
				case "sqrtsqrt":
					fixedSwingProgress = MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress));
					break;
				case "sq":
					fixedSwingProgress = swingProgress * swingProgress;
					break;
				case "sqsq":
					fixedSwingProgress = swingProgress * swingProgress * swingProgress;
					break;
				default:
					fixedSwingProgress = swingProgress;
			}

			final Boolean sqrtSmoothSin = (isSwing ? swingAnimation.getSwingSqrtSmoothingSin() : swingAnimation.getBlockSqrtSmoothingSin()).get();
			return sqrtSmoothSin ? MathHelper.sin(fixedSwingProgress * WMathHelper.PI) : fixedSwingProgress;
		}

		return MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
	}

	/**
	 * Performs transformations prior to the rendering of a held item in first person.
	 *
	 * @author Mojang
	 * @see    SwingAnimation
	 */
	protected void transformFirstPersonItemBlock(final float equipProgress, final float swingProgress, final float equipProgressAffect)
	{
		final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

		final float sq = getAnimationProgress(swingProgress, true, false);
		final float sqrt = getAnimationProgress(swingProgress, false, false);

		translateBlock(swingAnimation, swingProgress);

		final double xTranslate = equipProgress * (swingAnimation.getEquipProgressAffectsAnimationTranslation().get() ? -swingAnimation.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
		final double yTranslate = equipProgress * (swingAnimation.getEquipProgressAffectsAnimationTranslation().get() ? -swingAnimation.getEquipProgressAnimationTranslationAffectnessY().get() : -0.6f);
		final double zTranslate = equipProgress * (swingAnimation.getEquipProgressAffectsAnimationTranslation().get() ? -swingAnimation.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);

		GlStateManager.translate(xTranslate, yTranslate, zTranslate);
		GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(equipProgressAffect * sqrt * -80.0f, 1.0F, 0.0F, 0.0F);

		final float scale = swingAnimation.getState() ? swingAnimation.getBlockScale().get() : 0.4f;

		GlStateManager.scale(scale, scale, scale);
	}

	/**
	 * @author CCBlueX
	 * @reason SwingAnimation
	 * @see    KillAura
	 * @see    SwingAnimation
	 */
	@Overwrite
	public void renderItemInFirstPerson(final float partialTicks)
	{
		final float interpolatedEquipProgress = prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
		final EntityPlayerSP abstractclientplayer = mc.thePlayer;

		final float swingProgress = abstractclientplayer.getSwingProgress(partialTicks);
		final float smoothPitchRotation = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
		final float smoothYawRotation = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;

		rotateArroundXAndY(smoothPitchRotation, smoothYawRotation);
		setLightMapFromPlayer(abstractclientplayer);
		rotateWithPlayerRotations(abstractclientplayer, partialTicks);
		GlStateManager.enableRescaleNormal();
		GlStateManager.pushMatrix();

		float equipProgress = 1.0F - interpolatedEquipProgress;

		if (itemToRender != null)
		{
			final KillAura killAura = (KillAura) LiquidBounce.moduleManager.get(KillAura.class);
			final TpAura tpaura = (TpAura) LiquidBounce.moduleManager.get(TpAura.class);
			final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

			if (swingAnimation.getState())
				equipProgress *= swingAnimation.getEquipProgressAffectMultiplier().get();

			if (itemToRender.getItem() instanceof ItemMap)
				renderItemMap(abstractclientplayer, smoothPitchRotation, equipProgress, swingProgress);
			else if (abstractclientplayer.getItemInUseCount() > 0 || itemToRender.getItem() instanceof ItemSword && (killAura.getClientSideBlockingStatus() || tpaura.getClientSideBlockingStatus())) // Using Item (eating, drinking, blocking, etc.)
			{
				final EnumAction enumaction = killAura.getClientSideBlockingStatus()/* || tpaura.blockingRenderStatus */ ? EnumAction.BLOCK : itemToRender.getItemUseAction();

				switch (enumaction)
				{
					case NONE:
						transformFirstPersonItem(equipProgress, 0.0F);
						break;
					case EAT: // Eating food
					case DRINK: // Drinking potion
						performDrinking(abstractclientplayer, partialTicks);
						transformFirstPersonItem(equipProgress, swingProgress);
						break;
					case BLOCK:
						// Sword Blocking

						if (swingAnimation.getState())
							applyCustomBlockAnimation(swingAnimation, swingProgress, equipProgress, interpolatedEquipProgress);
						else
						{
							transformFirstPersonItem(equipProgress, swingProgress);
							doBlockTransformations();
						}
						break;
					case BOW:
						// Charging bow
						transformFirstPersonItem(equipProgress, swingProgress);
						doBowTransformations(partialTicks, abstractclientplayer);
				}
			}
			else if (swingAnimation.getState() && swingAnimation.getSmoothSwing().get()) // Smooth swing
				transformFirstPersonItem(equipProgress, swingProgress);
			else
			{
				doItemUsedTransformations(swingProgress);
				transformFirstPersonItem(equipProgress, swingProgress);
			}

			renderItem(abstractclientplayer, itemToRender, TransformType.FIRST_PERSON);
		}
		else if (!abstractclientplayer.isInvisible()) // Render player arm if player is hold nothing and not invisible
			renderPlayerArm(abstractclientplayer, equipProgress, swingProgress);

		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
	}

	private void applyCustomBlockAnimation(final SwingAnimation swingAnimation, float swingProgress, final float equipProgress, final float interpolatedEquipProgress)
	{
		if (swingAnimation.getBlockStaticSwingProgress().get())
			swingProgress = swingAnimation.getBlockStaticSwingProgressValue().get();

		final float sq = getAnimationProgress(swingProgress, true, false);
		final float sqrt = getAnimationProgress(swingProgress, false, false);
		final float equipProgressAffectness = swingAnimation.getEquipProgressAnimationAffectness().get() * 0.01f;
		final float equipProgressAffect = swingAnimation.getEquipProgressAffectsAnimation().get() ? 1 - equipProgressAffectness + interpolatedEquipProgress * equipProgressAffectness : 1;
		final float blockScale = swingAnimation.getBlockScale().get();

		final Boolean equipProgressAffectTranslation = swingAnimation.getEquipProgressAffectsAnimationTranslation().get();

		final float xEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressAnimationTranslationAffectnessX().get();
		final float yEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressAnimationTranslationAffectnessY().get();
		final float zEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressAnimationTranslationAffectnessZ().get();

		switch (swingAnimation.getAnimationMode().get().toLowerCase())
		{
			case "liquidbounce":
			{
				transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
				doBlockTransformations();
				GlStateManager.translate(-0.5f, 0.2F, 0.0F);
				break;
			}
			case "1.8":
			{
				transformFirstPersonItemBlock(equipProgress, 0, 1);
				doBlockTransformations();
				break;
			}
			case "1.7":
			{
				transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
				doBlockTransformations();
				break;
			}
			case "avatar":
			{
				translateBlock(swingAnimation, swingProgress);

				final double xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
				final double yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
				final double zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

				GlStateManager.translate(xTranslate, yTranslate, zTranslate);

				GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
				GlStateManager.rotate(equipProgressAffect * sqrt * -40.0f, 1.0F, 0.0F, 0.0F);
				GlStateManager.scale(blockScale, blockScale, blockScale);
				doBlockTransformations();
				break;
			}
			case "sigma":
			{
				transformFirstPersonItemBlock(equipProgress, 0, 1);
				GlStateManager.rotate(equipProgressAffect * -sqrt * 27.5f, -8.0f, -0.0f, 9.0F);
				GlStateManager.rotate(equipProgressAffect * -sqrt * 45, 1.0F, sqrt * 0.5f, -0.0f);
				doBlockTransformations();
				GL11.glTranslated(1.2, 0.3, 0.5);
				GL11.glTranslatef(-1, mc.thePlayer.isSneaking() ? -0.1f : -0.2f, 0.2F);
				break;
			}
			case "push":
			{
				transformFirstPersonItemBlock(equipProgress, 0.0F, 1);
				doBlockTransformations();
				GlStateManager.translate(-0.3f, 0.1f, 0.0f);
				GlStateManager.rotate(equipProgressAffect * sqrt * -25.0f, -8.0f, 0.0F, 9.0f);
				break;
			}
			case "tap":
			{
				translateBlock(swingAnimation, swingProgress);

				final double xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
				final double yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
				final double zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

				GlStateManager.translate(xTranslate, yTranslate, zTranslate);

				GlStateManager.rotate(30, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(equipProgressAffect * sqrt * -30.0f, 0.0F, 1.0F, 0.0F);
				GlStateManager.scale(blockScale, blockScale, blockScale);
				doBlockTransformations();
				break;
			}
			case "tap2":
			{
				final float smooth = swingProgress * 0.8f - swingProgress * swingProgress * 0.8f;

				translateBlock(swingAnimation, swingProgress);

				final double xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
				final double yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
				final double zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

				GlStateManager.translate(xTranslate, yTranslate, zTranslate);

				GlStateManager.rotate(45.0f, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(equipProgressAffect * smooth * -90.0f, 0.0F, 1.0F, 0.0F);
				GlStateManager.scale(blockScale, blockScale, blockScale);
				doBlockTransformations();
				break;
			}
			case "slide":
			{
				translateBlock(swingAnimation, swingProgress);

				final double xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * (swingAnimation.getSlideXPos().get() * 0.001);
				final double yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.4f) + equipProgressAffect * sqrt * (swingAnimation.getSlideYPos().get() * 0.01);
				final double zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

				GlStateManager.translate(xTranslate, yTranslate, zTranslate);

				GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(equipProgressAffect * sqrt * swingAnimation.getSlideAngleY().get(), 0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(equipProgressAffect * sqrt * swingAnimation.getSlideAngleZ().get(), 0.0f, 0.0f, 1.0f);
				GlStateManager.rotate(equipProgressAffect * -sqrt * swingAnimation.getSlideAngleX().get(), 1.0f, 0.0f, 0.0f);
				GlStateManager.scale(blockScale, blockScale, blockScale);
				doBlockTransformations();
				break;
			}
			case "exhibobo":
			{
				translateBlock(swingAnimation, swingProgress);

				final double xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
				final double yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.2f) + equipProgressAffect * sqrt * (swingAnimation.getExhiYPushPos().get() * 0.001) + equipProgressAffect * -sq * (swingAnimation.getExhiSmooth().get() * 0.005);
				final double zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * (swingAnimation.getExhiZPushPos().get() * 0.001);

				GlStateManager.translate(xTranslate, yTranslate, zTranslate);

				GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + swingAnimation.getExhiAngleY().get()), 0.0f, 1.0f, 0.0f);
				GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + swingAnimation.getExhiAngleZ().get()), 0.0f, 0.0f, 1.0f);
				GlStateManager.rotate(-equipProgressAffect * sqrt * (20 + swingAnimation.getExhiAngleX().get()), 1.0f, 0.0f, 0.0f);

				GlStateManager.scale(blockScale, blockScale, blockScale);
				doBlockTransformations();
				break;
			}
		}
	}

	private void translate(final SwingAnimation swingAnimation, final float swingProgress)
	{
		if (!swingAnimation.getState())
		{
			GlStateManager.translate(0.56, -0.52, -0.72);
			return;
		}

		double smooth;
		switch (swingAnimation.getXRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double x = 0.56 + swingAnimation.getXTranslation().get() + smooth * swingAnimation.getXRTranslation().get();
		switch (swingAnimation.getYRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double y = -0.52 + swingAnimation.getYTranslation().get() + smooth * swingAnimation.getYRTranslation().get();
		switch (swingAnimation.getZRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double z = -0.72 + swingAnimation.getZTranslation().get() + smooth * swingAnimation.getZRTranslation().get();

		GlStateManager.translate(x, y, z);
	}

	private void translateBlock(final SwingAnimation swingAnimation, final float swingProgress)
	{
		if (!swingAnimation.getState())
		{
			GlStateManager.translate(0.56, -0.52, -0.72);
			return;
		}

		double smooth;
		switch (swingAnimation.getBlockXRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double x = 0.56 + swingAnimation.getBlockXTranslation().get() + smooth * swingAnimation.getBlockXRTranslation().get();
		switch (swingAnimation.getBlockYRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double y = -0.52 + swingAnimation.getBlockYTranslation().get() + smooth * swingAnimation.getBlockYRTranslation().get();
		switch (swingAnimation.getBlockZRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * WMathHelper.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * WMathHelper.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * WMathHelper.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * WMathHelper.PI);
		}

		final double z = -0.72 + swingAnimation.getBlockZTranslation().get() + smooth * swingAnimation.getBlockZRTranslation().get();

		GlStateManager.translate(x, y, z);
	}

	@Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
	private void renderFireInFirstPerson(final CallbackInfo callbackInfo)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if (antiBlind.getState() && antiBlind.getFireEffect().get())
			callbackInfo.cancel();
	}
}
