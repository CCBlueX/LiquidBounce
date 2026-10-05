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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;

@NullMarked
@Mixin(SectionCompiler.class)
public abstract class MixinSectionCompiler {

    /**
     * Captures XRay's state for the whole section build, so the per face and per block checks of
     * {@link net.minecraft.client.renderer.block.ModelBlockRenderer} below read it instead of
     * resolving it again.
     *
     * @see ModuleXRay#renderActive()
     */
    @WrapMethod(method = "compile")
    private SectionCompiler.Results liquidbounce$xrayScope(SectionPos sectionPos, RenderSectionRegion region,
            VertexSorting vertexSorting, SectionBufferBuilderPack builders, Operation<SectionCompiler.Results> original) {
        return ScopedValue.where(ModuleXRay.RENDER_ACTIVE, ModuleXRay.INSTANCE.getRunning())
            .call(() -> original.call(sectionPos, region, vertexSorting, builders));
    }

}
