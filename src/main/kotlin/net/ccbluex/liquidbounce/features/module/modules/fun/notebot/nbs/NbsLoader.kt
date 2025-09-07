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

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException

object NbsLoader {

    fun load(nbsFile: File): SongData? {
        if (!nbsFile.exists()) {
            chat(markAsError(ModuleNotebot.message("noNbs", nbsFile.absolutePath)), ModuleNotebot)
            return null
        }

        return try {
            val nbs = nbsFile.source().buffer().use { it.readNbsData() }
            val notesByTick = nbs.noteBlocks.groupBy { it.tick }
            val songTickLength = notesByTick.keys.max()
            val songTicksPerGameTick = (nbs.header.tempo / 100f) / 20f

            SongData(nbsFile.nameWithoutExtension, nbs, notesByTick, songTickLength, songTicksPerGameTick)
        } catch (e: IOException) {
            logger.error("Failed to load NBS data from ${nbsFile.absolutePath}", e)
            chat(markAsError(ModuleNotebot.message("couldNotParse")), ModuleNotebot)
            null
        }
    }

}
