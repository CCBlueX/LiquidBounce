/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.ccbluex.liquidbounce.injection.backend.BlockImplKt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer
{
	@Inject(method = "renderModelSmooth", at = @At("HEAD"), cancellable = true)
	public void renderModelSmooth(final IBlockAccess worldIn, final IBakedModel modelIn, final IBlockState stateIn, final BlockPos posIn, final BufferBuilder buffer, final boolean checkSides, final long rand, final CallbackInfoReturnable<? super Boolean> cir)
	{
		final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

		if (xray.getState() && !xray.getXrayBlocks().contains(BlockImplKt.wrap(stateIn.getBlock())))
			cir.setReturnValue(false);
	}

	@Inject(method = "renderModelFlat", at = @At("HEAD"), cancellable = true)
	private void renderModelStandard(final IBlockAccess worldIn, final IBakedModel modelIn, final IBlockState stateIn, final BlockPos posIn, final BufferBuilder buffer, final boolean checkSides, final long rand, final CallbackInfoReturnable<? super Boolean> booleanCallbackInfoReturnable)
	{
		final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

		if (xray.getState() && !xray.getXrayBlocks().contains(BlockImplKt.wrap(stateIn.getBlock())))
			booleanCallbackInfoReturnable.setReturnValue(false);
	}
}
