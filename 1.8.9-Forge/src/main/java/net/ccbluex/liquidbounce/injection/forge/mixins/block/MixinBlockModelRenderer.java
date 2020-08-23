/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
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

import java.util.Objects;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {

    @Inject(method = "renderModelAmbientOcclusion", at = @At("HEAD"), cancellable = true)
    private void renderModelAmbientOcclusion(IBlockAccess blockAccessIn, IBakedModel modelIn, Block blockIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSide, final CallbackInfoReturnable<Boolean> booleanCallbackInfoReturnable) {
        final XRay xray = (XRay) LiquidBounce.moduleManager.getModule(XRay.class);

        if (Objects.requireNonNull(xray).getState() && !xray.getXrayBlocks().contains(blockIn))
            booleanCallbackInfoReturnable.setReturnValue(false);
    }

    @Inject(method = "renderModelStandard", at = @At("HEAD"), cancellable = true)
    private void renderModelStandard(IBlockAccess blockAccessIn, IBakedModel modelIn, Block blockIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides, final CallbackInfoReturnable<Boolean> booleanCallbackInfoReturnable) {
        final XRay xray = (XRay) LiquidBounce.moduleManager.getModule(XRay.class);

        if (Objects.requireNonNull(xray).getState() && !xray.getXrayBlocks().contains(blockIn))
            booleanCallbackInfoReturnable.setReturnValue(false);
    }
}
