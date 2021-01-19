/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
	protected abstract void rotateArroundXAndY(float angle, float angleY);

	@Shadow
	protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

	@Shadow
	protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

	@Shadow
	private ItemStack itemToRender;

	@Shadow
	protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

	@Shadow
	protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

	@Shadow
	protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

	@Shadow
	protected abstract void doBlockTransformations();

	@Shadow
	protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

	@Shadow
	protected abstract void doItemUsedTransformations(float swingProgress);

	@Shadow
	public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, TransformType transform);

	@Shadow
	protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	public void renderItemInFirstPerson(final float partialTicks)
	{
		final float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
		final EntityPlayerSP abstractclientplayer = mc.thePlayer;
		final float f1 = abstractclientplayer.getSwingProgress(partialTicks);
		final float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
		final float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
		rotateArroundXAndY(f2, f3);
		setLightMapFromPlayer(abstractclientplayer);
		rotateWithPlayerRotations(abstractclientplayer, partialTicks);
		GlStateManager.enableRescaleNormal();
		GlStateManager.pushMatrix();

		if (itemToRender != null)
		{
			final KillAura killAura = (KillAura) LiquidBounce.moduleManager.getModule(KillAura.class);

			if (itemToRender.getItem() instanceof ItemMap)
			{
				renderItemMap(abstractclientplayer, f2, f, f1);
			}
			else if (abstractclientplayer.getItemInUseCount() > 0 || itemToRender.getItem() instanceof ItemSword && killAura.getBlockingStatus())
			{
				final EnumAction enumaction = killAura.getBlockingStatus() ? EnumAction.BLOCK : itemToRender.getItemUseAction();

				switch (enumaction)
				{
					case NONE:
						transformFirstPersonItem(f, 0.0F);
						break;
					case EAT:
					case DRINK:
						performDrinking(abstractclientplayer, partialTicks);
						transformFirstPersonItem(f, f1);
						break;
					case BLOCK:
						transformFirstPersonItem(f + 0.1F, f1);
						doBlockTransformations();
						GlStateManager.translate(-0.5F, 0.2F, 0.0F);
						break;
					case BOW:
						transformFirstPersonItem(f, f1);
						doBowTransformations(partialTicks, abstractclientplayer);
				}
			}
			else
			{
				if (!LiquidBounce.moduleManager.getModule(SwingAnimation.class).getState())
					doItemUsedTransformations(f1);
				transformFirstPersonItem(f, f1);
			}

			renderItem(abstractclientplayer, itemToRender, TransformType.FIRST_PERSON);
		}
		else if (!abstractclientplayer.isInvisible())
		{
			renderPlayerArm(abstractclientplayer, f, f1);
		}

		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
	}

	@Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
	private void renderFireInFirstPerson(final CallbackInfo callbackInfo)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.getModule(AntiBlind.class);

		if (antiBlind.getState() && antiBlind.getFireEffect().get())
			callbackInfo.cancel();
	}
}
