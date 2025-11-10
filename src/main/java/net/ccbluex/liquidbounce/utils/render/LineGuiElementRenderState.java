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

package net.ccbluex.liquidbounce.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.ccbluex.liquidbounce.render.ClientRenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record LineGuiElementRenderState(
    float[] points,
    int argb,
    Matrix3x2f pose,
    @Nullable ScreenRect scissorArea,
    @Nullable ScreenRect bounds
) implements LiquidBounceGuiElementRenderState {

    public LineGuiElementRenderState {
        if ((points.length & 1) != 0) {
            throw new IllegalArgumentException("Incomplete points array. It must have an even number of elements.");
        }
    }

    public LineGuiElementRenderState(
        Vec2f[] points,
        int argb,
        Matrix3x2f pose,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
    ) {
        this(
            flat(points),
            argb,
            pose,
            scissorArea,
            bounds
        );
    }

    @Override
    public void setupVertices(VertexConsumer vertices, float depth) {
        for (int i = 0; i < points.length; i += 2) {
            float x = points[i];
            float y = points[i + 1];
            vertices.vertex(pose, x, y, depth).color(argb);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return ClientRenderPipelines.GUI.Lines;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    private static float[] flat(Vec2f[] points) {
        float[] flatPoints = new float[points.length << 1];
        for (int i = 0; i < points.length; i++) {
            Vec2f point = points[i];
            flatPoints[i << 1] = point.x;
            flatPoints[(i << 1) | 1] = point.y;
        }
        return flatPoints;
    }
}
