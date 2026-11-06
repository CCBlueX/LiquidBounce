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

package net.ccbluex.liquidbounce.common;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

@NullMarked
public final class LightmapOverride {

    public static final ScopedValue<GpuTextureView> VALUE = ScopedValue.newInstance();

    private LightmapOverride() {
    }

    public static <R> R call(GpuTextureView textureView, Supplier<? extends R> op) {
        // If we use ScopedValue.CallableOp for op Kotlin compiler will be mad
        // 2.4.20
        return ScopedValue.where(VALUE, textureView).call(op::get);
    }

}
