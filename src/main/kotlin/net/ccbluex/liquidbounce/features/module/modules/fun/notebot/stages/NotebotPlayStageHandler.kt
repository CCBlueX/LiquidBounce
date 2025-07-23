package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.stages

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NoteBlockTracker
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NotebotEngine
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.InstrumentNote
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.util.Formatting

class NotebotPlayStageHandler(
    private val availableBlocksForNote: Map<InstrumentNote, List<NoteBlockTracker>>
) : ModuleNotebot.NotebotStageHandler {
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

            ModuleNotebot.sendNewProgressMessage("Play", currentSongTick, songData.songTickLength)

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
        val usedBlocks = hashSetOf<NoteBlockTracker>()

        notes.forEach { note ->
            val instrumentNote = ModuleNotebot.getPlayedNote(note)

            val blockToPlayWith = this.availableBlocksForNote[instrumentNote]!!.firstOrNull { it !in usedBlocks }

            if (blockToPlayWith != null) {
                blockToPlayWith.click()

                usedBlocks.add(blockToPlayWith)
            }
        }
    }

}
