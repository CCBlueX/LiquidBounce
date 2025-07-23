package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.stages

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NoteBlockTracker
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NotebotEngine
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NotebotScanner
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.util.Formatting

class NotebotTestStageHandler(
    engine: NotebotEngine
): ModuleNotebot.NotebotStageHandler {
    private val allBlocks = engine.blocksAndRequirements.availableBlocks.flatMap { it.value }
    private val remainingNoteBlocks = ArrayDeque(allBlocks)

    override val handledStage: ModuleNotebot.NotebotStage
        get() = ModuleNotebot.NotebotStage.TEST

    override fun onTick(engine: NotebotEngine) {
        val untestedBlock = getNextBlockToTest()

        if (untestedBlock == null) {
            chat("All blocks tested, starting tuning...".asText().formatted(Formatting.GREEN), ModuleNotebot)
            engine.changeStage(NotebotTuneStageHandler(engine))

            return
        }

        if (!untestedBlock.canTestRightNow()) {
            return
        }

        untestedBlock.testOnce()

        // Requeue for checking at another point in time...
        remainingNoteBlocks.add(untestedBlock)

        val notTestedBlocks = remainingNoteBlocks.count { it.currentNote == null }

        val total = this.allBlocks.size

        ModuleNotebot.sendNewProgressMessage("Test", total - notTestedBlocks, total)
    }

    private fun getNextBlockToTest(): NoteBlockTracker? {
        while (remainingNoteBlocks.isNotEmpty()) {
            val currentSubject = remainingNoteBlocks.removeFirst()

            // Check if the note is already known...
            if (currentSubject.currentNote == null) {
                return currentSubject
            }
        }

        return null
    }

}
