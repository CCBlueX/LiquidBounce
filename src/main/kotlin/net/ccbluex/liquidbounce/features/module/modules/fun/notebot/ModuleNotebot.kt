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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.NbsLoader
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.util.Formatting

/**
 * Notebot Module
 *
 * Automatically plays noteblock songs from NBS files.
 *
 * @author ccetl
 */
object ModuleNotebot : ClientModule("Notebot", Category.FUN, disableOnQuit = true) {

    private val song by text("SongName", "")
    private val pianoOnly by boolean("PianoOnly", false)
    val range by float("Range", 6f, 1f..6f)
    val rotationsConfigurable = RotationsConfigurable(this)
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    val renderer = tree(NotebotRenderer)

    var state = NotebotState.TEST
        internal set
    var previousState = NotebotState.TEST
        private set

    val noteBlocks = mutableListOf<NoteBlock>()
    var songData: SongData? = null
        private set

    private var packetMineState = false

    internal var readyToStart = false
        private set

    init {
        NotebotEngine
    }

    override suspend fun enabledEffect() {
        chat("Starting loading song...", this)
        val songData = withContext(Dispatchers.IO) {
            NbsLoader.load(song)
        } ?: run {
            this.enabled = false
            return
        }

        if (!NotebotScanner.scanAndAssignNotes(songData)) {
            this.enabled = false
            return
        }

        this.songData = songData
        packetMineState = ModulePacketMine.enabled
        ModulePacketMine.enabled = false
        chat("Starting testing...".asText().formatted(Formatting.GREEN), this)
        readyToStart = true
    }

    override fun disable() {
        noteBlocks.clear()
        readyToStart = false
        songData = null

        previousState = state
        state = NotebotState.TEST
        NotebotEngine.reset()

        if (packetMineState) {
            ModulePacketMine.enabled = true
        }

        renderer.clearSilently()
    }

    fun instrumentFromNbs(id: Int): NoteBlockInstrument = when {
        pianoOnly -> NoteBlockInstrument.HARP
        else -> when (id) {
            1 -> NoteBlockInstrument.BASS
            2 -> NoteBlockInstrument.BASEDRUM
            3 -> NoteBlockInstrument.SNARE
            4 -> NoteBlockInstrument.HAT
            5 -> NoteBlockInstrument.GUITAR
            6 -> NoteBlockInstrument.FLUTE
            7 -> NoteBlockInstrument.BELL
            8 -> NoteBlockInstrument.CHIME
            9 -> NoteBlockInstrument.XYLOPHONE
            10 -> NoteBlockInstrument.IRON_XYLOPHONE
            11 -> NoteBlockInstrument.COW_BELL
            12 -> NoteBlockInstrument.DIDGERIDOO
            13 -> NoteBlockInstrument.BIT
            14 -> NoteBlockInstrument.BANJO
            15 -> NoteBlockInstrument.PLING
            else -> NoteBlockInstrument.HARP // 0
        }
    }

}
