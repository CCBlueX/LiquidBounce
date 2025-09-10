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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.stages

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NoteBlockTracker
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NotebotEngine
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.InstrumentNote
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.util.Formatting

class NotebotPlayStageHandler(
    private val availableBlocksForNote: Map<InstrumentNote, List<NoteBlockTracker>>
) : ModuleNotebot.NotebotStageHandler {

    private val progressName = ModuleNotebot.message("progressPlay")
    private var songTickAccumulator = 0f
    private var currentSongTick = 0

    override val handledStage: ModuleNotebot.NotebotStage
        get() = ModuleNotebot.NotebotStage.PLAY

    override fun onTick(engine: NotebotEngine) {
        val songData = engine.songData

        songTickAccumulator += songData.songTicksPerGameTick

        while (songTickAccumulator >= 1f) {
            songTickAccumulator -= 1f
            currentSongTick++

            ModuleNotebot.sendNewProgressMessage(progressName, currentSongTick, songData.songTickLength)

            if (currentSongTick > songData.songTickLength) {
                chat(ModuleNotebot.message("finished").formatted(Formatting.GREEN), ModuleNotebot)
                ModuleNotebot.enabled = false
                return
            }

            playNotesAtTick(currentSongTick, songData)
        }
    }

    private fun playNotesAtTick(tick: Int, songData: SongData) {
        val notes = songData.notesByTick[tick] ?: return
        val usedBlocks = hashSetOf<NoteBlockTracker>()

        notes.forEach { note ->
            val instrumentNote = ModuleNotebot.getPlayedNote(note)

            val blockToPlayWith = this.availableBlocksForNote[instrumentNote]!!.firstOrNull { it !in usedBlocks }

            if (blockToPlayWith != null) {
                blockToPlayWith.click()

                if (!ModuleNotebot.reuseBlocks) {
                    usedBlocks.add(blockToPlayWith)
                }
            }
        }
    }

}
