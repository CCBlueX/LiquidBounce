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
import net.ccbluex.liquidbounce.common.XRayBlockRenderContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedQuad.MaterialInfo.class)
public abstract class MixinBakedQuadMaterialInfo {

    @ModifyReturnValue(method = "layer", at = @At("RETURN"))
    private ChunkSectionLayer injectXRayTransparentBackgroundLayer1(ChunkSectionLayer original) {
        return XRayBlockRenderContext.forceTranslucentLayer(original);
    }

    @ModifyExpressionValue(method = "flags", at = @At(value = "FIELD", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;layer:Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;", opcode = Opcodes.GETFIELD))
    private ChunkSectionLayer injectXRayTransparentBackgroundLayer2(ChunkSectionLayer original) {
        return XRayBlockRenderContext.forceTranslucentLayer(original);
    }

}
