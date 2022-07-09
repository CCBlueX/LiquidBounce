/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.block.BlockImplKt;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer
{
    @Inject(method = "renderModelAmbientOcclusion", at = @At("HEAD"), cancellable = true)
    private void renderModelAmbientOcclusion_injectXRay(final IBlockAccess blockAccessIn, final IBakedModel modelIn, final Block blockIn, final BlockPos blockPosIn, final WorldRenderer worldRendererIn, final boolean checkSide, final CallbackInfoReturnable<? super Boolean> booleanCallbackInfoReturnable)
    {
        // XRay
        final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

        if (xray.getState() && !xray.canBeRendered(BlockImplKt.wrap(blockIn), null))
            booleanCallbackInfoReturnable.setReturnValue(false);
    }

    @Inject(method = "renderModelStandard", at = @At("HEAD"), cancellable = true)
    private void renderModelStandard_injectXRay(final IBlockAccess blockAccessIn, final IBakedModel modelIn, final Block blockIn, final BlockPos blockPosIn, final WorldRenderer worldRendererIn, final boolean checkSides, final CallbackInfoReturnable<? super Boolean> booleanCallbackInfoReturnable)
    {
        // XRay
        final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

        if (xray.getState() && !xray.canBeRendered(BlockImplKt.wrap(blockIn), null)) // #298 Bugfix
            booleanCallbackInfoReturnable.setReturnValue(false);
    }
}
