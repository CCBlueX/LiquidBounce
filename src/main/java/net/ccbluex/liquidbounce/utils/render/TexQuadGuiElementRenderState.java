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
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * Float version of {@link net.minecraft.client.gui.render.state.BlitRenderState}
 */
public record TexQuadGuiElementRenderState(
    float x0,
    float y0,
    float x1,
    float y1,
    float u1,
    float v1,
    float u2,
    float v2,
    int argb,
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements LiquidBounceGuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertices) {
        vertices.addVertexWith2DPose(pose, x0, y0).setUv(u1, v1).setColor(argb);
        vertices.addVertexWith2DPose(pose, x0, y1).setUv(u1, v2).setColor(argb);
        vertices.addVertexWith2DPose(pose, x1, y1).setUv(u2, v2).setColor(argb);
        vertices.addVertexWith2DPose(pose, x1, y0).setUv(u2, v1).setColor(argb);
    }

}
