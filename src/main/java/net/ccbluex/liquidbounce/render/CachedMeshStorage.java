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

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ccbluex.liquidbounce.render.mesh.MeshDraw;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Storage for a reusable (static) mesh draw call.
 *
 * <p>Unlike the dynamic path that uploads per frame into shared buffers,
 * this class keeps uploaded vertex data, optional dedicated index storage,
 * and a cached {@link MeshDraw} so the same geometry can be rendered across
 * multiple frames.</p>
 */
public final class CachedMeshStorage implements AutoCloseable {

    /**
     * Align block positions to 4096 to reduce writing to buffer.
     */
    private static final int BASE_BLOCK_POS_MASK = ~0xFFF;

    public final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(0xC0000);
    private final GpuBufferSlice baseBlockPosUniform;

    private @Nullable MeshDraw meshDraw;
    private @Nullable GpuBuffer vertexBuffer;
    private @Nullable GpuBuffer indexBuffer;
    private long baseBlockPosAsLong;
    private final BlockPos.MutableBlockPos baseBlockPos = new BlockPos.MutableBlockPos();
    private boolean hasBaseBlockPosUniformValue;

    public final String label;

    public CachedMeshStorage(String label) {
        this.baseBlockPosUniform = ClientUniformDefine.MESH_BASE_BLOCK_POS.createSingleBuffer(() -> label + " BaseBlockPos UBO");
        this.label = label;
    }

    public boolean isReady() {
        return this.meshDraw != null;
    }

    public void bindUniform(RenderPass renderPass) {
        ClientUniformDefine.MESH_BASE_BLOCK_POS.setTo(renderPass, this.baseBlockPosUniform);
    }

    /**
     * Resolve the mesh origin used for this build.
     *
     * <p>If the requested origin is close enough to the current origin, this reuses the current origin
     * to avoid rewriting the base block position uniform. The returned origin must be used when building
     * vertex positions for this mesh.</p>
     */
    public BlockPos resolveBaseBlockPos(BlockPos base) {
        long normalized = normalize(base);
        if (this.hasBaseBlockPosUniformValue && this.baseBlockPosAsLong == normalized) {
            return this.baseBlockPos;
        }

        this.baseBlockPosAsLong = normalized;
        this.baseBlockPos.set(normalized);
        this.writeBaseBlockPosUniform(this.baseBlockPos);
        this.hasBaseBlockPosUniformValue = true;
        return this.baseBlockPos;
    }

    private static long normalize(BlockPos pos) {
        return BlockPos.asLong(
            pos.getX() & BASE_BLOCK_POS_MASK,
            pos.getY() & BASE_BLOCK_POS_MASK,
            pos.getZ() & BASE_BLOCK_POS_MASK
        );
    }

    private void writeBaseBlockPosUniform(BlockPos baseBlockPos) {
        try (var view = this.baseBlockPosUniform.map(false, true)) {
            Std140Builder.intoBuffer(view.data())
                .putIVec3(baseBlockPos.getX(), baseBlockPos.getY(), baseBlockPos.getZ());
        }
    }

    /**
     * Upload mesh data into this storage's dedicated buffers and refresh the draw state.
     *
     * @param meshData built mesh data to upload
     * @param pipeline pipeline used to determine vertex/index layout
     */
    public void uploadAndSet(
        MeshData meshData,
        RenderPipeline pipeline
    ) {
        var uploadContext = new UploadContext();

        try {
            var newMeshDraw = MeshDraw.create(meshData, pipeline, uploadContext, uploadContext);

            var oldVertexBuffer = this.vertexBuffer;
            var oldIndexBuffer = this.indexBuffer;

            this.meshDraw = newMeshDraw;
            this.vertexBuffer = uploadContext.vertexBuffer;
            this.indexBuffer = uploadContext.indexBuffer;

            StaticGpuBufferPool.release(oldVertexBuffer);
            StaticGpuBufferPool.release(oldIndexBuffer);
        } catch (Throwable throwable) {
            uploadContext.release();
            throw throwable;
        }
    }

    /**
     * Draw the currently uploaded mesh if available.
     */
    public void bindAndDraw(RenderPass pass) {
        if (!this.isReady()) return;
        MeshDraw.bindAndDraw(pass, this.meshDraw);
    }

    /**
     * Clear the render state. This won't close the buffers.
     */
    public void clearStates() {
        this.meshDraw = null;
    }

    public void clearBuffers() {
        this.clearStates();
        StaticGpuBufferPool.release(this.vertexBuffer);
        StaticGpuBufferPool.release(this.indexBuffer);
        this.vertexBuffer = null;
        this.indexBuffer = null;
    }

    @Override
    public void close() {
        this.clearBuffers();
        this.baseBlockPosUniform.buffer().close();
        this.byteBufferBuilder.close();
    }

    private final class UploadContext implements MeshDraw.IndexUploader, MeshDraw.VertexUploader {
        private @Nullable GpuBuffer vertexBuffer;
        private @Nullable GpuBuffer indexBuffer;

        private void release() {
            StaticGpuBufferPool.release(this.vertexBuffer);
            StaticGpuBufferPool.release(this.indexBuffer);
            this.vertexBuffer = null;
            this.indexBuffer = null;
        }

        @Override
        public GpuBufferSlice upload(IndexType type, ByteBuffer data) {
            var upload = StaticGpuBufferPool.upload(
                () -> CachedMeshStorage.this.label + " IBO",
                GpuBuffer.USAGE_INDEX,
                data
            );
            this.indexBuffer = upload.buffer();
            return upload;
        }

        @Override
        public GpuBufferSlice upload(VertexFormat format, ByteBuffer data) {
            var upload = StaticGpuBufferPool.upload(
                () -> CachedMeshStorage.this.label + " VBO",
                GpuBuffer.USAGE_VERTEX,
                data
            );
            this.vertexBuffer = upload.buffer();
            return upload;
        }
    }

}
