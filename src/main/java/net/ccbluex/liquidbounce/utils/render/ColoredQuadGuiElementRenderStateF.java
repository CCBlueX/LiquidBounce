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
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.Optional;

/**
 * Float version of [ColoredQuadGuiElementRenderState]
 */
public record ColoredQuadGuiElementRenderStateF(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                                float x0, float y0, float x1, float y1, int col1, int col2,
                                                @Nullable ScreenRect scissorArea,
                                                @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
    public ColoredQuadGuiElementRenderStateF(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x0, float y0, float x1, float y1, int col1, int col2, @Nullable ScreenRect scissorArea) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, col1, col2, scissorArea, Optional.ofNullable(createBounds(x0, y0, x1, y1, pose, scissorArea)).map(ScreenRectF::expandToInt).orElse(null));
    }

    public void setupVertices(VertexConsumer vertices, float depth) {
        vertices.vertex(this.pose(), this.x0(), this.y0(), depth).color(this.col1());
        vertices.vertex(this.pose(), this.x0(), this.y1(), depth).color(this.col2());
        vertices.vertex(this.pose(), this.x1(), this.y1(), depth).color(this.col2());
        vertices.vertex(this.pose(), this.x1(), this.y0(), depth).color(this.col1());
    }

    @Nullable
    private static ScreenRectF createBounds(float x0, float y0, float x1, float y1, Matrix3x2f pose, @Nullable ScreenRect scissorArea) {
        ScreenRectF screenRect = (new ScreenRectF(x0, y0, x1 - x0, y1 - y0)).transformEachVertex(pose);
        return scissorArea != null ? screenRect.intersection(scissorArea) : screenRect;
    }
}
