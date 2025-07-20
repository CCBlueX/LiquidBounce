package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Addition to {@link net.minecraft.client.gui.DrawContext}.
 */
public interface DrawContextAddition {

    /**
     * drawTexture with floats
     */
    void liquid_bounce$drawTexture(Function<Identifier, RenderLayer> renderLayers, Identifier texture, float x, float y, int width, int height);

    /**
     * drawTexturedQuad with floats
     */
    void liquid_bounce$drawTexturedQuad(Function<Identifier, RenderLayer> renderLayers, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color);
}
