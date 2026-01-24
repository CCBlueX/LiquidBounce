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

package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@NullMarked
@Mixin(GpuBuffer.class)
public abstract class MixinGpuBuffer {

    @Unique
    private @Nullable GpuBufferSlice lb$slice = null;

    @Redirect(
        method = "slice()Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
        at = @At(value = "NEW", target = "com/mojang/blaze3d/buffers/GpuBufferSlice")
    )
    private GpuBufferSlice wrapNewSlice(GpuBuffer buffer, long offset, long length) {
        if (lb$slice != null) return lb$slice;
        return lb$slice = new GpuBufferSlice(buffer, offset, length);
    }

}
