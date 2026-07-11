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
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Owns GPU buffers that are no longer in flight and may be reused.
 */
final class GpuBufferAvailableCache implements AutoCloseable {

    private static final Comparator<GpuBuffer> LARGEST_FIRST = Comparator.comparingLong(GpuBuffer::size).reversed();

    private final ReferenceArrayList<GpuBuffer> available = new ReferenceArrayList<>();

    public void add(GpuBuffer buffer) {
        this.available.add(buffer);
    }

    public @Nullable GpuBuffer takeBest(long minSize, long maxSize) {
        int bestIndex = -1;
        long bestSize = maxSize == Long.MAX_VALUE ? Long.MAX_VALUE : maxSize + 1L;

        for (int i = 0; i < this.available.size(); i++) {
            long size = this.available.get(i).size();
            if (size == minSize) {
                return this.available.remove(i);
            }

            if (size > minSize && size < bestSize) {
                bestIndex = i;
                bestSize = size;
            }
        }

        return bestIndex == -1 ? null : this.available.remove(bestIndex);
    }

    public void discardSmallerThan(long minSize) {
        this.available.removeIf(buffer -> {
            if (buffer.size() < minSize) {
                buffer.close();
                return true;
            }

            return false;
        });
    }

    public void trimTo(int maxBuffers) {
        if (this.available.size() <= maxBuffers) {
            return;
        }

        this.available.sort(LARGEST_FIRST);
        while (this.available.size() > maxBuffers) {
            this.available.removeLast().close();
        }
    }

    @Override
    public void close() {
        this.available.forEach(GpuBuffer::close);
        this.available.clear();
    }
}
