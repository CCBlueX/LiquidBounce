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

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record CircleGuiElementRenderState(
    float x,
    float y,
    float radius,
    float innerRatio,
    int lutRow,
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PoseReusableGuiElementRenderState {

    private static final int INNER_RATIO_SCALE = 32767;

    public CircleGuiElementRenderState {
        assert pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS;
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        int encodedInnerRatio = Math.round(Mth.clamp(innerRatio(), 0.0f, 1.0f) * INNER_RATIO_SCALE);
        float x0 = x() - radius();
        float y0 = y() - radius();
        float x1 = x() + radius();
        float y1 = y() + radius();

        vertices.addVertexWith2DPose(pose, x0, y0).setUv(0.0f, 0.0f).setUv2(lutRow(), encodedInnerRatio);
        vertices.addVertexWith2DPose(pose, x0, y1).setUv(0.0f, 1.0f).setUv2(lutRow(), encodedInnerRatio);
        vertices.addVertexWith2DPose(pose, x1, y1).setUv(1.0f, 1.0f).setUv2(lutRow(), encodedInnerRatio);
        vertices.addVertexWith2DPose(pose, x1, y0).setUv(1.0f, 0.0f).setUv2(lutRow(), encodedInnerRatio);
    }
}
