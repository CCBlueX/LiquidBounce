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
package net.ccbluex.liquidbounce.common;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.ARGB;

/**
 * Scoped context for XRay background block rendering.
 *
 * @see net.minecraft.client.renderer.chunk.SectionCompiler#compile
 * @see net.minecraft.client.renderer.chunk.ChunkSectionLayer#byTransparency
 * @see net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo#layer
 * @see net.minecraft.client.renderer.block.ModelBlockRenderer#putQuadWithTint
 * @see net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer#processQuad
 */
public final class XRayBlockRenderContext {

    private static final ScopedValue<Integer> BACKGROUND_ALPHA = ScopedValue.newInstance();

    private XRayBlockRenderContext() {
    }

    public static boolean isRenderingTransparentBackground() {
        return BACKGROUND_ALPHA.isBound();
    }

    public static ChunkSectionLayer forceTranslucentLayer(ChunkSectionLayer original) {
        return isRenderingTransparentBackground() ? ChunkSectionLayer.TRANSLUCENT : original;
    }

    public static void renderTransparentBackground(int alpha, Runnable render) {
        if (alpha >= 255) {
            render.run();
            return;
        }

        ScopedValue.where(BACKGROUND_ALPHA, alpha).run(render);
    }

    public static void applyAlpha(QuadInstance quadInstance) {
        if (!isRenderingTransparentBackground()) {
            return;
        }

        float alpha = BACKGROUND_ALPHA.get() / 255f;
        for (int i = 0; i < 4; i++) {
            quadInstance.setColor(i, ARGB.multiplyAlpha(quadInstance.getColor(i), alpha));
        }
    }

    public static int applyAlpha(int color) {
        float alpha = BACKGROUND_ALPHA.get() / 255f;
        return ARGB.multiplyAlpha(color, alpha);
    }

}
