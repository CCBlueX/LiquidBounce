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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.ccbluex.liquidbounce.common.XRayBlockRenderContext;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@NullMarked
@Mixin(ModelBlockRenderer.class)
public abstract class MixinModelBlockRenderer {

    @Shadow
    private QuadInstance quadInstance;

    @WrapMethod(method = "tesselateBlock")
    private void wrapXRayTransparentBackground(BlockQuadOutput output, float x, float y, float z,
            BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed,
            Operation<Void> original) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            original.call(output, x, y, z, level, pos, state, model, seed);
            return;
        }

        XRayBlockRenderContext.renderTransparentBackground(module.transparentBackgroundAlpha(state),
            () -> original.call(output, x, y, z, level, pos, state, model, seed));
    }

    @Inject(method = {"tesselateFlat", "tesselateAmbientOcclusion"}, at = @At("HEAD"), cancellable = true)
    private void injectXRaySkipHiddenBlocks(BlockQuadOutput output, float x, float y, float z,
            List<BlockStateModelPart> parts, BlockAndTintGetter level, BlockState state, BlockPos pos,
            CallbackInfo ci) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            return;
        }

        if (module.shouldSkipRender(state, pos)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "tesselateBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;ambientOcclusion:Z", opcode = Opcodes.GETFIELD))
    private static boolean injectXRayFullBright(boolean original) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.getFullBright()) {
            return original;
        }

        return false;
    }

    @ModifyReturnValue(method = "forceOpaque", at = @At("RETURN"))
    private static boolean injectXRayTransparentBackgroundForceTranslucent(boolean original, boolean cutoutLeaves,
            BlockState state) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.shouldRenderTransparentBackground(state)) {
            return original;
        }

        return false;
    }

    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
    private boolean injectXRayDrawSide(boolean original, BlockAndTintGetter level, BlockState state, Direction direction,
            BlockPos neighborPos) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            return original;
        }

        return module.modifyDrawSide(state, level, neighborPos.relative(direction.getOpposite()), direction, original);
    }

    @Inject(
        method = "putQuadWithTint",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V")
    )
    private void injectXRayTransparentBackgroundAlpha(BlockQuadOutput output, float x, float y, float z,
            BlockAndTintGetter level, BlockState state, BlockPos pos,
            net.minecraft.client.resources.model.geometry.BakedQuad quad, CallbackInfo ci) {
        XRayBlockRenderContext.applyAlpha(this.quadInstance);
    }

}
