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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;

/**
 * Extensions to RenderLayer for custom render layers.
 */
public class RenderLayerExtensions {

    /**
     * Render Layer for smoother textures using bilinear filtering.
     */
    public static final Function<Identifier, RenderLayer> SMOOTH_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "smooth_textured",
                            786432,
                            ClientRenderPipelines.JCEF.SMOOTH_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new RenderPhase.Texture(textureId, TriState.DEFAULT, false))
                                    .build(false)
                    ));

    /**
     * Render Layer for elements that need to be rendered to the blur framebuffer.
     */
    public static final Function<Identifier, RenderLayer> BLURRED_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "blurred_ui_layer",
                            786432,
                            ClientRenderPipelines.JCEF.BLURRED_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
                                    .build(false)
                    ));

    /**
     * Render Layer for BGRA textures.
     */
    public static final Function<Identifier, RenderLayer> BGRA_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "bgra_texture_layer",
                            786432,
                            ClientRenderPipelines.JCEF.BGRA_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
                                    .build(false)
                    ));

    /**
     * Render Layer for BGRA textures that also need blur effect.
     */
    public static final Function<Identifier, RenderLayer> BGRA_BLURRED_TEXTURE_LAYER = Util.memoize(
            textureId ->
                    RenderLayer.of(
                            "bgra_blurred_texture_layer",
                            786432,
                            ClientRenderPipelines.JCEF.BGRA_BLURRED_TEXTURE,
                            RenderLayer.MultiPhaseParameters.builder()
                                    .texture(new Texture(textureId, TriState.FALSE, false))
                                    .build(false)
                    ));

}
