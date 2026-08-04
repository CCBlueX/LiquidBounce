/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.ccbluex.liquidbounce.common.XRayBlockRenderContext;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class MixinSodiumBlockRenderer {

    @WrapMethod(method = "renderModel")
    private void wrapXRayTransparentBackground(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin,
            Operation<Void> original) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            original.call(model, state, pos, origin);
            return;
        }

        XRayBlockRenderContext.renderTransparentBackground(module.transparentBackgroundAlpha(state),
            () -> original.call(model, state, pos, origin));
    }

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void injectXRaySkipHiddenBlocks(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin,
            CallbackInfo ci) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            return;
        }

        if (module.shouldSkipRender(state, pos)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "processQuad", at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;forceOpaque:Z", opcode = Opcodes.GETFIELD))
    private boolean injectXRayTransparentBackgroundDisableForceOpaque(boolean original) {
        return XRayBlockRenderContext.isRenderingTransparentBackground() ? false : original;
    }

    @ModifyExpressionValue(method = "processQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getRenderType()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"))
    private ChunkSectionLayer injectXRayTransparentBackgroundLayer(ChunkSectionLayer original) {
        return XRayBlockRenderContext.forceTranslucentLayer(original);
    }

    @Inject(
        method = "processQuad",
        at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;shadeQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;Lnet/caffeinemc/mods/sodium/client/model/light/LightMode;ZLnet/caffeinemc/mods/sodium/client/render/model/SodiumShadeMode;)V")
    )
    private void injectXRayTransparentBackgroundAlpha(MutableQuadViewImpl quad, CallbackInfo ci) {
        if (!XRayBlockRenderContext.isRenderingTransparentBackground()) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            quad.setColor(i, XRayBlockRenderContext.applyAlpha(quad.baseColor(i)));
        }
    }

}
