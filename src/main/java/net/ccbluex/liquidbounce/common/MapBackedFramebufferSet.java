/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;

import java.util.HashMap;

import javax.annotation.Nullable;

/**
 * Stupid class, but Minecraft needed one more abstraction...
 */
public record MapBackedFramebufferSet(
        HashMap<Identifier, Handle<Framebuffer>> backingMap
) implements PostEffectProcessor.FramebufferSet {

    @Override
    public void set(Identifier id, Handle<Framebuffer> framebuffer) {
        this.backingMap.put(id, framebuffer);
    }

    @Override
    public @Nullable Handle<Framebuffer> get(Identifier id) {
        return this.backingMap.get(id);
    }
}
