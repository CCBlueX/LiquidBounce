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
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.InstrumentNote
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.NbsLoader
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.NbsNoteBlock
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.text.MutableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import java.util.*

/**
 * Notebot Module
 *
 * Automatically plays note block songs from NBS files.
 *
 * @author ccetl
 */
object ModuleNotebot : ClientModule("Notebot", Category.FUN, disableOnQuit = true) {

    // LWJGL native bug (windows)
    private val song = file("Song") // , supportedExtensions = setOf("nbs")
    private val pianoOnly by boolean("PianoOnly", false)
    val reuseBlocks by boolean("ReuseBlocks", true).onChanged { enabled = false }
    val range by float("Range", 6f, 1f..6f)
    val rotationsConfigurable = RotationsConfigurable(this)
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private object StartDelay : Configurable("StartDelay") {
        val test by int("Test", 0, 0..20, "ticks")
        val tune by int("Tune", 0, 0..20, "ticks")
        val play by int("Play", 2, 0..20, "ticks")
    }

    init {
        tree(StartDelay)
    }

    val renderer = tree(NotebotRenderer)

    var engine: NotebotEngine? = null
        private set

    @Suppress("unused")
    private val tickHandler = tickHandler {
        engine?.onTick(this)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlaySoundS2CPacket) {
            this.engine?.handleSoundPacket(event.packet)
        }
    }

    override suspend fun enabledEffect() {
        val messageMetadata = MessageMetadata(id = "M${this.name}#loaded", remove = false)
        mc.inGameHud.chatHud.removeMessage(messageMetadata.id)

        if (!checkRequirements()) {
            this.enabled = false
            return
        }

        val songData = loadSongData()

        if (songData == null) {
            this.enabled = false
            return
        }

        val blocksAndRequirements = NotebotScanner.scanBlocksAndCheckRequirements(songData)

        if (!blocksAndRequirements.validateRequirements()) {
            blocksAndRequirements.printRequirements()
            this.enabled = false
            return
        }

        this.setRenderedBlocks(blocksAndRequirements.availableBlocks.flatMap { it.value })

        showSongInfo(songData, messageMetadata)

        this.engine = NotebotEngine(songData, blocksAndRequirements)
        chat(message("startTesting").formatted(Formatting.GREEN), this)
    }

    fun setRenderedBlocks(blocks: List<NoteBlockTracker>) {
        renderer.clearSilently()

        blocks.forEach {
            renderer.addBlock(it.pos, false)
        }

        renderer.updateAll()
    }

    private suspend fun loadSongData(): SongData? {
        chat(message("startLoading").formatted(Formatting.GREEN), this)

        val songData = withContext(Dispatchers.IO) {
            NbsLoader.load(song.absoluteFile)
        }

        return songData
    }

    private fun checkRequirements(): Boolean {
        return when {
            !inGame -> {
                chat(markAsError(message("notInGame")), this)
                false
            }

            player.isCreative -> {
                chat(markAsError(message("inCreative")), this)
                false
            }

            ModulePacketMine.enabled -> {
                chat(markAsError(message("packetMineEnabled")), this)
                false
            }

            else -> true
        }
    }

    private fun showSongInfo(
        songData: SongData,
        messageMetadata: MessageMetadata
    ) {
        chat(
            regular(message("songInfoName", variable(songData.name))),
            messageMetadata
        )
        chat(
            regular(message("songInfoTicksPerGameTick", variable(songData.songTicksPerGameTick.toString()))),
            messageMetadata
        )
        chat(
            regular(message("songInfoTickLength", variable(songData.songTickLength.toString()))),
            messageMetadata
        )
        chat(
            regular(message("songInfoTotalNotes", variable(songData.nbs.noteBlocks.size.toString()))),
            messageMetadata
        )
    }

    override fun onDisabled() {
        removeProgressMessage()

        renderer.reset()
    }

    private val progressMessageMetadata = MessageMetadata(id = "M$name#progress", remove = false)

    private fun removeProgressMessage() {
        mc.inGameHud.chatHud.removeMessage(progressMessageMetadata.id)
    }

    fun sendNewProgressMessage(name: MutableText, progress: Int, total: Int) {
        removeProgressMessage()

        val percent = (progress.toDouble() / total.toDouble() * 100.0).toInt()
        chat(
            variable(name.copy())
                .append(regular(" ["))
                .append(textLoadingBar(percent))
                .append(regular("] "))
                .append(variable(percent.toString()))
                .append(regular("%")),
            metadata = progressMessageMetadata
        )
    }

    fun getPlayedNote(note: NbsNoteBlock): InstrumentNote {
        val noteValue = MathHelper.clamp(note.key - 33, 0, 24)
        val instrument = if (!this.pianoOnly) {
            note.instrument.toInt()
        } else {
            0
        }

        return InstrumentNote(instrument, noteValue)
    }

    fun getRequiredInstruments(songData: SongData): EnumSet<NoteBlockInstrument> {
        if (pianoOnly) {
            return EnumSet.of(NoteBlockInstrument.HARP)
        }

        return songData.nbs.noteBlocks
            .mapTo(EnumSet.noneOf(NoteBlockInstrument::class.java)) {
                InstrumentNote.getInstrumentEnumFromId(it.instrument.toInt())
            }
    }

    enum class NotebotStage(
        val stageStartDelay: () -> Int,
        val blockColor: () -> Color4b,
        val blockOutlineColor: () -> Color4b
    ) {
        TEST(StartDelay::test, NotebotRenderer::testColor, NotebotRenderer::outlineTestColor),
        TUNE(StartDelay::tune, NotebotRenderer::tuneColor, NotebotRenderer::outlineTuneColor),
        PLAY(StartDelay::play, NotebotRenderer::colorSetting, NotebotRenderer::outlineColorSetting)
    }

    interface NotebotStageHandler {
        val handledStage: NotebotStage

        fun onTick(engine: NotebotEngine)
    }
}
