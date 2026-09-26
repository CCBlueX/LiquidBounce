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

package net.ccbluex.liquidbounce.utils.io

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.minecraft.util.Util
import okio.Buffer
import org.lwjgl.sdl.SDLClipboard
import org.lwjgl.sdl.SDL_ClipboardCleanupCallback
import org.lwjgl.sdl.SDL_ClipboardDataCallback
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

private fun encodeClipboardData(image: NativeImage): ClipboardData {
    // Windows: use image/bmp (24-bit BGR with a BITMAPFILEHEADER - SDL3's Windows backend
    // translates it to CF_DIB). On other platforms SDL3 has no built-in BMP->CF_DIB mapping,
    // so fall back to image/png, which requires the platform's native clipboard code to
    // flatten aliased data (supported by X11/Wayland/org.kde.plasma clipboards; not verified).
    if (Util.getPlatform() != Util.OS.WINDOWS) {
        return ClipboardData("image/png", Buffer().also(image::writeToChannel).readToNativeBuffer())
    }

    val source = image.toBufferedImage()
    val bmpImage = BufferedImage(source.width, source.height, BufferedImage.TYPE_3BYTE_BGR)
    bmpImage.createGraphics().run {
        drawImage(source, 0, 0, null)
        dispose()
    }

    val bmp = Buffer().also { output ->
        check(ImageIO.write(bmpImage, "bmp", output.outputStream())) { "No BMP image writer is available" }
    }.readToNativeBuffer()

    return ClipboardData("image/bmp", bmp)
}

fun clipboardSet(image: NativeImage): Boolean {
    return SdlClipboard.set(encodeClipboardData(image))
}

fun clipboardCopyAction(image: NativeImage): Runnable {
    val data = encodeClipboardData(image)
    return Runnable { SdlClipboard.set(data) }
}

private class ClipboardData(
    private val mimeType: String,
    private val content: ByteBuffer,
) : AutoCloseable {
    private val userData = MemoryUtil.memAlloc(1)
    private val mimeTypeData = MemoryUtil.memASCII(mimeType)
    val mimeTypes = MemoryUtil.memAllocPointer(1).put(0, MemoryUtil.memAddress(mimeTypeData))
    val userDataAddress: Long
        get() = MemoryUtil.memAddress(userData)

    fun getData(requestedMimeType: Long, size: Long): Long {
        if (MemoryUtil.memASCII(requestedMimeType) != mimeType) {
            return MemoryUtil.NULL
        }

        MemoryUtil.memPutAddress(size, content.remaining().toLong())
        return MemoryUtil.memAddress(content)
    }

    override fun close() {
        MemoryUtil.memFree(userData)
        MemoryUtil.memFree(mimeTypeData)
        MemoryUtil.memFree(mimeTypes)
        MemoryUtil.memFree(content)
    }
}

private fun Buffer.readToNativeBuffer(): ByteBuffer {
    require(size <= Int.MAX_VALUE) { "Clipboard content exceeds the maximum native buffer size" }

    val buffer = MemoryUtil.memAlloc(size.toInt())
    try {
        while (buffer.hasRemaining()) {
            check(read(buffer) > 0) { "Failed to read clipboard content" }
        }
        return buffer.flip()
    } catch (throwable: Throwable) {
        MemoryUtil.memFree(buffer)
        throw throwable
    }
}

private object SdlClipboard {
    private val dataByUserData = ConcurrentHashMap<Long, ClipboardData>()

    private val dataCallback = object : SDL_ClipboardDataCallback() {
        override fun invoke(userdata: Long, mimeType: Long, size: Long): Long =
            dataByUserData[userdata]?.getData(mimeType, size) ?: MemoryUtil.NULL
    }

    private val cleanupCallback = object : SDL_ClipboardCleanupCallback() {
        override fun invoke(userdata: Long) {
            dataByUserData.remove(userdata)?.close()
        }
    }

    fun set(data: ClipboardData): Boolean {
        dataByUserData[data.userDataAddress] = data
        if (SDLClipboard.SDL_SetClipboardData(dataCallback, cleanupCallback, data.userDataAddress, data.mimeTypes)) {
            return true
        }

        dataByUserData.remove(data.userDataAddress)?.close()
        return false
    }
}
