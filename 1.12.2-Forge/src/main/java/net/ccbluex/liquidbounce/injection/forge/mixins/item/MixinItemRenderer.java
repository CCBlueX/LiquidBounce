/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
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
	@Final
	private Minecraft mc;
	@Shadow
	private ItemStack itemStackOffHand;

	@Shadow
	protected abstract void renderMapFirstPerson(float p_187463_1_, float p_187463_2_, float p_187463_3_);

	@Shadow
	protected abstract void transformFirstPerson(EnumHandSide hand, float swingProgress);

	@Shadow
	protected abstract void transformEatFirstPerson(float p_187454_1_, EnumHandSide hand, ItemStack stack);

	@Shadow
	protected abstract void renderArmFirstPerson(float p_187456_1_, float p_187456_2_, EnumHandSide p_187456_3_);

	@Shadow
	protected abstract void renderMapFirstPersonSide(float p_187465_1_, EnumHandSide hand, float p_187465_3_, ItemStack stack);

	@Shadow
	protected abstract void transformSideFirstPerson(EnumHandSide hand, float p_187459_2_);

	@Shadow
	public abstract void renderItemSide(EntityLivingBase entitylivingbaseIn, ItemStack heldStack, TransformType transform, boolean leftHanded);

	/**
	 * @author CCBlueX (superblaubeere27)
	 */
	@Overwrite
	public void renderItemInFirstPerson(final AbstractClientPlayer player, final float p_187457_2_, final float p_187457_3_, final EnumHand hand, final float p_187457_5_, final ItemStack stack, final float p_187457_7_)
	{
		final boolean flag = hand == EnumHand.MAIN_HAND;
		final EnumHandSide enumhandside = flag ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
		GlStateManager.pushMatrix();

		if (stack.isEmpty())
		{
			if (flag && !player.isInvisible())
			{
				renderArmFirstPerson(p_187457_7_, p_187457_5_, enumhandside);
			}
		}
		else if (stack.getItem() instanceof ItemMap)
		{
			if (flag && itemStackOffHand.isEmpty())
			{
				renderMapFirstPerson(p_187457_3_, p_187457_7_, p_187457_5_);
			}
			else
			{
				renderMapFirstPersonSide(p_187457_7_, enumhandside, p_187457_5_, stack);
			}
		}
		else
		{
			final KillAura killAura = (KillAura) LiquidBounce.moduleManager.get(KillAura.class);

			final boolean flag1 = enumhandside == EnumHandSide.RIGHT;

			if (player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == hand)
			{
				final int j = flag1 ? 1 : -1;

				final EnumAction enumaction = killAura.getBlockingStatus() ? EnumAction.BLOCK : stack.getItemUseAction();

				switch (enumaction)
				{
					case NONE:
					case BLOCK:
						transformSideFirstPerson(enumhandside, p_187457_7_);
						break;
					case EAT:
					case DRINK:
						transformEatFirstPerson(p_187457_2_, enumhandside, stack);
						transformSideFirstPerson(enumhandside, p_187457_7_);
						break;
					case BOW:
						transformSideFirstPerson(enumhandside, p_187457_7_);
						GlStateManager.translate((float) j * -0.2785682F, 0.18344387F, 0.15731531F);
						GlStateManager.rotate(-13.935F, 1.0F, 0.0F, 0.0F);
						GlStateManager.rotate((float) j * 35.3F, 0.0F, 1.0F, 0.0F);
						GlStateManager.rotate((float) j * -9.785F, 0.0F, 0.0F, 1.0F);
						final float f5 = (float) stack.getMaxItemUseDuration() - ((float) mc.player.getItemInUseCount() - p_187457_2_ + 1.0F);
						float f6 = f5 * 0.05f;
						f6 = (f6 * f6 + f6 * 2.0F) / 3.0F;

						if (f6 > 1.0F)
						{
							f6 = 1.0F;
						}

						if (f6 > 0.1F)
						{
							final float f7 = MathHelper.sin((f5 - 0.1F) * 1.3F);
							final float f3 = f6 - 0.1F;
							final float f4 = f7 * f3;
							GlStateManager.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
						}

						GlStateManager.translate(f6 * 0.0F, f6 * 0.0F, f6 * 0.04F);
						GlStateManager.scale(1.0F, 1.0F, 1.0F + f6 * 0.2F);
						GlStateManager.rotate((float) j * 45.0F, 0.0F, -1.0F, 0.0F);
				}
			}
			else
			{
				final float f = -0.4F * MathHelper.sin(MathHelper.sqrt(p_187457_5_) * WMathHelper.PI);
				final float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt(p_187457_5_) * (WMathHelper.PI * 2F));
				final float f2 = -0.2F * MathHelper.sin(p_187457_5_ * WMathHelper.PI);
				final int i = flag1 ? 1 : -1;
				GlStateManager.translate((float) i * f, f1, f2);
				transformSideFirstPerson(enumhandside, p_187457_7_);
				transformFirstPerson(enumhandside, p_187457_5_);
			}

			renderItemSide(player, stack, flag1 ? TransformType.FIRST_PERSON_RIGHT_HAND : TransformType.FIRST_PERSON_LEFT_HAND, !flag1);
		}

		GlStateManager.popMatrix();
	}

	/**
	 * @author CCBlueX
	 */

	@Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
	private void renderFireInFirstPerson(final CallbackInfo callbackInfo)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if (antiBlind.getState() && antiBlind.getFireEffect().get())
			callbackInfo.cancel();
	}
}
