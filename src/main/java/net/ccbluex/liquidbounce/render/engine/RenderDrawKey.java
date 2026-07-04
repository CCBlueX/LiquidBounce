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

package net.ccbluex.liquidbounce.render.engine;

import static java.util.Collections.emptyMap;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Comparator;
import java.util.Map;

public record RenderDrawKey(
    RenderPipeline pipeline,
    Map<String, AbstractTexture> textures,
    Map<String, GpuBufferSlice> uniforms
) implements Comparable<RenderDrawKey> {
    private static final Reference2ObjectOpenHashMap<RenderPipeline, RenderDrawKey> CACHE_ONLY_PIPELINE = new Reference2ObjectOpenHashMap<>();

    private static final Comparator<RenderDrawKey> COMPARATOR =
        Comparator.<RenderDrawKey>comparingInt(k -> k.pipeline.getSortKey());

    public static RenderDrawKey of(RenderPipeline pipeline) {
        return CACHE_ONLY_PIPELINE.computeIfAbsent(pipeline,
            p -> new RenderDrawKey((RenderPipeline) p, emptyMap(), emptyMap()));
    }

    public static RenderDrawKey of(
        RenderPipeline pipeline,
        Map<String, AbstractTexture> textures,
        Map<String, GpuBufferSlice> uniforms
    ) {
        if (textures.isEmpty() && uniforms.isEmpty()) {
            return of(pipeline);
        }

        return new RenderDrawKey(pipeline, Map.copyOf(textures), Map.copyOf(uniforms));
    }

    @Override
    public int compareTo(RenderDrawKey o) {
        return COMPARATOR.compare(this, o);
    }
}
