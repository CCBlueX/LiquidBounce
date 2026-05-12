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

package net.ccbluex.liquidbounce.render.gui.element;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ccbluex.liquidbounce.render.ClientRenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * Rounded rectangle GUI element whose per-element parameters are passed through vertex attributes.
 *
 * <p>GUI render states are batched by pipeline and texture setup, so per-element uniforms cannot be used here.</p>
 */
public record RoundedRectGuiElementRenderState(
    float x0,
    float y0,
    float x1,
    float y1,
    float radius,
    int fillColor,
    int outlineColor,
    float outlineWidth,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PoseReusableGuiElementRenderState {

    private static final int MAX_ENCODED_VALUE = 32767;

    @Override
    public void buildVertices(VertexConsumer vertices) {
        float width = x1 - x0;
        float height = y1 - y0;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        int encodedWidth = encode(width);
        int encodedHeight = encode(height);
        int encodedRadius = encode(Math.min(radius, Math.min(width, height) * 0.5F));

        if (isVisible(fillColor)) {
            emitQuad(vertices, fillColor, encodedWidth, encodedHeight, encodedRadius, 0.0F);
        }

        if (isVisible(outlineColor) && outlineWidth > 0.0F) {
            emitQuad(vertices, outlineColor, encodedWidth, encodedHeight, encodedRadius, outlineWidth);
        }
    }

    private void emitQuad(
        VertexConsumer vertices,
        int color,
        int encodedWidth,
        int encodedHeight,
        int encodedRadius,
        float strokeWidth
    ) {
        vertices.addVertexWith2DPose(pose, x0, y0)
            .setUv(0.0F, 0.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(encodedRadius, 0)
            .setLineWidth(strokeWidth);
        vertices.addVertexWith2DPose(pose, x0, y1)
            .setUv(0.0F, 1.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(encodedRadius, 0)
            .setLineWidth(strokeWidth);
        vertices.addVertexWith2DPose(pose, x1, y1)
            .setUv(1.0F, 1.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(encodedRadius, 0)
            .setLineWidth(strokeWidth);
        vertices.addVertexWith2DPose(pose, x1, y0)
            .setUv(1.0F, 0.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(encodedRadius, 0)
            .setLineWidth(strokeWidth);
    }

    @Override
    public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
        return ClientRenderPipelines.GUI.roundedRect();
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    private static int encode(float value) {
        return Math.round(Math.clamp(value, 0.0F, MAX_ENCODED_VALUE));
    }

    private static boolean isVisible(int argb) {
        return (argb >>> 24) != 0;
    }
}
