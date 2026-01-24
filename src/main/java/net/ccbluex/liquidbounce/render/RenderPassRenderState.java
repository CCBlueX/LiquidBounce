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
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import kotlin.Pair;
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

    public @Nullable GpuBufferSlice vertexSlice;
    public @Nullable GpuBufferSlice indexSlice;
    public int indexCount;
    public @Nullable VertexFormat vertexFormat;
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

    public void uploadAndSet(
        MeshData meshData,
        RenderPipeline pipeline,
        boolean rotate
    ) {
        // Vertices
        if (rotate) this.vboStorage.rotate();
        ByteBuffer vertices = meshData.vertexBuffer();
        vertexSlice = this.vboStorage.upload(vertices);
        vertexFormat = pipeline.getVertexFormat();

        // Indices
        if (rotate) this.iboStorage.rotate();
        indexCount = meshData.drawState().indexCount();
        var pair = uploadIndicesOrUseSharedSequential(
            meshData,
            this.iboStorage,
            pipeline.getVertexFormatMode()
        );
        indexSlice = pair.getFirst();
        indexType = pair.getSecond();
    }

    public void bindAndDraw(RenderPass pass) {
        if (!ready) {
            return;
        }

        assert vertexSlice != null;
        assert vertexFormat != null;
        assert indexSlice != null;
        assert indexType != null;
        // Alert: this call requires same vertex format (size) and same index type, unless offset is 0
        RenderPassExtensionsKt.bindAndDraw(
            pass,
            vertexSlice,
            indexSlice,
            vertexFormat,
            indexType,
            indexCount
        );
    }

    /**
     * Clear the render state. This won't close the buffers.
     */
    public void clearStates() {
        vertexSlice = null;
        vertexFormat = null;
        indexSlice = null;
        indexCount = 0;
        indexType = null;
        ready = false;
    }

    public void clearBuffers() {
        vboStorage.clear();
        iboStorage.clear();
    }

    private static Pair<GpuBufferSlice, VertexFormat.IndexType> uploadIndicesOrUseSharedSequential(
        MeshData meshData,
        GrowableMappableRingBuffer bufferStorage,
        VertexFormat.Mode vertexFormatMode
    ) {
        ByteBuffer indices = meshData.indexBuffer();
        GpuBufferSlice indexSlice;
        VertexFormat.IndexType indexType;
        if (indices == null) {
            int indexCount = meshData.drawState().indexCount();
            var shapeIndexBuffer = RenderSystem.getSequentialBuffer(vertexFormatMode);
            indexType = shapeIndexBuffer.type();
            indexSlice = shapeIndexBuffer.getBuffer(indexCount)
                .slice(0, (long) indexCount * indexType.bytes);
        } else {
            indexType = meshData.drawState().indexType();
            indexSlice = bufferStorage.upload(indices);
        }
        return new Pair<>(indexSlice, indexType);
    }
}
