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
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundSource
import org.lwjgl.stb.STBVorbis
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.libc.LibCStdlib
import java.util.concurrent.CompletableFuture
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Sound effects of the splash intro, cued by [SplashScreenRenderer]'s playhead.
 *
 * Minecraft's sound engine only starts once the first resource reload is done, so these play
 * through Java Sound instead, scaled by the master volume.
 */
object SplashScreenSounds {

    private class Cue(val time: Float, val sound: String, val gain: Float) {
        var clip: Clip? = null
    }

    /**
     * Start times are set so each sound's peak lands on its frame: the riser ends at the impact.
     */
    private val CUES = arrayOf(
        Cue(150f, "fall", 0.22f),
        Cue(440f, "impact", 1.1f),
        Cue(775f, "hop", 0.8f),
        Cue(880f, "slide", 0.16f),
        // BOUNCE badge hitting its spot
        Cue(1330f, "pop", 0.6f),
        Cue(1592f, "pop", 0.35f),
        Cue(1722f, "pop", 0.2f),
    )

    /**
     * Played when the overlay starts fading into the menu.
     */
    private val OUTRO = Cue(0f, "out", 0.35f)

    /**
     * Cues this far behind the playhead are dropped, e.g. when the intro speeds up to finish.
     */
    private const val MAX_LATENESS = 120f

    private var loading: CompletableFuture<Void>? = null

    /**
     * Opens the clips off the render thread; a system without audio output just stays silent.
     */
    fun load() {
        if (loading != null) {
            return
        }

        val volume = Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.MASTER)
        loading = CompletableFuture.runAsync {
            if (volume <= 0f) {
                return@runAsync
            }

            runCatching {
                val all = CUES + OUTRO
                val decoded = all.map { it.sound }.distinct().associateWith(::decode)
                for (cue in all) {
                    val (format, samples) = decoded.getValue(cue.sound)
                    val bytes = pcm(samples, cue.gain * volume)
                    cue.clip = AudioSystem.getClip().apply { open(format, bytes, 0, bytes.size) }
                }
            }.onFailure {
                logger.debug("Splash screen sounds unavailable", it)
            }
        }
    }

    /**
     * Plays the cues the playhead passed since [from].
     */
    fun play(from: Float, to: Float) {
        if (loading?.isDone != true || to <= from) {
            return
        }

        for (cue in CUES) {
            if (cue.time > from && cue.time <= to && to - cue.time <= MAX_LATENESS) {
                cue.start()
            }
        }
    }

    fun playOutro() {
        if (loading?.isDone == true) {
            OUTRO.start()
        }
    }

    private fun Cue.start() {
        clip?.run {
            framePosition = 0
            start()
        }
    }

    private fun decode(name: String): Pair<AudioFormat, ShortArray> {
        val bytes = LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/splash/$name.ogg")!!
            .use { it.readAllBytes() }
        val buffer = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
        try {
            MemoryStack.stackPush().use { stack ->
                val channels = stack.mallocInt(1)
                val sampleRate = stack.mallocInt(1)
                val pcm = checkNotNull(STBVorbis.stb_vorbis_decode_memory(buffer, channels, sampleRate)) {
                    "Cannot decode $name.ogg"
                }
                try {
                    // Absolute read: free() takes the buffer's address at its current position
                    val samples = ShortArray(pcm.remaining()).also { pcm.get(0, it) }
                    val format = AudioFormat(sampleRate[0].toFloat(), 16, channels[0], true, false)
                    return format to samples
                } finally {
                    LibCStdlib.free(pcm)
                }
            }
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    private fun pcm(samples: ShortArray, gain: Float): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val sample = (samples[i] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bytes[i * 2] = sample.toByte()
            bytes[i * 2 + 1] = (sample shr 8).toByte()
        }
        return bytes
    }

}
