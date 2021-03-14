/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LayerHeldItem.class)
@SideOnly(Side.CLIENT)
public abstract class MixinLayerHeldItem
{

	@Shadow
	@Final
	protected RenderLivingBase<?> livingEntityRenderer;

	@Shadow
	protected abstract void renderHeldItem(EntityLivingBase p_188358_1_, ItemStack p_188358_2_, TransformType p_188358_3_, EnumHandSide handSide);

	/**
	 * @author CCBlueX
	 */
	// TODO This method got lost while porting
	@Overwrite
	public void doRenderLayer(final EntityLivingBase entitylivingbaseIn, final float p_177141_2_, final float p_177141_3_, final float partialTicks, final float p_177141_5_, final float p_177141_6_, final float p_177141_7_, final float scale)
	{
		final boolean flag = entitylivingbaseIn.getPrimaryHand() == EnumHandSide.RIGHT;
		final ItemStack itemstack = flag ? entitylivingbaseIn.getHeldItemOffhand() : entitylivingbaseIn.getHeldItemMainhand();
		final ItemStack itemstack1 = flag ? entitylivingbaseIn.getHeldItemMainhand() : entitylivingbaseIn.getHeldItemOffhand();

		if (!itemstack.isEmpty() || !itemstack1.isEmpty())
		{
			GlStateManager.pushMatrix();

			if (livingEntityRenderer.getMainModel().isChild)
			{
				final float f = 0.5F;
				GlStateManager.translate(0.0F, 0.75F, 0.0F);
				GlStateManager.scale(0.5F, 0.5F, 0.5F);
			}

			renderHeldItem(entitylivingbaseIn, itemstack1, TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
			renderHeldItem(entitylivingbaseIn, itemstack, TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
			GlStateManager.popMatrix();
		}
	}
}
