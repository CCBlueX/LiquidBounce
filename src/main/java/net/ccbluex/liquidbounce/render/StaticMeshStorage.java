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
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.ccbluex.liquidbounce.render.mesh.MeshDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * Storage for a reusable (static) mesh draw call.
 *
 * <p>Unlike the dynamic path that uploads per frame into shared buffers,
 * this class keeps dedicated VBO/IBO storage and a cached {@link MeshDraw}
 * so the same geometry can be rendered across multiple frames.</p>
 */
public final class StaticMeshStorage {

    public final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(0xC0000);
    private final GrowableMappableRingBuffer vboStorage;
    private final GrowableMappableRingBuffer iboStorage;
    private final GpuBufferSlice baseBlockPosUniform;

    private @Nullable MeshDraw meshDraw;
    private final BlockPos.MutableBlockPos baseBlockPos = new BlockPos.MutableBlockPos();
    private boolean hasBaseBlockPosUniformValue;

    public final String label;

    public StaticMeshStorage(String label) {
        this.vboStorage = new GrowableMappableRingBuffer(
            label + " VBO",
            GpuBuffer.USAGE_VERTEX
        );
        this.iboStorage = new GrowableMappableRingBuffer(
            label + " IBO",
            GpuBuffer.USAGE_INDEX
        );
        this.baseBlockPosUniform = ClientUniformDefine.MESH_BASE_BLOCK_POS.createSingleBuffer(() -> label + " BaseBlockPos UBO");
        this.label = label;
    }

    public boolean isReady() {
        return this.meshDraw != null;
    }

    public BlockPos getBaseBlockPos() {
        return this.baseBlockPos;
    }

    public GpuBufferSlice getBaseBlockPosUniform() {
        return this.baseBlockPosUniform;
    }

    public void setBaseBlockPosUniform(RenderPass renderPass) {
        ClientUniformDefine.MESH_BASE_BLOCK_POS.setTo(renderPass, this.baseBlockPosUniform);
    }

    public void setBaseBlockPos(Vec3i baseBlockPos) {
        if (this.hasBaseBlockPosUniformValue && baseBlockPos.equals(this.baseBlockPos)) {
            return;
        }

        this.writeBaseBlockPosUniform(this.baseBlockPos.set(baseBlockPos));
        this.hasBaseBlockPosUniformValue = true;
    }

    private void writeBaseBlockPosUniform(BlockPos baseBlockPos) {
        try (var view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.baseBlockPosUniform, false, true)) {
            Std140Builder.intoBuffer(view.data())
                .putIVec3(baseBlockPos.getX(), baseBlockPos.getY(), baseBlockPos.getZ());
        }
    }

    /**
     * Upload mesh data into this storage's dedicated buffers and refresh the draw state.
     *
     * @param meshData built mesh data to upload
     * @param pipeline pipeline used to determine vertex/index layout
     * @param rotate whether to rotate ring buffers before upload (recommended when rebuilding after a frame boundary)
     */
    public void uploadAndSet(
        MeshData meshData,
        RenderPipeline pipeline,
        boolean rotate
    ) {
        if (rotate) {
            this.vboStorage.rotate();
            this.iboStorage.rotate();
        }

        this.meshDraw = MeshDraw.create(meshData, pipeline, v -> this.vboStorage, i -> this.iboStorage);
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
        this.baseBlockPos.set(0, 0, 0);
        this.hasBaseBlockPosUniformValue = false;
        this.meshDraw = null;
    }

    public void clearBuffers() {
        this.vboStorage.clear();
        this.iboStorage.clear();
    }

}
