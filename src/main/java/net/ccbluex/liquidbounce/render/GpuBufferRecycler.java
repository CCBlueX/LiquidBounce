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
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class GpuBufferRecycler {

    private final Consumer<GpuBuffer> closeAction;
    private final ArrayList<PendingRecycle> pendingRecycle = new ArrayList<>();

    public GpuBufferRecycler(Consumer<GpuBuffer> closeAction) {
        this.closeAction = closeAction;
    }

    public void add(GpuBuffer buffer) {
        var fence = RenderSystem.getDevice().createCommandEncoder().createFence();
        this.pendingRecycle.add(new PendingRecycle(buffer, fence));
    }

    public void tryClose() {
        this.pendingRecycle.removeIf(pending -> {
            if (pending.fence.awaitCompletion(0L)) {
                pending.fence.close();
                this.closeAction.accept(pending.buffer);
                return true;
            }

            return false;
        });
    }

    private record PendingRecycle(GpuBuffer buffer, GpuFence fence) {
    }

}
