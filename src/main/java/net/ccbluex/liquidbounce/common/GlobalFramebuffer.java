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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.ccbluex.liquidbounce.render.buffer.AbstractFramebuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL30;

/**
 * Modifies {@link MinecraftClient#getFramebuffer()} to return an own framebuffer so that minecraft writes results
 * to that framebuffer and not the main framebuffer.
 *
 * @author ccetl
 */
public final class GlobalFramebuffer {

    private GlobalFramebuffer() {}

    private static final IntArrayList readStack = new IntArrayList(2);
    private static final IntArrayList writeStack = new IntArrayList(2);
    private static final ObjectArrayList<AbstractFramebuffer> stack = new ObjectArrayList<>(1);

    static {
        readStack.add(0);
        writeStack.add(0);
    }

    private static boolean lock;
    private static boolean minecraftChangesRead;
    private static boolean minecraftChangesWrite;

    // framebuffers minecraft sets
    public static void updateRead(int id) {
        if (!minecraftChangesRead || id == readStack.getInt(0)) {
            readStack.set(0, id);
            minecraftChangesRead = false;
        }

        if (!lock || minecraftChangesRead) {
            bindRead(id);
        }
    }

    public static void updateWrite(int id) {
        if (!minecraftChangesWrite || id == writeStack.getInt(0)) {
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
        return readStack.getInt(0);
    }

    public static int getWrite() {
        minecraftChangesWrite = true;
        return writeStack.getInt(0);
    }

    public static void push(AbstractFramebuffer spoofedFramebuffer) {
        if (!stack.isEmpty() && stack.top() == spoofedFramebuffer) {
            return;
        }

        stack.push(spoofedFramebuffer);
        readStack.push(spoofedFramebuffer.getId());
        writeStack.push(spoofedFramebuffer.getId());
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
            readStack.popInt();
            if (!minecraftChangesRead) {
                bindRead(readStack.topInt());
            }
        }

        if (!writeStack.isEmpty()) {
            writeStack.popInt();
            if (!minecraftChangesWrite) {
                bindWrite(writeStack.topInt());
            }
        }

        if (!stack.isEmpty()) {
            stack.pop();
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
