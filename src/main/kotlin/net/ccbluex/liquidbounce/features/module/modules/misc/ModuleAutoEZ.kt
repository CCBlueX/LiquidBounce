package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.longs.LongArrayList
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.HeypixelSWKillEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.io.skipLine
import net.minecraft.entity.Entity
import java.io.RandomAccessFile


object ModuleAutoEZ : ClientModule("AutoEz", Category.MISC, aliases = arrayOf("AutoL")) {

    private object WordPatternCustom : Choice("Custom") {
        override val parent: ChoiceConfigurable<*>
            get() = wordPattern

        val customMessages by textList("CustomMessages", mutableListOf(""))
    }

    private object WordPatternFile : Choice("File") {
        override val parent: ChoiceConfigurable<*>
            get() = wordPattern

        private val source by file("Source").onChanged {
            lineIndex.clear()
            if (!it.isFile) return@onChanged

            lineIndex.add(0L)
            RandomAccessFile(it, "r").use { raf ->
                while (raf.skipLine() != 0L) {
                    lineIndex.add(raf.filePointer)
                }
            }
        }

        private var linear = 0
        private val lineIndex = LongArrayList()

        val messages: List<String>
            get() {
                if (lineIndex.isEmpty()) return emptyList()

                val index = lineIndex.getLong(linear++ % lineIndex.size)
                return listOf(
                    RandomAccessFile(source, "r").use { raf ->
                        raf.seek(index)
                        raf.readLine()
                    }
                )
            }
    }

    private val wordPattern = choices(
        "WordPattern",
        WordPatternCustom,
        arrayOf(
            WordPatternCustom,
            WordPatternFile,
        )
    )

    private object Normal : Choice("Normal") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        private val enemies = mutableListOf<Entity>()

        @Suppress("unused")
        private val worldChangeEventHandler = handler<WorldChangeEvent> {
            enemies.clear()
        }

        @Suppress("unused")
        private val attackEntityEventHandler = handler<AttackEntityEvent> { event ->
            if (event.entity.isPlayer && !enemies.contains(event.entity)) {
                enemies.add(event.entity)
            }
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            enemies.filter { !it.isAlive }.forEach {
                sendMessage(it.name.literalString!!)
                enemies.remove(it)
            }
        }

        override fun enable() {
            enemies.clear()
        }
    }

    private object HeypixelSW : Choice("HeypixelSW") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val heypixelSWKillEventHandler =
            handler<HeypixelSWKillEvent> { event ->
                if (event.killer == player.name.string) {
                    sendMessage(event.victim)
            }
        }
    }

    val modes = choices(
        "Mode",
        HeypixelSW,
        arrayOf(
            Normal,
            HeypixelSW
        )
    )

    private val nameInFront by boolean("NameInFront", true)
    private val advertisementInEnd by text("AdvertisementInEnd","JMcomicFix Client get->1057670997")
    private val delay by intRange("Delay", 4..5, 1..10, "secs")
    private val messagesPerTick by intRange("MessagesPerTick", 1..1, 1..10)
    private var lastSent = 0L

    private fun sendMessage(name: String) {
        val now = System.currentTimeMillis()
        if (now - lastSent < delay.random() * 1000L) return
        lastSent = now

        repeat(messagesPerTick.random()) {
            var message = when (val active = wordPattern.activeChoice) {
                is WordPatternCustom -> active.customMessages.randomOrNull().orEmpty()
                is WordPatternFile -> active.messages.randomOrNull().orEmpty()
                else -> ""
            }

            if (nameInFront) {
                message = "$name $message"
            }

            message += advertisementInEnd

            message = message.take(256)
            if (message.isNotBlank()) {
                network.sendChatMessage(message)
            }

        }
    }
}
