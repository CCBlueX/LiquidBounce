/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.ccbluex.liquidbounce.injection.backend.BlockImplKt;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcher
{

	@Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
	private void xray(final TileEntity tileentityIn, final float partialTicks, final int destroyStage, final CallbackInfo callbackInfo)
	{
		final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

		if (xray.getState() && !xray.canBeRendered(BackendExtentionsKt.wrap(tileentityIn.getPos()), BlockImplKt.wrap(tileentityIn.getBlockType()))) // #298 Bugfix
			callbackInfo.cancel();
	}
}
