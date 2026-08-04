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
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defers closing or recycling of GPU buffers until the GPU has finished using them.
 *
 * <p>This class uses fences to ensure that a buffer is not closed/recycled while it is still
 * referenced by in-flight GPU work. Call {@link #add(GpuBuffer)} after the last use of a buffer
 * in the current frame, then call {@link #tryClose()} in a later frame after command submission.
 *
 * <p>Both methods must be called on the render thread.
 */
public final class GpuBufferDeferredCloser implements AutoCloseable {

    private final Consumer<GpuBuffer> closeAction;
    private final ArrayList<PendingClose> pendingClose = new ArrayList<>();

    /**
     * Creates a deferred closer with default action {@link GpuBuffer#close()}.
     */
    public GpuBufferDeferredCloser() {
        this(GpuBuffer::close);
    }

    /**
     * Creates a deferred closer.
     *
     * @param closeAction action to run once a buffer's fence has completed;
     *                    typically closes or recycles the buffer
     */
    public GpuBufferDeferredCloser(Consumer<GpuBuffer> closeAction) {
        this.closeAction = closeAction;
    }

    /**
     * Defers closing/recycling of the given buffer until all GPU work submitted before this call
     * has completed.
     *
     * @param buffer the buffer that must remain valid until its fence is signaled
     */
    public void add(GpuBuffer buffer) {
        var fence = RenderSystem.getDevice().createCommandEncoder().createFence();
        this.pendingClose.add(new PendingClose(List.of(buffer), fence));
    }

    /**
     * Defers closing/recycling all given buffers behind a single fence.
     *
     * <p>The fence must be created after every command that references any buffer in {@code buffers}
     * has been recorded and before the command encoder is submitted.</p>
     *
     * @param buffers buffers that must remain valid until the fence is signaled
     */
    public void add(Collection<GpuBuffer> buffers) {
        if (buffers.isEmpty()) {
            return;
        }

        var fence = RenderSystem.getDevice().createCommandEncoder().createFence();
        this.pendingClose.add(new PendingClose(List.copyOf(buffers), fence));
    }

    /**
     * Non-blockingly checks all pending buffers and runs the configured action on those whose
     * fences have completed.
     *
     * <p>This should be called after submitting a command buffer (typically in a later frame).
     * Buffers whose work is still in flight will remain pending.
     */
    public void tryClose() {
        this.pendingClose.removeIf(pending -> {
            if (pending.fence.awaitCompletion(0L)) {
                pending.fence.close();
                pending.buffers.forEach(this.closeAction);
                return true;
            }

            return false;
        });
    }

    @Override
    public void close() {
        this.pendingClose.forEach(PendingClose::close);
        this.pendingClose.clear();
    }

    private record PendingClose(List<GpuBuffer> buffers, GpuFence fence) implements AutoCloseable {
        @Override
        public void close() {
            this.buffers.forEach(GpuBuffer::close);
            this.fence.close();
        }
    }

}
