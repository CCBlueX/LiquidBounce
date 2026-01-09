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

package net.ccbluex.liquidbounce.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public class RenderPassRenderState {

    static final Vector4f colorModulator = new Vector4f(1F, 1F, 1F, 1F);
    static final Vector3f modelOffset = new Vector3f();
    static final Matrix4f textureMatrix = new Matrix4f();

    public @Nullable GpuBuffer vertexBuffer;
    public @Nullable GpuBuffer indexBuffer;
    public int indexCount;
    public VertexFormat.@Nullable IndexType indexType;

    public boolean ready = false;

    public void bindAndDraw(RenderPass pass) {
        if (!ready) {
            return;
        }

        assert vertexBuffer != null;
        assert indexBuffer != null;
        assert indexType != null;
        pass.setVertexBuffer(0, vertexBuffer);
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.drawIndexed(0, 0, indexCount, 1);
    }

    public void clear() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            indexBuffer.close();
            indexBuffer = null;
        }
        indexCount = 0;
        indexType = null;
        ready = false;
    }

}
