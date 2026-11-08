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

import net.ccbluex.liquidbounce.utils.client.logger
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.log10

/**
 * Plays the splash's sounds, each on a line of its own while it lasts. On PulseAudio's ALSA plugin a Clip stops
 * after half a second, a streamed line plays to the end.
 */
internal class SplashSoundPlayer(private val sounds: Map<String, SplashSound>) : AutoCloseable {

    private val playing = HashSet<SourceDataLine>()
    private var closed = false

    /** Plays [name] at [volume], the game's master volume. */
    fun play(name: String, volume: Float) {
        val sound = sounds[name] ?: return
        if (volume <= 0f) {
            return
        }

        thread(name = "Splash sound", isDaemon = true) {
            runCatching {
                AudioSystem.getSourceDataLine(sound.format).use { line ->
                    if (!open(line, sound)) {
                        return@use
                    }
                    try {
                        line.setVolume(volume)
                        line.start()
                        line.write(sound.pcm, 0, sound.pcm.size)
                        // The ALSA plugin plays nothing of a sound that does not fill the line's buffer
                        line.writeSilence(line.bufferSize)
                        line.drain()
                    } finally {
                        synchronized(this) { playing -= line }
                    }
                }
            }.onFailure { logger.debug("Failed to play splash sound $name", it) }
        }
    }

    /** Opens [line] unless the player is closed, so that [close] stops it. */
    private fun open(line: SourceDataLine, sound: SplashSound): Boolean = synchronized(this) {
        if (closed) {
            return false
        }
        line.open(sound.format)
        playing += line
        true
    }

    override fun close() = synchronized(this) {
        closed = true
        playing.forEach(SourceDataLine::close)
    }

}

private val SILENCE = ByteArray(4096)

private fun SourceDataLine.writeSilence(bytes: Int) {
    var left = bytes
    while (left > 0 && isOpen) {
        left -= write(SILENCE, 0, minOf(left, SILENCE.size))
    }
}

private fun SourceDataLine.setVolume(volume: Float) {
    if (isControlSupported(FloatControl.Type.MASTER_GAIN)) {
        val gain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gain.value = (20f * log10(volume)).coerceIn(gain.minimum, gain.maximum)
    }
}
