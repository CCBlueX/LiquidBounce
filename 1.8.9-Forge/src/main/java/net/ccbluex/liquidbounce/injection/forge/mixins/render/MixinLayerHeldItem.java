/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LayerHeldItem.class)
@SideOnly(Side.CLIENT)
public class MixinLayerHeldItem
{
	@Shadow
	@Final
	private RendererLivingEntity<?> livingEntityRenderer;

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	public void doRenderLayer(final EntityLivingBase entitylivingbaseIn, final float p_177141_2_, final float p_177141_3_, final float partialTicks, final float p_177141_5_, final float p_177141_6_, final float p_177141_7_, final float scale)
	{
		ItemStack itemstack = entitylivingbaseIn.getHeldItem();

		if (itemstack != null)
		{
			GlStateManager.pushMatrix();

			if (livingEntityRenderer.getMainModel().isChild)
			{
				GlStateManager.translate(0.0F, 0.625F, 0.0F);
				GlStateManager.rotate(-20.0F, -1.0F, 0.0F, 0.0F);

				final float childModelScale = 0.5F;
				GlStateManager.scale(childModelScale, childModelScale, childModelScale);
			}

			final UUID uuid = entitylivingbaseIn.getUniqueID();
			final EntityPlayer entityplayer = Minecraft.getMinecraft().theWorld.getPlayerEntityByUUID(uuid);

			final ModelBiped mainModel = (ModelBiped) livingEntityRenderer.getMainModel();
			if (entityplayer != null && entityplayer.isBlocking())
				if (entitylivingbaseIn.isSneaking())
				{
					mainModel.postRenderArm(0.0325F);

					GlStateManager.translate(-0.58F, 0.3F, -0.2F);
					GlStateManager.rotate(-24390.0F, 137290.0F, -2009900.0F, -2054900.0F);
				}
				else
				{
					mainModel.postRenderArm(0.0325F);

					GlStateManager.translate(-0.48F, 0.2F, -0.2F);
					GlStateManager.rotate(-24390.0F, 137290.0F, -2009900.0F, -2054900.0F);
				}
			else
				mainModel.postRenderArm(0.0625F);

			GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);

			if (entitylivingbaseIn instanceof EntityPlayer && ((EntityPlayer) entitylivingbaseIn).fishEntity != null)
				itemstack = new ItemStack(Items.fishing_rod, 0);

			final Item item = itemstack.getItem();
			final Minecraft minecraft = Minecraft.getMinecraft();

			if (item instanceof ItemBlock && Block.getBlockFromItem(item).getRenderType() == 2)
			{
				GlStateManager.translate(0.0F, 0.1875F, -0.3125F);
				GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
				final float f1 = 0.375F;
				GlStateManager.scale(-f1, -f1, f1);
			}

			if (entitylivingbaseIn.isSneaking())
				GlStateManager.translate(0.0F, 0.203125F, 0.0F);

			minecraft.getItemRenderer().renderItem(entitylivingbaseIn, itemstack, TransformType.THIRD_PERSON);
			GlStateManager.popMatrix();
		}
	}
}
