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

import net.ccbluex.liquidbounce.render.ClientRenderPipelines;
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer;
import net.ccbluex.liquidbounce.render.shader.CustomShaderProgramPhase;
import net.ccbluex.liquidbounce.render.shader.shaders.BgraPositionTexColorShader;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;

/**
 * Extensions to RenderLayer for custom render layers.
 *
 * FIXME: rewrite ShaderProgram
 */
public class RenderLayerExtensions {

//    private static final RenderPhase.ShaderProgram BGRA_POSITION_TEXTURE_COLOR_PROGRAM = new CustomShaderProgramPhase(
//            BgraPositionTexColorShader.INSTANCE,
//            BgraPositionTexColorShader.INSTANCE.getUniforms(),
//            BgraPositionTexColorShader.INSTANCE.getSamples()
//    );

    /**
     * Render Layer for smoother textures using bilinear filtering.
     */
    private static final Function<Identifier, RenderLayer> SMOOTH_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "smooth_textured",
                            786432,
                            ClientRenderPipelines.SMOOTH_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new RenderPhase.Texture(textureId, TriState.DEFAULT, false))
//                                    .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
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
                            ClientRenderPipelines.BLURRED_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
//                                    .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                                    .target(BlurEffectRenderer.getOutlineTarget())
                                    .build(false)
                    ));

    /**
     * Render Layer for BGRA textures.
     */
    private static final Function<Identifier, RenderLayer> BGRA_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "bgra_texture_layer",
                            786432,
                            ClientRenderPipelines.BGRA_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
//                                    .program(BGRA_POSITION_TEXTURE_COLOR_PROGRAM)
                                    .build(false)
                    ));

    /**
     * Render Layer for BGRA textures that also need blur effect.
     */
    private static final Function<Identifier, RenderLayer> BGRA_BLURRED_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "bgra_blurred_texture_layer",
                            786432,
                            ClientRenderPipelines.BGRA_BLURRED_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
//                                    .program(BGRA_POSITION_TEXTURE_COLOR_PROGRAM)
                                    .target(BlurEffectRenderer.getOutlineTarget())
                                    .build(false)
                    ));

    public static RenderLayer getSmoothTextureLayer(Identifier textureId) {
        return SMOOTH_TEXTURE_LAYER.apply(textureId);
    }

    public static RenderLayer getBlurredTextureLayer(Identifier textureId) {
        return BLURRED_TEXTURE_LAYER.apply(textureId);
    }

    public static RenderLayer getBgraTextureLayer(Identifier textureId) {
        return BGRA_TEXTURE_LAYER.apply(textureId);
    }

    public static RenderLayer getBgraBlurredTextureLayer(Identifier textureId) {
        return BGRA_BLURRED_TEXTURE_LAYER.apply(textureId);
    }

}
