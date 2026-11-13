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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.render.ReplayBuffer
import java.io.File
import java.time.LocalDateTime
import java.util.EnumMap
import java.util.concurrent.TimeUnit

/**
 * Saves a video of the moments worth a look, from 20 s before until 5 s after, to
 * `~/Videos/lb-bot-clips/<instance>/<time>_<event>.mp4`, see [ReplayBuffer]. `.clip <label>` saves one by hand.
 */
internal object Clips : ToggleableValueGroup(ModuleGameBot, "Clips", true) {

    private val triggers by multiEnumChoice("Events", Trigger.entries)

    enum class Trigger(override val tag: String) : Tagged {
        /** Died without a hit taken shortly before, as in the void. */
        DEATH("Death"),
        FALL("Fall"),
        RESCUE("Rescue"),
        SETBACKS("Setbacks"),
        STUCK("Stuck"),

        /** Died with hits taken or a killer named. */
        LOST_FIGHT("LostFight"),
    }

    private val logger = clientLogger("Clips")
    private val folder = File(System.getProperty("user.home"), "Videos/lb-bot-clips")

    private val lastSaved = EnumMap<Trigger, Long>(Trigger::class.java)
    private val setbacks = ArrayDeque<Long>()
    private var stuckCount = -1

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> {
        ReplayBuffer.capture()
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        val count = ModuleGameBot.games.activeMode.navigator?.view?.stuckCount ?: return@handler
        if (stuckCount in 0 until count) {
            trigger(Trigger.STUCK)
        }
        stuckCount = count
    }

    /** Saves a clip for [trigger] unless it is off or one was saved for it within [COOLDOWN_NANOS]. */
    fun trigger(trigger: Trigger) {
        if (!running || trigger !in triggers) {
            return
        }
        val now = System.nanoTime()
        val last = lastSaved[trigger]
        if (last != null && now - last < COOLDOWN_NANOS) {
            return
        }
        lastSaved[trigger] = now
        save(trigger.name.lowercase())
    }

    /** A clip once [SETBACK_BURST] setbacks came within [SETBACK_WINDOW_NANOS]. */
    fun noteSetback() {
        val now = System.nanoTime()
        setbacks.addLast(now)
        while (now - setbacks.first() > SETBACK_WINDOW_NANOS) {
            setbacks.removeFirst()
        }
        if (setbacks.size >= SETBACK_BURST) {
            trigger(Trigger.SETBACKS)
        }
    }

    /** Saves a clip named [label] right away; false while clips are not recorded. */
    fun save(label: String): Boolean {
        if (!running) {
            return false
        }
        val name = "${LocalDateTime.now().toUnderlinedString()}_$label.mp4"
        val output = File(folder, "${mc.gameDirectory.name}/$name")
        ReplayBuffer.save(output, ::trimFolder) { file ->
            mc.execute { report("clip saved ${file.path} event=$label") }
        }
        return true
    }

    /** Deletes the oldest clips of every instance once all of them take more than [MAX_BYTES]. */
    private fun trimFolder() {
        val clips = folder.walkTopDown().filter { it.isFile && it.extension == "mp4" }.toMutableList()
        var total = clips.sumOf { it.length() }
        if (total <= MAX_BYTES) {
            return
        }
        clips.sortBy { it.lastModified() }
        for (clip in clips) {
            if (total <= TRIM_TO_BYTES) {
                break
            }
            val size = clip.length()
            if (clip.delete()) {
                total -= size
                logger.info("Deleted old clip $clip")
            }
        }
    }

    private val COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(30)
    private val SETBACK_WINDOW_NANOS = TimeUnit.SECONDS.toNanos(5)
    private const val SETBACK_BURST = 3
    private const val MAX_BYTES = 100L * 1024 * 1024
    private const val TRIM_TO_BYTES = 80L * 1024 * 1024

}
