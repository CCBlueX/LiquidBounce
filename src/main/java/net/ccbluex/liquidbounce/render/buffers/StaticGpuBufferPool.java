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

package net.ccbluex.liquidbounce.render.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Small fence-aware pool for reusable static mesh buffers.
 *
 * <p>This follows the same lifetime model as vanilla's StagedVertexBuffer pool:
 * buffers that were used by draw commands are not made available again until a GPU
 * fence confirms that previous work has completed.</p>
 *
 * @see net.minecraft.client.renderer.StagedVertexBuffer
 * @see net.minecraft.client.renderer.WorldBorderRenderer
 * @see net.minecraft.client.renderer.WeatherEffectRenderer
 */
public final class StaticGpuBufferPool {

    private static final int BUFFER_SIZE_INCREMENT = 16 * 1024;
    private static final long MAX_REUSE_SIZE_FACTOR = 4L;
    private static final int MAX_AVAILABLE_BUFFERS_PER_USAGE = 16;

    private static final Pool VERTEX_POOL = new Pool(GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST);
    private static final Pool INDEX_POOL = new Pool(GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST);

    private StaticGpuBufferPool() {
    }

    public static GpuBufferSlice upload(Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
        int byteCount = data.remaining();
        if (byteCount <= 0) {
            throw new IllegalArgumentException("Static mesh buffer upload must not be empty");
        }

        var pool = poolFor(usage);
        var buffer = pool.acquire(label, byteCount);
        var slice = buffer.slice(0L, byteCount);
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(slice, data);
        return slice;
    }

    public static void release(@Nullable GpuBuffer buffer) {
        if (buffer == null || buffer.isClosed()) {
            return;
        }

        poolFor(buffer.usage()).release(buffer);
    }

    public static void cleanup() {
        VERTEX_POOL.cleanup();
        INDEX_POOL.cleanup();
    }

    private static Pool poolFor(@GpuBuffer.Usage int usage) {
        if ((usage & GpuBuffer.USAGE_VERTEX) != 0) {
            return VERTEX_POOL;
        } else if ((usage & GpuBuffer.USAGE_INDEX) != 0) {
            return INDEX_POOL;
        }

        throw new IllegalArgumentException("Unsupported static mesh buffer usage: " + usage);
    }

    private static final class Pool {
        private final @GpuBuffer.Usage int usage;
        private final GpuBufferAvailableCache available = new GpuBufferAvailableCache();
        private final GpuBufferDeferredCloser closer = new GpuBufferDeferredCloser(this.available::add);

        private Pool(@GpuBuffer.Usage int usage) {
            this.usage = usage;
        }

        private GpuBuffer acquire(Supplier<String> label, int minSize) {
            this.cleanup();

            int roundedMinSize = Mth.roundToward(minSize, BUFFER_SIZE_INCREMENT);
            GpuBuffer buffer = this.available.takeBest(roundedMinSize, roundedMinSize * MAX_REUSE_SIZE_FACTOR);
            if (buffer == null) {
                buffer = RenderSystem.getDevice().createBuffer(label, this.usage, roundedMinSize);
            }

            return buffer;
        }

        private void release(GpuBuffer buffer) {
            if (!buffer.isClosed()) {
                this.closer.add(buffer);
            }
        }

        private void cleanup() {
            this.closer.tryClose();
            this.trimAvailable();
        }

        private void trimAvailable() {
            this.available.trimTo(MAX_AVAILABLE_BUFFERS_PER_USAGE);
        }
    }
}
