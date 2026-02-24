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
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.ccbluex.liquidbounce.render.mesh.MeshDraw;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class RenderPassRenderState {

    static final Vector4f colorModulator = new Vector4f(1F, 1F, 1F, 1F);
    static final Vector3f modelOffset = new Vector3f();
    static final Matrix4f textureMatrix = new Matrix4f();

    public final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(0xC0000);
    private final GrowableMappableRingBuffer vboStorage;
    private final GrowableMappableRingBuffer iboStorage;

    public @Nullable MeshDraw meshDraw;

    public boolean ready = false;

    public final String label;

    public RenderPassRenderState(String label) {
        this.vboStorage = new GrowableMappableRingBuffer(
            label + " VBO",
            GpuBuffer.USAGE_VERTEX
        );
        this.iboStorage = new GrowableMappableRingBuffer(
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
        if (rotate) {
            this.vboStorage.rotate();
            this.iboStorage.rotate();
        }

        this.meshDraw = MeshDraw.create(meshData, pipeline, v -> this.vboStorage, i -> this.iboStorage);
    }

    public void bindAndDraw(RenderPass pass) {
        if (!ready) {
            return;
        }

        assert this.meshDraw != null;
        MeshDraw.bindAndDraw(pass, this.meshDraw);
    }

    /**
     * Clear the render state. This won't close the buffers.
     */
    public void clearStates() {
        this.meshDraw = null;
        this.ready = false;
    }

    public void clearBuffers() {
        this.vboStorage.clear();
        this.iboStorage.clear();
    }

}
