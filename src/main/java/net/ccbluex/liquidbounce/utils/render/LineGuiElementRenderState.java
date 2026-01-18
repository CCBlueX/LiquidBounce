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

package net.ccbluex.liquidbounce.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record LineGuiElementRenderState(
    float[] points,
    int argb,
    RenderPipeline pipeline,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements LiquidBounceGuiElementRenderState {

    public LineGuiElementRenderState {
        if ((points.length & 1) != 0) {
            throw new IllegalArgumentException("Incomplete points array. It must have an even number of elements.");
        }
    }

    public LineGuiElementRenderState(
        Vec2[] points,
        int argb,
        Matrix3x2f pose,
        RenderPipeline pipeline,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) {
        this(
            flat(points),
            argb,
            pipeline,
            pose,
            scissorArea,
            bounds
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (int i = 0; i < points.length; i += 2) {
            float x = points[i];
            float y = points[i + 1];
            vertices.addVertexWith2DPose(pose, x, y).setColor(argb);
        }
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    private static float[] flat(Vec2[] points) {
        float[] flatPoints = new float[points.length << 1];
        for (int i = 0; i < points.length; i++) {
            Vec2 point = points[i];
            flatPoints[i << 1] = point.x;
            flatPoints[(i << 1) | 1] = point.y;
        }
        return flatPoints;
    }
}
