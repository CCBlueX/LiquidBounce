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
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.floats.Float2IntFunction;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * @param colorGetter Angle to ARGB
 */
public record CircleGuiElementRenderState(
    float x,
    float y,
    float radius,
    float innerRadius,
    int segments,
    Float2IntFunction colorGetter,
    RenderPipeline pipeline,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements LiquidBounceGuiElementRenderState {

    public CircleGuiElementRenderState {
        assert pipeline.getVertexFormatMode() == VertexFormat.Mode.TRIANGLES;
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        float step = Mth.TWO_PI / segments();
        Matrix3x2fc pose = pose();

        // Initial state (i=0)
        float cAngle = 0.0f;
        int cColor = colorGetter().get(cAngle);
        float sinC = Mth.sin(cAngle);
        float cosC = Mth.cos(cAngle);

        float innerCurrX = x() + sinC * innerRadius();
        float innerCurrY = y() + cosC * innerRadius();
        float outerCurrX = x() + sinC * radius();
        float outerCurrY = y() + cosC * radius();

        for (int i = 1; i <= segments(); ++i) {
            // Calculate next state
            float nAngle = step * i;
            int nColor = colorGetter().get(nAngle);
            float sinN = Mth.sin(nAngle);
            float cosN = Mth.cos(nAngle);

            float innerNextX = x() + sinN * innerRadius();
            float innerNextY = y() + cosN * innerRadius();
            float outerNextX = x() + sinN * radius();
            float outerNextY = y() + cosN * radius();

            // Draw triangles for the segment
            vertices.addVertexWith2DPose(pose, innerCurrX, innerCurrY).setColor(cColor);
            vertices.addVertexWith2DPose(pose, outerCurrX, outerCurrY).setColor(cColor);
            vertices.addVertexWith2DPose(pose, outerNextX, outerNextY).setColor(cColor);

            vertices.addVertexWith2DPose(pose, innerCurrX, innerCurrY).setColor(nColor);
            vertices.addVertexWith2DPose(pose, outerNextX, outerNextY).setColor(nColor);
            vertices.addVertexWith2DPose(pose, innerNextX, innerNextY).setColor(nColor);

            // Update current state for next iteration
            cAngle = nAngle;
            cColor = nColor;
            innerCurrX = innerNextX;
            innerCurrY = innerNextY;
            outerCurrX = outerNextX;
            outerCurrY = outerNextY;
        }
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }
}
