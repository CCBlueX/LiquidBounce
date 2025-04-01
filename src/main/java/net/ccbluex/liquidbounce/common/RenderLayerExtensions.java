/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import org.apache.commons.lang3.RandomStringUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.TRANSLUCENT_TARGET;
import static net.minecraft.client.render.RenderPhase.Texture;

/**
 * Extensions to RenderLayer for custom render layers.
 */
public class RenderLayerExtensions {

    /**
     * Blend mode for JCEF compatible blending.
     */
	private static final BlendFunction JCEF_COMPATIBLE_BLEND = new BlendFunction(
			SourceFactor.ONE,
			DestFactor.ONE_MINUS_SRC_ALPHA
	);

    /**
     * Render Layer for smoother textures using bilinear filtering.
     */
    private static final Function<Identifier, RenderLayer> SMOOTH_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "smooth_textured",
							786432,
							RenderPipeline.builder()
									.withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR,
											VertexFormat.DrawMode.QUADS)
									// TODO: these are required, if you remove them, it will throw an exception
									.withLocation(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.withVertexShader(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.withFragmentShader(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.build(),
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new RenderPhase.Texture(textureId, TriState.DEFAULT, false))
									.target(TRANSLUCENT_TARGET)
                                    .build(false)
                    ));

    /**
     * Render Layer for elements that need to be rendered to the blur framebuffer.
     */
    private static final Function<Identifier, RenderLayer> BLURRED_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "blurred_ui_layer",
							786432,
							RenderPipeline.builder()
									.withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
									.withBlend(JCEF_COMPATIBLE_BLEND)
									.withLocation(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.withVertexShader(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.withFragmentShader(RandomStringUtils.randomAlphabetic(12).toLowerCase() + "_lb")
									.build(),
							RenderLayer.MultiPhaseParameters.builder()
									.texture(new Texture(textureId, TriState.FALSE, false))
									.target(BlurEffectRenderer.getOutlineTarget())
									.build(false)
                    ));

    public static RenderLayer getSmoothTextureLayer(Identifier textureId) {
        return SMOOTH_TEXTURE_LAYER.apply(textureId);
    }

    public static RenderLayer getBlurredTextureLayer(Identifier textureId) {
        return BLURRED_TEXTURE_LAYER.apply(textureId);
    }

}
