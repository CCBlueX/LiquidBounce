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
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.ccbluex.liquidbounce.render.ClientRenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Anti-aliased indicator arrow, drawn as the quad the shader carves the shape out of. The local origin is the middle
 * of its base, the tip is at {@code (0, height)}, and the tail is notched along the local Y axis.
 */
public record ArrowGuiElementRenderState(
    float halfWidth,
    float height,
    float notch,
    int color,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PoseReusableGuiElementRenderState {

    /** UV1 is packed into shorts, so the arrow's size travels as fixed point. */
    private static final float FIXED_POINT = 32.0F;
    private static final int MAX_ENCODED_VALUE = 32767;

    @Override
    public void buildVertices(VertexConsumer vertices) {
        int encodedWidth = encode(halfWidth * 2.0F);
        int encodedHeight = encode(height);

        vertices.addVertexWith2DPose(pose, -halfWidth, 0.0F)
            .setUv(-halfWidth, 0.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(0, 0)
            .setLineWidth(notch);
        vertices.addVertexWith2DPose(pose, -halfWidth, height)
            .setUv(-halfWidth, height)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(0, 0)
            .setLineWidth(notch);
        vertices.addVertexWith2DPose(pose, halfWidth, height)
            .setUv(halfWidth, height)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(0, 0)
            .setLineWidth(notch);
        vertices.addVertexWith2DPose(pose, halfWidth, 0.0F)
            .setUv(halfWidth, 0.0F)
            .setColor(color)
            .setUv1(encodedWidth, encodedHeight)
            .setUv2(0, 0)
            .setLineWidth(notch);
    }

    @Override
    public RenderPipeline pipeline() {
        return ClientRenderPipelines.GUI.Arrow;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    private static int encode(float value) {
        return Math.round(Math.clamp(value * FIXED_POINT, 0.0F, MAX_ENCODED_VALUE));
    }
}
