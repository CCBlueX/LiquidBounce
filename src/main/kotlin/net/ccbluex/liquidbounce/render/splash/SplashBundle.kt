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
package net.ccbluex.liquidbounce.render.splash

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.config.gson.util.string
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.sounds.JOrbisAudioStream
import net.minecraft.util.PngInfo
import java.io.File
import java.util.Base64
import javax.sound.sampled.AudioFormat

/**
 * Files of a theme, by path relative to the theme's root.
 */
internal fun interface SplashFiles {
    fun read(path: String): ByteArray?
}

internal class ResourceSplashFiles(private val root: String) : SplashFiles {
    override fun read(path: String) = javaClass.getResourceAsStream(root + path)?.use { it.readAllBytes() }
}

internal class DirectorySplashFiles(private val root: File) : SplashFiles {
    override fun read(path: String) = File(root, path).takeIf(File::isFile)?.readBytes()
}

/**
 * The `splash` section of a theme's `metadata.json`.
 */
class SplashSettings(val animation: String, val background: Int, val text: Int, val accent: Int) {

    companion object {

        const val DEFAULT_BACKGROUND = 0xFF181A1B.toInt()
        const val DEFAULT_ACCENT = 0xFF4677FF.toInt()

        fun of(metadata: JsonObject): SplashSettings? {
            val splash = metadata.getAsJsonObject("splash") ?: return null
            fun color(key: String, default: Int) =
                splash.string(key)?.let { hex -> runCatching { Color4b.fromHex(hex).argb }.getOrNull() } ?: default

            return SplashSettings(
                animation = requireNotNull(splash.string("animation")) { "splash.animation is missing" },
                background = color("background", DEFAULT_BACKGROUND),
                text = color("text", -1),
                accent = color("accent", DEFAULT_ACCENT),
            )
        }

    }

}

class SplashSound(val format: AudioFormat, val pcm: ByteArray)

/**
 * A splash screen, read and decoded: the [animation] with its [images] and the [sounds] its markers name.
 */
internal class SplashBundle(
    val settings: SplashSettings,
    val animation: LottieAnimation,
    val images: Map<String, NativeImage>,
    val sounds: Map<String, SplashSound>,
) : AutoCloseable {

    override fun close() {
        images.values.forEach(NativeImage::close)
    }

    companion object {

        const val METADATA = "metadata.json"
        const val MAX_IMAGE_SIZE = 2048
        private const val MAX_TOTAL_BYTES = 16 shl 20
        private const val MAX_SOUND_BYTES = 8 shl 20
        private const val DATA_URI = "base64,"

        /**
         * Reads and decodes the splash of the theme in [files]; null when there is no theme or it has no splash.
         */
        fun load(files: SplashFiles): SplashBundle? {
            var total = 0L
            fun count(bytes: ByteArray) = bytes.also {
                total += bytes.size
                require(total <= MAX_TOTAL_BYTES) { "Splash files are larger than ${MAX_TOTAL_BYTES shr 20} MiB" }
            }
            fun read(path: String) = count(requireNotNull(files.read(checkedPath(path))) { "Missing $path" })

            val metadata = files.read(METADATA)?.let(::count) ?: return null
            val settings = SplashSettings.of(JsonParser.parseString(metadata.decodeToString()).asJsonObject)
                ?: return null
            val animation = LottieAnimation.parse(read(settings.animation).decodeToString())
            val folder = settings.animation.substringBeforeLast('/', "").let { if (it.isEmpty()) it else "$it/" }

            val images = LinkedHashMap<String, NativeImage>()
            try {
                for (image in animation.images) {
                    val bytes = if (image.path.startsWith("data:")) {
                        count(Base64.getDecoder().decode(image.path.substringAfter(DATA_URI)))
                    } else {
                        read(folder + image.path)
                    }
                    // Decoding allocates the full size the header claims
                    val header = PngInfo.fromBytes(bytes)
                    require(header.width() <= MAX_IMAGE_SIZE && header.height() <= MAX_IMAGE_SIZE) {
                        "Image ${image.id} is larger than ${MAX_IMAGE_SIZE}x$MAX_IMAGE_SIZE"
                    }
                    images[image.id] = NativeImage.read(bytes)
                }

                val sounds = animation.markers
                    .filter { marker -> marker.name.startsWith(SplashTimeline.SOUND_PREFIX) }
                    .map { marker -> marker.name.removePrefix(SplashTimeline.SOUND_PREFIX) }
                    .distinct()
                    .associateWith { name -> decodeOgg(read("sounds/$name.ogg"), name) }

                return SplashBundle(settings, animation, images, sounds)
            } catch (e: Exception) {
                images.values.forEach(NativeImage::close)
                throw e
            }
        }

        /** Paths stay inside the theme. */
        private fun checkedPath(path: String): String {
            val escapes = path.startsWith("/") || path.split('/', '\\').any { it == ".." }
            require(!escapes) { "Path leaves the theme: $path" }
            return path
        }

        private fun decodeOgg(bytes: ByteArray, name: String) = JOrbisAudioStream(bytes.inputStream()).use { stream ->
            // Reads a little past the limit to tell a sound at the limit from a longer one
            val pcm = stream.read(MAX_SOUND_BYTES + 1)
            require(pcm.remaining() <= MAX_SOUND_BYTES) { "Sound $name is longer than ${MAX_SOUND_BYTES shr 20} MiB" }
            SplashSound(stream.format, ByteArray(pcm.remaining()).also(pcm::get))
        }

    }

}
