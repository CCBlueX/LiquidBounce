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

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.MinecraftClient;
import net.ccbluex.liquidbounce.render.buffer.Framebuffer;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

/**
 * Modifies {@link MinecraftClient#getFramebuffer()} to return an own framebuffer so that minecraft writes results
 * to that framebuffer and not the main framebuffer.
 *
 * @author ccetl
 */
public final class GlobalFramebuffer {

    private GlobalFramebuffer() {}

    private static final IntList readStack = new IntArrayList(2);
    private static final IntList writeStack = new IntArrayList(2);
    private static final List<Framebuffer> stack = new ArrayList<>(1);

    private static boolean lock;
    private static boolean minecraftChangesRead;
    private static boolean minecraftChangesWrite;

    // framebuffers minecraft sets
    public static void updateRead(int id) {
        if (!minecraftChangesRead || id == readStack.getFirst()) {
            readStack.set(0, id);
            minecraftChangesRead = false;
        }

        if (!lock || minecraftChangesRead) {
            bindRead(id);
        }
    }

    public static void updateWrite(int id) {
        if (!minecraftChangesWrite || id == writeStack.getFirst()) {
            writeStack.set(0, id);
            minecraftChangesWrite = false;
        }

        if (!lock || minecraftChangesWrite) {
            bindWrite(id);
        }
    }

    // when mc changes stuff at the framebuffer, they need to cache the current fbo
    public static int getRead() {
        minecraftChangesRead = true;
        return readStack.getFirst();
    }

    public static int getWrite() {
        minecraftChangesWrite = true;
        return writeStack.getFirst();
    }

    public static void push(Framebuffer spoofedFramebuffer) {
        if (!stack.isEmpty() && stack.getLast() == spoofedFramebuffer) {
            return;
        }

        stack.addLast(spoofedFramebuffer);
        readStack.addLast(spoofedFramebuffer.getId());
        writeStack.addLast(spoofedFramebuffer.getId());
        lock = true;

        if (!minecraftChangesRead) {
            bindRead(spoofedFramebuffer.getId());
        }

        if (!minecraftChangesWrite) {
            bindWrite(spoofedFramebuffer.getId());
        }
    }

    public static void pop() {
        if (!readStack.isEmpty()) {
            readStack.removeLast();
            if (!minecraftChangesRead) {
                bindRead(readStack.getLast());
            }
        }

        if (!writeStack.isEmpty()) {
            writeStack.removeLast();
            if (!minecraftChangesWrite) {
                bindWrite(writeStack.getLast());
            }
        }

        if (!stack.isEmpty()) {
            stack.removeLast();
        }

        if (stack.isEmpty()) {
            lock = false;
        }
    }

    private static void bindRead(int id) {
        GlStateManager.readFbo = id;
        GL30.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, id);
    }

    private static void bindWrite(int id) {
        GlStateManager.writeFbo = id;
        GL30.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, id);
    }

}
