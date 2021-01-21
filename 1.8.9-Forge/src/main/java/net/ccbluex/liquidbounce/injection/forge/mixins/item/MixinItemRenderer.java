/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import net.ccbluex.liquidbounce.LiquidBounce;
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

@SuppressWarnings({"WeakerAccess", "MethodMayBeStatic", "DesignForExtension", "NumericCastThatLosesPrecision"})
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
	 * @reason sword block rotation angle
	 */
	@Overwrite
	protected void doBlockTransformations()
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
	 * @reason sword swing animation center point modification
	 * @see    SwingAnimation
	 */
	@Overwrite
	protected void transformFirstPersonItem(final float equipProgress, final float swingProgress)
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		final float fixedSwingProgress = sa.getState() && sa.getStaticSwingProgress().get() ? sa.getStaticSwingProgressValue().get() : swingProgress;
		final float sq = getAnimationProgress(fixedSwingProgress, true, true);
		final float sqrt = getAnimationProgress(fixedSwingProgress, false, true);

		translate(sa, fixedSwingProgress);
		GlStateManager.translate(0.0F, equipProgress * -0.6f, 0.0F);
		GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(sq * -20.0f, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(sqrt * -80.0f, 1.0F, 0.0F, 0.0F);
		final float scale = sa.getState() ? sa.getScale().get() : 0.4f;
		GlStateManager.scale(scale, scale, scale);
	}

	private float getAnimationProgress(final float swingProgress, final boolean sq, final boolean isswing)
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		if (sq)
		{
			if (sa.getState())
			{
				final float f;
				switch ((isswing ? sa.getSwingSqSmoothingMethod() : sa.getBlockSqSwingSmoothingMethod()).get().toLowerCase())
				{
					case "sqrt":
						f = MathHelper.sqrt_float(swingProgress);
						break;
					case "sqrtsqrt":
						f = MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress));
						break;
					case "sq":
						f = swingProgress * swingProgress;
						break;
					case "sqsq":
						f = swingProgress * swingProgress * swingProgress;
						break;
					default:
						f = swingProgress;
				}
				return (isswing ? sa.getSwingSqSmoothingSin() : sa.getBlockSqSmoothingSin()).get() ? MathHelper.sin(f * (float) Math.PI) : f;
			}
			return MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
		}
		if (sa.getState())
		{
			final float f;
			switch ((isswing ? sa.getSwingSqrtSmoothingMethod() : sa.getBlockSqrtSwingSmoothingMethod()).get().toLowerCase())
			{
				case "sqrt":
					f = MathHelper.sqrt_float(swingProgress);
					break;
				case "sqrtsqrt":
					f = MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress));
					break;
				case "sq":
					f = swingProgress * swingProgress;
					break;
				case "sqsq":
					f = swingProgress * swingProgress * swingProgress;
					break;
				default:
					f = swingProgress;
			}
			return (isswing ? sa.getSwingSqrtSmoothingSin() : sa.getBlockSqrtSmoothingSin()).get() ? MathHelper.sin(f * (float) Math.PI) : f;
		}
		return MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
	}

	/**
	 * Performs transformations prior to the rendering of a held item in first person.
	 *
	 * @author Mojang
	 * @see    SwingAnimation
	 */
	protected void transformFirstPersonItemBlock(final float equipProgress, final float swingProgress, final float equipProgressAffect)
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		final float sq = getAnimationProgress(swingProgress, true, false);
		final float sqrt = getAnimationProgress(swingProgress, false, false);
		translateBlock(sa, swingProgress);
		final double tX = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
		final double tY = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.6f);
		final double tZ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);
		GlStateManager.translate(tX, tY, tZ);
		GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(equipProgressAffect * sqrt * -80.0f, 1.0F, 0.0F, 0.0F);
		final float scale = sa.getState() ? sa.getBlockScale().get() : 0.4f;
		GlStateManager.scale(scale, scale, scale);
	}

	/**
	 * @author CCBlueX
	 * @reason Swing Animations
	 * @see    KillAura
	 * @see    SwingAnimation
	 */
	@Overwrite
	public void renderItemInFirstPerson(final float partialTicks)
	{
		final float interpolatedEquipProgress = prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
		final EntityPlayerSP abstractclientplayer = mc.thePlayer;
		float swingProgress = abstractclientplayer.getSwingProgress(partialTicks);
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
			final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
			if (sa.getState())
				equipProgress *= sa.getEquipProgressAffectMultiplier().get();

			if (itemToRender.getItem() instanceof ItemMap)
				renderItemMap(abstractclientplayer, smoothPitchRotation, equipProgress, swingProgress);
			else // Swing
			if (abstractclientplayer.getItemInUseCount() > 0 || itemToRender.getItem() instanceof ItemSword && (killAura.getClientSideBlockingStatus() || tpaura.clientSideBlockingStatus))
			{
				final EnumAction enumaction = killAura.getClientSideBlockingStatus()/* || tpaura.blockingRenderStatus */ ? EnumAction.BLOCK : itemToRender.getItemUseAction();

				switch (enumaction)
				{
					case NONE:
						transformFirstPersonItem(equipProgress, 0.0F);
						break;
					case EAT:
					case DRINK:
						performDrinking(abstractclientplayer, partialTicks);
						transformFirstPersonItem(equipProgress, swingProgress);
						break;
					case BLOCK:
						// Sword Blocking
						if (sa.getState())
						{
							if (sa.getBlockStaticSwingProgress().get())
								swingProgress = sa.getBlockStaticSwingProgressValue().get();

							final float sq = getAnimationProgress(swingProgress, true, false);
							final float sqrt = getAnimationProgress(swingProgress, false, false);
							final float equipProgressAffectness = sa.getEquipProgressAnimationAffectness().get() / 100.0F;
							final float equipProgressAffect = sa.getEquipProgressAffectsAnimation().get() ? 1 - equipProgressAffectness + interpolatedEquipProgress * equipProgressAffectness : 1;

							switch (sa.getAnimationMode().get().toLowerCase())
							{
								case "liquidbounce":
									transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
									doBlockTransformations();
									GlStateManager.translate(-0.5f, 0.2F, 0.0F);
									break;
								case "1.8":
									transformFirstPersonItemBlock(equipProgress, 0, 1);
									doBlockTransformations();
									break;
								case "1.7":
									transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
									doBlockTransformations();
									break;
								case "avatar":
									translateBlock(sa, swingProgress);
									final double tX = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
									final double tY = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.6f);
									final double tZ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);
									GlStateManager.translate(tX, tY, tZ);

									GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
									GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
									GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
									GlStateManager.rotate(equipProgressAffect * sqrt * -40.0f, 1.0F, 0.0F, 0.0F);
									GlStateManager.scale(sa.getBlockScale().get(), sa.getBlockScale().get(), sa.getBlockScale().get());
									doBlockTransformations();
									break;
								case "sigma":
									transformFirstPersonItemBlock(equipProgress, 0, 1);
									GlStateManager.rotate(equipProgressAffect * -sqrt * 55 / 2.0F, -8.0f, -0.0f, 9.0F);
									GlStateManager.rotate(equipProgressAffect * -sqrt * 45, 1.0F, sqrt / 2, -0.0f);
									doBlockTransformations();
									GL11.glTranslated(1.2, 0.3, 0.5);
									GL11.glTranslatef(-1, mc.thePlayer.isSneaking() ? -0.1f : -0.2f, 0.2F);
									break;
								case "push":
									transformFirstPersonItemBlock(equipProgress, 0.0F, 1);
									doBlockTransformations();
									GlStateManager.translate(-0.3f, 0.1f, 0.0f);
									GlStateManager.rotate(equipProgressAffect * sqrt * -25.0f, -8.0f, 0.0F, 9.0f);
									break;
								case "tap":
									translateBlock(sa, swingProgress);
									final double tX_ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
									final double tY_ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.6f);
									final double tZ_ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);
									GlStateManager.translate(tX_, tY_, tZ_);

									GlStateManager.rotate(30, 0.0F, 1.0F, 0.0F);
									GlStateManager.rotate(equipProgressAffect * sqrt * -30.0f, 0.0F, 1.0F, 0.0F);
									GlStateManager.scale(sa.getBlockScale().get(), sa.getBlockScale().get(), sa.getBlockScale().get());
									doBlockTransformations();
									break;
								case "tap2":
									final float smooth = swingProgress * 0.8f - swingProgress * swingProgress * 0.8f;
									translateBlock(sa, swingProgress);
									final double tX__ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
									final double tY__ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.6f);
									final double tZ__ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);
									GlStateManager.translate(tX__, tY__, tZ__);

									GlStateManager.rotate(45.0f, 0.0F, 1.0F, 0.0F);
									GlStateManager.rotate(equipProgressAffect * smooth * -90.0f, 0.0F, 1.0F, 0.0F);
									GlStateManager.scale(sa.getBlockScale().get(), sa.getBlockScale().get(), sa.getBlockScale().get());
									doBlockTransformations();
									break;
								case "slide":
									translateBlock(sa, swingProgress);
									final double tX___ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f) + equipProgressAffect * sqrt * (sa.getSlideXPos().get() * 0.001);
									final double tY___ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.4f) + equipProgressAffect * sqrt * (sa.getSlideYPos().get() * 0.01);
									final double tZ___ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f);
									GlStateManager.translate(tX___, tY___, tZ___);

									GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
									GlStateManager.rotate(equipProgressAffect * sqrt * sa.getSlideAngleY().get(), 0.0f, 1.0f, 0.0f);
									GlStateManager.rotate(equipProgressAffect * sqrt * sa.getSlideAngleZ().get(), 0.0f, 0.0f, 1.0f);
									GlStateManager.rotate(equipProgressAffect * -sqrt * sa.getSlideAngleX().get(), 1.0f, 0.0f, 0.0f);
									GlStateManager.scale(sa.getBlockScale().get(), sa.getBlockScale().get(), sa.getBlockScale().get());
									doBlockTransformations();
									break;
								case "exhibobo":
									translateBlock(sa, swingProgress);
									final double tX____ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessX().get() : 0.0f);
									final double tY____ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessY().get() : -0.2f) + equipProgressAffect * sqrt * (sa.getExhiYPushPos().get() * 0.001) + equipProgressAffect * -sq * (sa.getExhiSmooth().get() * 0.005);
									final double tZ____ = equipProgress * (sa.getEquipProgressAffectsAnimationTranslation().get() ? -sa.getEquipProgressAnimationTranslationAffectnessZ().get() : 0.0f) + equipProgressAffect * sqrt * (sa.getExhiZPushPos().get() * 0.001);
									GlStateManager.translate(tX____, tY____, tZ____);

									GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
									GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + sa.getExhiAngleY().get()), 0.0f, 1.0f, 0.0f);
									GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + sa.getExhiAngleZ().get()), 0.0f, 0.0f, 1.0f);
									GlStateManager.rotate(-equipProgressAffect * sqrt * (20 + sa.getExhiAngleX().get()), 1.0f, 0.0f, 0.0f);

									GlStateManager.scale(sa.getBlockScale().get(), sa.getBlockScale().get(), sa.getBlockScale().get());
									doBlockTransformations();
									break;
							}
						}
						else
						{
							transformFirstPersonItem(equipProgress, swingProgress);
							doBlockTransformations();
						}
						break;
					case BOW:
						transformFirstPersonItem(equipProgress, swingProgress);
						doBowTransformations(partialTicks, abstractclientplayer);
				}
			}
			else if (sa.getState() && sa.getSmoothSwing().get())
				transformFirstPersonItem(equipProgress, swingProgress);
			else
			{
				doItemUsedTransformations(swingProgress);
				transformFirstPersonItem(equipProgress, swingProgress);
			}

			renderItem(abstractclientplayer, itemToRender, TransformType.FIRST_PERSON);
		}
		else if (!abstractclientplayer.isInvisible())
			renderPlayerArm(abstractclientplayer, equipProgress, swingProgress);

		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
	}

	private void translate(final SwingAnimation sa, final float swingProgress)
	{
		if (!sa.getState())
		{
			GlStateManager.translate(0.56, -0.52, -0.72);
			return;
		}
		double smooth;
		switch (sa.getXRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double x = 0.56 + sa.getXTranslation().get() + smooth * sa.getXRTranslation().get();
		switch (sa.getYRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double y = -0.52 + sa.getYTranslation().get() + smooth * sa.getYRTranslation().get();
		switch (sa.getZRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double z = -0.72 + sa.getZTranslation().get() + smooth * sa.getZRTranslation().get();
		GlStateManager.translate(x, y, z);
	}

	private void translateBlock(final SwingAnimation sa, final float swingProgress)
	{
		if (!sa.getState())
		{
			GlStateManager.translate(0.56, -0.52, -0.72);
			return;
		}
		double smooth;
		switch (sa.getBlockXRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double x = 0.56 + sa.getBlockXTranslation().get() + smooth * sa.getBlockXRTranslation().get();
		switch (sa.getBlockYRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double y = -0.52 + sa.getBlockYTranslation().get() + smooth * sa.getBlockYRTranslation().get();
		switch (sa.getBlockZRTranslationSmoothingMethod().get().toLowerCase())
		{
			case "sqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
				break;
			case "sqrtsqrt":
				smooth = MathHelper.sin(MathHelper.sqrt_float(MathHelper.sqrt_float(swingProgress)) * (float) Math.PI);
				break;
			case "sq":
				smooth = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
				break;
			case "sqsq":
				smooth = MathHelper.sin(swingProgress * swingProgress * swingProgress * (float) Math.PI);
				break;
			case "none":
				smooth = swingProgress;
				break;
			case "reverse":
				smooth = 1 - swingProgress;
				break;
			default:
				smooth = MathHelper.sin(swingProgress * (float) Math.PI);
		}
		final double z = -0.72 + sa.getBlockZTranslation().get() + smooth * sa.getBlockZRTranslation().get();
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
