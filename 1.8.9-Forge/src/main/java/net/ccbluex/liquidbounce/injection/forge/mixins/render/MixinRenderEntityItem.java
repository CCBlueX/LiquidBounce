/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Chams;
import net.ccbluex.liquidbounce.features.module.modules.render.ItemPhysics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem
{
	public double rotation;
	public final Random random = new Random();

	@Inject(method = "doRender", at = @At("HEAD"))
	private void injectChamsPre(final EntityItem entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo callbackInfo)
	{
		final Minecraft mc = Minecraft.getMinecraft();
		final Chams chams = (Chams) LiquidBounce.moduleManager.getModule(Chams.class);
		final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.moduleManager.get(ItemPhysics.class);

		if (chams.getState() && chams.getItemsValue().get())
		{
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glPolygonOffset(1.0F, -1000000.0F);
		}

		if (itemPhysics.getState())
		{
			final RenderEntityItem renderer = (RenderEntityItem) (Object) this;
			rotation = (double) (System.nanoTime() - itemPhysics.tick) / 2500000 * itemPhysics.itemRotationSpeed.get();
			if (!mc.inGameHasFocus)
				rotation = 0;
			final EntityItem item = entity;

			final ItemStack itemstack = item.getEntityItem();
			final int i = itemstack.getItem() != null ? Item.getIdFromItem(itemstack.getItem()) + itemstack.getMetadata() : 187;

			random.setSeed(i);

			renderer.bindTexture(TextureMap.locationBlocksTexture);
			renderer.getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);

			GlStateManager.enableRescaleNormal();
			GlStateManager.alphaFunc(516, 0.1F);
			GlStateManager.enableBlend();
			RenderHelper.enableStandardItemLighting();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			GlStateManager.pushMatrix();
			IBakedModel ibakedmodel = mc.getRenderItem().getItemModelMesher().getItemModel(itemstack);
			final boolean flag1 = ibakedmodel.isGui3d();
			final boolean is3D = ibakedmodel.isGui3d();
			final int j = getModelCount(itemstack);

			GlStateManager.translate((float) x, (float) y, (float) z);

			if (ibakedmodel.isGui3d())
				GlStateManager.scale(0.5F, 0.5F, 0.5F);

			GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(item.rotationYaw, 0.0F, 0.0F, 1.0F);

			GlStateManager.translate(0, 0, is3D ? -0.08 : -0.04);

			// Handle Rotations
			if (is3D || mc.getRenderManager().options != null)
			{
				if (is3D)
				{
					if (!item.onGround)
					{
						double _rotation = rotation * 2;
						Fluid fluid = getFluid(item);
						if (fluid == null)
							fluid = getFluid(item, true);
						if (fluid != null)
							_rotation /= fluid.getDensity() / 1000.0 * 10;

						item.rotationPitch += _rotation;
					}
				}
				else if (!Double.isNaN(item.posX) && !Double.isNaN(item.posY) && !Double.isNaN(item.posZ) && item.worldObj != null)
					if (item.onGround)
						item.rotationPitch = 0;
					else
					{
						double _rotation = rotation * 2;
						final Fluid fluid = getFluid(item);
						if (fluid != null)
							_rotation /= fluid.getDensity() / 1000.0 * 10;

						item.rotationPitch += _rotation;
					}

				GlStateManager.rotate(item.rotationPitch, 1, 0, 0.0F);
			}

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			for (int k = 0; k < j; ++k)
				if (flag1)
				{
					GlStateManager.pushMatrix();

					if (k > 0)
					{

						final float f7 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
						final float f9 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
						final float f6 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
						GlStateManager.translate(renderer.shouldSpreadItems() ? f7 : 0, renderer.shouldSpreadItems() ? f9 : 0, f6);
					}

					ibakedmodel = ForgeHooksClient.handleCameraTransforms(ibakedmodel, TransformType.GROUND);
					mc.getRenderItem().renderItem(itemstack, ibakedmodel);
					GlStateManager.popMatrix();
				}
				else
				{
					GlStateManager.pushMatrix();

					ibakedmodel = ForgeHooksClient.handleCameraTransforms(ibakedmodel, TransformType.GROUND);
					mc.getRenderItem().renderItem(itemstack, ibakedmodel);
					GlStateManager.popMatrix();
					GlStateManager.translate(0.0F, 0.0F, 0.05375F);
				}

			GlStateManager.popMatrix();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableBlend();
			renderer.bindTexture(TextureMap.locationBlocksTexture);

			renderer.getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();

			try
			{
				// idk anyelse solution...
				for (final Method method : Render.class.getMethods())
					if ("doRender".equals(method.getName()))
					{
						method.invoke(this, entity, x, y, z, entityYaw, partialTicks);
						break;
					}
			}
			catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e)
			{
				e.printStackTrace();
			}

			if (chams.getState() && chams.getItemsValue().get())
			{
				GL11.glPolygonOffset(1.0F, 1000000.0F);
				GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
			}
			callbackInfo.cancel();
		}
	}

	@Inject(method = "doRender", at = @At("RETURN"))
	private void injectChamsPost(final CallbackInfo callbackInfo)
	{
		final Chams chams = (Chams) LiquidBounce.moduleManager.getModule(Chams.class);
		final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.moduleManager.getModule(ItemPhysics.class);

		if (!itemPhysics.getState() && chams.getState() && chams.getItemsValue().get())
		{
			GL11.glPolygonOffset(1.0F, 1000000.0F);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
		}
	}

	public final int getModelCount(final ItemStack stack)
	{

		if (stack.stackSize > 48)
			return 5;
		else if (stack.stackSize > 32)
			return 4;
		else if (stack.stackSize > 16)
			return 3;
		else if (stack.stackSize > 1)
			return 2;

		return 1;
	}

	public final Fluid getFluid(final EntityItem item)
	{
		return getFluid(item, false);
	}

	public final Fluid getFluid(final EntityItem item, final boolean below)
	{
		final double d0 = item.posY + item.getEyeHeight();
		final int i = MathHelper.floor_double(item.posX);
		int j = MathHelper.floor_float(MathHelper.floor_double(d0));
		if (below)
			j--;
		final int k = MathHelper.floor_double(item.posZ);
		final BlockPos pos = new BlockPos(i, j, k);
		final Block block = item.worldObj.getBlockState(pos).getBlock();

		Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
		if (fluid == null && block instanceof IFluidBlock)
			fluid = ((IFluidBlock) block).getFluid();
		else if (block instanceof BlockLiquid)
			fluid = FluidRegistry.WATER;

		if (below)
			return fluid;

		double filled = 1.0f; // If it's not a liquid assume it's a solid block
		if (block instanceof IFluidBlock)
			filled = ((IFluidBlock) block).getFilledPercentage(item.worldObj, pos);

		if (filled < 0)
		{
			filled *= -1;
			// filled -= 0.11111111F; //Why this is needed.. not sure...
			if (d0 > j + (1 - filled))
				return fluid;
		}
		else if (d0 < j + filled)
			return fluid;
		return null;
	}
}
