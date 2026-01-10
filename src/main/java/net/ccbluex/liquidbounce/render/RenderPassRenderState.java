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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public class RenderPassRenderState {

    static final Vector4f colorModulator = new Vector4f(1F, 1F, 1F, 1F);
    static final Vector3f modelOffset = new Vector3f();
    static final Matrix4f textureMatrix = new Matrix4f();

    private final GrowableMappableRingBuffer vboStorage;
    private final GrowableMappableRingBuffer iboStorage;

    public @Nullable GpuBuffer vertexBuffer;
    public @Nullable GpuBuffer indexBuffer;
    public int indexCount;
    public VertexFormat.@Nullable IndexType indexType;

    public boolean ready = false;

    public final String label;

    public RenderPassRenderState(String label) {
        vboStorage = new GrowableMappableRingBuffer(
            label + " VBO",
            GpuBuffer.USAGE_VERTEX
        );
        iboStorage = new GrowableMappableRingBuffer(
            label + " IBO",
            GpuBuffer.USAGE_INDEX
        );
        this.label = label;
    }

    public void uploadAndSetVertices(
        MeshData meshData
    ) {
        this.vboStorage.rotate();
        ByteBuffer vertices = meshData.vertexBuffer();
        vertexBuffer = this.vboStorage.upload(vertices).buffer();
    }

    public void uploadAndSetIndices(
        MeshData meshData,
        VertexFormat.Mode vertexFormatMode
    ) {
        this.iboStorage.rotate();
        ByteBuffer indices = meshData.indexBuffer();
        indexCount = meshData.drawState().indexCount();
        if (indices == null) {
            var shapeIndexBuffer = RenderSystem.getSequentialBuffer(vertexFormatMode);
            indexBuffer = shapeIndexBuffer.getBuffer(indexCount);
            indexType = shapeIndexBuffer.type();
        } else {
            indexBuffer = this.iboStorage.upload(indices).buffer();
            indexType = meshData.drawState().indexType();
        }
    }

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

    /**
     * Clear the render state. This won't close the buffers.
     */
    public void clearStates() {
        vertexBuffer = null;
        indexBuffer = null;
        indexCount = 0;
        indexType = null;
        ready = false;
    }

    public void clearBuffers() {
        vboStorage.clear();
        iboStorage.clear();
    }
}
