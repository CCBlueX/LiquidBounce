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
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.ccbluex.liquidbounce.render.setPipeline
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.renderer.RenderPipelines
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

/**
 * Keeps the last [BEFORE_SECONDS] of the game window as small JPEG frames, so a moment can be saved as a video after
 * it happened.
 *
 * The owner calls [capture] every frame. Every [FRAME_INTERVAL_NANOS] the main render target is scaled down on the
 * GPU and copied into a mapped buffer; once the copy is done the pixels are read back and a worker thread encodes
 * them. [save] waits [AFTER_SECONDS] on that worker, then hands the frames to `ffmpeg` in its own process.
 */
object ReplayBuffer {

    private val logger = clientLogger("ReplayBuffer")

    private class Frame(val at: Long, val width: Int, val height: Int, val jpeg: ByteArray)

    private val worker = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ReplayBuffer").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    // Render thread only.
    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var buffer: GpuBuffer? = null
    private var width = 0
    private var height = 0
    private var lastCapture = 0L
    private var renderNanos = 0L
    private var captures = 0

    @Volatile
    private var copying = false

    private val spareArrays = ConcurrentLinkedQueue<ByteArray>()

    // Worker only.
    private val frames = ArrayDeque<Frame>()
    private var image: BufferedImage? = null
    private val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
    private val jpegParams = jpegWriter.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = JPEG_QUALITY
    }

    /** Grabs the frame shown last when one is due; cheap to call every frame. */
    fun capture() {
        val now = System.nanoTime()
        if (copying || now - lastCapture < FRAME_INTERVAL_NANOS) {
            return
        }
        val source = mc.gameRenderer.mainRenderTarget()
        val sourceView = source.colorTextureView ?: return
        if (source.width <= 0 || source.height <= 0) {
            return
        }

        lastCapture = now
        fit(source.width, source.height)
        val texture = texture ?: return
        val buffer = buffer ?: return
        val width = width
        val height = height

        val encoder = gpuDevice.createCommandEncoder()
        encoder.createRenderPass({ "Replay buffer blit" }, textureView!!, Optional.empty()).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            pass.setPipeline(RenderPipelines.TRACY_BLIT)
            pass.setUniform("InSampler", sourceView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.draw(3, 1, 0, 0)
        }
        copying = true
        encoder.copyTextureToBuffer(texture, buffer, 0L, { readBack(now, buffer, width, height) }, 0)
        renderNanos += System.nanoTime() - now
        captures++
    }

    /**
     * Saves the frames from [BEFORE_SECONDS] before until [AFTER_SECONDS] after now as an MP4 to [output].
     * [prepare] runs on the worker right before anything is written, [onSaved] once the video is complete.
     */
    fun save(output: File, prepare: () -> Unit, onSaved: (File) -> Unit) {
        val at = System.nanoTime()
        worker.schedule({
            try {
                write(at, output, prepare, onSaved)
            } catch (e: Exception) {
                logger.error("Saving clip $output failed", e)
            }
        }, AFTER_SECONDS, TimeUnit.SECONDS)
    }

    /** Sizes the capture to fit [MAX_WIDTH] x [MAX_HEIGHT] with the aspect of the window, in steps of 4 pixels. */
    private fun fit(sourceWidth: Int, sourceHeight: Int) {
        val scale = minOf(1f, MAX_WIDTH.toFloat() / sourceWidth, MAX_HEIGHT.toFloat() / sourceHeight)
        val width = ((sourceWidth * scale).toInt() / 4 * 4).coerceAtLeast(4)
        val height = ((sourceHeight * scale).toInt() / 4 * 4).coerceAtLeast(4)
        if (width == this.width && height == this.height && texture != null) {
            return
        }

        textureView?.close()
        texture?.close()
        buffer?.close()
        val texture = gpuDevice.createTexture(
            "Replay buffer", GpuTexture.USAGE_COPY_SRC or GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM, width, height, 1, 1,
        )
        this.texture = texture
        textureView = gpuDevice.createTextureView(texture)
        buffer = gpuDevice.createBuffer(
            { "Replay buffer pixels" }, GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            width * height * BYTES_PER_PIXEL,
        )
        this.width = width
        this.height = height
    }

    private fun readBack(at: Long, buffer: GpuBuffer, width: Int, height: Int) {
        val start = System.nanoTime()
        val size = width * height * BYTES_PER_PIXEL.toInt()
        val pixels = spareArrays.poll()?.takeIf { it.size == size } ?: ByteArray(size)
        try {
            if (!buffer.isClosed) {
                buffer.map(true, false).use { view -> view.data().get(0, pixels) }
            }
        } finally {
            copying = false
        }
        renderNanos += System.nanoTime() - start
        worker.execute { encode(at, width, height, pixels) }
    }

    private fun encode(at: Long, width: Int, height: Int, pixels: ByteArray) {
        val image = image?.takeIf { it.width == width && it.height == height }
            ?: BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR).also { image = it }
        val bgr = (image.raster.dataBuffer as DataBufferByte).data
        // The GPU copy starts at the bottom row.
        for (y in 0 until height) {
            var from = (height - 1 - y) * width * BYTES_PER_PIXEL.toInt()
            var to = y * width * 3
            repeat(width) {
                bgr[to] = pixels[from + 2]
                bgr[to + 1] = pixels[from + 1]
                bgr[to + 2] = pixels[from]
                from += BYTES_PER_PIXEL.toInt()
                to += 3
            }
        }
        spareArrays.offer(pixels)

        val out = ByteArrayOutputStream(JPEG_CAPACITY)
        MemoryCacheImageOutputStream(out).use { stream ->
            jpegWriter.output = stream
            jpegWriter.write(null, IIOImage(image, null, null), jpegParams)
        }
        frames.addLast(Frame(at, width, height, out.toByteArray()))
        while (at - frames.first().at > KEEP_NANOS) {
            frames.removeFirst()
        }
    }

    private fun write(at: Long, output: File, prepare: () -> Unit, onSaved: (File) -> Unit) {
        val clip = frames.filter { it.at >= at - BEFORE_NANOS }
        if (clip.size < 2) {
            logger.warn("No frames to save as $output")
            return
        }

        prepare()
        val folder = output.absoluteFile.parentFile
        val dir = File(folder, ".${output.nameWithoutExtension}.frames")
        dir.mkdirs()
        val list = StringBuilder()
        clip.forEachIndexed { index, frame ->
            val name = "%05d.jpg".format(Locale.ROOT, index)
            File(dir, name).writeBytes(frame.jpeg)
            val next = clip.getOrNull(index + 1)?.at ?: (frame.at + FRAME_INTERVAL_NANOS)
            list.append("file '").append(name).append("'\nduration ")
                .append("%.4f".format(Locale.ROOT, (next - frame.at) / 1e9)).append('\n')
        }
        // The concat demuxer only applies the duration of the last entry when it is listed again.
        list.append("file '%05d.jpg'\n".format(Locale.ROOT, clip.lastIndex))
        val listFile = File(dir, "frames.txt")
        listFile.writeText(list.toString())

        val last = clip.last()
        val part = File(folder, "${output.name}.part")
        val log = File(dir, "ffmpeg.log")
        val command = buildList {
            if (File(NICE).canExecute()) {
                addAll(listOf(NICE, "-n", "15"))
            }
            addAll(listOf(
                FFMPEG, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", listFile.path,
                "-vf", "scale=${last.width}:${last.height},format=yuv420p", "-fps_mode", "vfr",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "26", "-threads", "2",
                "-movflags", "+faststart", "-f", "mp4", part.path,
            ))
        }
        val cost = if (captures > 0) renderNanos / captures / 1e6 else 0.0
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
        process.onExit().thenAccept { done ->
            worker.execute {
                if (done.exitValue() == 0 && part.renameTo(output)) {
                    logger.info(
                        "Saved $output frames=${clip.size} " +
                            "renderThread=${"%.3f".format(Locale.ROOT, cost)}ms/capture captures=$captures"
                    )
                    onSaved(output)
                } else {
                    logger.warn("ffmpeg failed for $output (exit ${done.exitValue()}): ${log.readText().trim()}")
                    part.delete()
                }
                dir.deleteRecursively()
            }
        }
    }

    private const val FFMPEG = "/usr/bin/ffmpeg"
    private const val NICE = "/usr/bin/nice"

    private const val MAX_WIDTH = 640
    private const val MAX_HEIGHT = 360
    private const val BYTES_PER_PIXEL = 4L
    private const val JPEG_QUALITY = 0.8f
    private const val JPEG_CAPACITY = 64 * 1024

    private const val FRAME_INTERVAL_NANOS = 125_000_000L
    private const val BEFORE_SECONDS = 20L
    private const val AFTER_SECONDS = 5L
    private val BEFORE_NANOS = TimeUnit.SECONDS.toNanos(BEFORE_SECONDS)

    /**
     * A little more than [BEFORE_SECONDS] plus [AFTER_SECONDS], so a save due right now still finds its first frame.
     */
    private val KEEP_NANOS = TimeUnit.SECONDS.toNanos(BEFORE_SECONDS + AFTER_SECONDS + 1)

}
