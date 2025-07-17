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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.util.Formatting
import java.io.File
import java.io.IOException
import java.util.*

object NbsLoader {

    val root = File(ConfigSystem.rootFolder, "nbs").apply { mkdirs() }

    fun load(songName: String): SongData? {
        val nbsFile = root.resolve("$songName.nbs")
        if (!nbsFile.exists()) {
            chat("No NBS found at ${nbsFile.absolutePath}!".asText().formatted(Formatting.RED), ModuleNotebot)
            return null
        }

        return try {
            val nbs = Nbs(nbsFile.inputStream(), songName)
            val notesByTick = buildNotesByTick(nbs)
            val songTickLength = Collections.max(notesByTick.keys)
            val songTicksPerGameTick = (nbs.header.tempo / 100.0f) / 20.0f

            SongData(nbs, notesByTick, songTickLength, songTicksPerGameTick)
        } catch (e: IOException) {
            e.printStackTrace()
            chat("Could not parse the NoteBlockSong!".asText().formatted(Formatting.RED), ModuleNotebot)
            null
        }
    }

    private fun buildNotesByTick(nbs: Nbs): Map<Int, List<NbsNoteBlock>> {
        return nbs.noteBlocks.groupBy { it.tick }.mapValues { it.value.toList() }
    }

}
