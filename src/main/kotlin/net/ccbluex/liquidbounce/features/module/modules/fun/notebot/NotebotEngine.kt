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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import kotlin.math.ln
import kotlin.math.round

object NotebotEngine : EventListener {

    private var songTickAccumulator = 0f
    private var currentSongTick = 0

    override fun parent() = ModuleNotebot

    fun reset() {
        songTickAccumulator = 0f
        currentSongTick = 0
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet !is PlaySoundS2CPacket ||
            ModuleNotebot.state == NotebotState.PLAY ||
            packet.category != SoundCategory.RECORDS ||
            !packet.sound.key.get().value.path.contains("note_block")
        ) {
            return@handler
        }

        val pos = BlockPos((packet.x - 0.5).toInt(), (packet.y - 0.5).toInt(), (packet.z - 0.5).toInt())
        ModuleNotebot.noteBlocks
            .firstOrNull { it.blockPos == pos && (!it.deliveredCurrent || !it.verified) }
            ?.apply {
                currentNote = round(12f + 12f * (ln(packet.pitch) / ln(2.0)).toFloat()).toInt()
                deliveredCurrent = true
                verified = true
            }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        when (ModuleNotebot.state) {
            NotebotState.TEST -> handleTestState()
            NotebotState.TUNE -> handleTuneState()
            NotebotState.PLAY -> handlePlayState()
        }
    }

    private fun handleTestState() {
        if (ModuleNotebot.noteBlocks.all { it.test() }) {
            if (ModuleNotebot.noteBlocks.all { it.currentNote == it.noteValue }) {
                transitionToPlayState()
            } else {
                transitionToTuneState()
            }
        }
    }

    private fun handleTuneState() {
        if (ModuleNotebot.noteBlocks.all { it.tune() }) {
            transitionToPlayState()
        }
    }

    private fun handlePlayState() {
        val songData = ModuleNotebot.songData ?: return
        songTickAccumulator += songData.songTicksPerGameTick

        while (songTickAccumulator >= 1f) {
            songTickAccumulator -= 1f
            currentSongTick++

            if (currentSongTick > songData.songTickLength) {
                chat("Finished song!".asText().formatted(Formatting.GREEN), ModuleNotebot)
                ModuleNotebot.enabled = false
                return
            }

            playNotesAtTick(currentSongTick, songData)
        }
    }

    private fun playNotesAtTick(tick: Int, songData: SongData) {
        val notes = songData.notesByTick[tick] ?: return
        val usedBlocks = mutableSetOf<NoteBlock>()

        notes.forEach { note ->
            val noteValue = MathHelper.clamp(note.key - 33, 0, 24)
            val instrument = ModuleNotebot.instrumentFromNbs(note.instrument.toInt())

            ModuleNotebot.noteBlocks
                .firstOrNull {
                    it.noteValue == noteValue && it.instrument == instrument && it !in usedBlocks
                }?.apply {
                    usedBlocks.add(this)
                    click()
                }
        }
    }

    private fun transitionToPlayState() {
        chat("All blocks tested, starting playing...".asText().formatted(Formatting.GREEN), ModuleNotebot)
        ModuleNotebot.state = NotebotState.PLAY
        ModuleNotebot.noteBlocks.forEach { it.tuned = true }
    }

    private fun transitionToTuneState() {
        chat("All blocks tested, starting tuning...".asText().formatted(Formatting.GREEN), ModuleNotebot)
        ModuleNotebot.state = NotebotState.TUNE
    }

}
