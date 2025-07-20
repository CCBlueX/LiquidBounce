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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Modifies {@link MinecraftClient#getFramebuffer()} to return an own framebuffer so that minecraft writes results
 * to that framebuffer and not the main framebuffer.
 *
 * @author ccetl
 */
public class GlobalFramebuffer {

    public final static List<Framebuffer> stack = new ArrayList<>(2);

    private static Framebuffer spoofedFramebuffer;

    public static Framebuffer getSpoofedFramebuffer() {
        return spoofedFramebuffer;
    }

    public static void push(Framebuffer spoofedFramebuffer) {
        if (GlobalFramebuffer.spoofedFramebuffer != null) {
            stack.addLast(GlobalFramebuffer.spoofedFramebuffer);
        }

        GlobalFramebuffer.spoofedFramebuffer = spoofedFramebuffer;
    }

    public static void pop() {
        if (stack.isEmpty()) {
            spoofedFramebuffer = null;
            return;
        }

        spoofedFramebuffer = stack.getLast();
    }

    public static void clear() {
        stack.clear();
        spoofedFramebuffer = null;
    }

}
