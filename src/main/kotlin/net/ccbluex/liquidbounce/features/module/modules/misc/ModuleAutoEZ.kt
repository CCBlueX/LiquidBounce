package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket

object ModuleAutoEZ : ClientModule("AutoEz", Category.MISC, aliases = arrayOf("AutoL")) {

    private object WordPatternCustom : Choice("Custom") {
        override val parent: ChoiceConfigurable<*>
            get() = wordPattern

        val customMessages by textList("CustomMessages", mutableListOf(""))
    }

    private object WordPatternFile : Choice("File") {
        override val parent: ChoiceConfigurable<*>
            get() = wordPattern

        private val source = file("Source")

        val messages: List<String>
            get() {
                val file = source.absoluteFile.takeIf {
                    it.exists() && it.isFile && it.canRead() && it.length() != 0L
                } ?: return emptyList()

                return file.readText().split('\n').map { it.trim() }.filter { it.isNotEmpty() }
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
                sayL(it.name.literalString!!)
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
        private val packetEventHandler = handler<PacketEvent> { event ->
            val packet = event.packet
            if (packet !is GameMessageS2CPacket) return@handler

            val message = packet.content.string

            val patterns = listOf(
                Regex("(.+?)被(.+?)击败"),
                Regex("(.+?)被炸成了粉尘, 最终还是被(.+?)击败!?"),
                Regex("(.+?)消逝了, 最终还是被(.+?)击败!?"),
                Regex("(.+?)被架在了烧烤架上, 熟透了, 最终还是被(.+?)击败!?"),
                Regex("(.+?)跑得很快, 但是他还是摔了一跤, 最终被(.+?)击败?"),
                Regex("(.+?)被(.+?)用弓箭射穿了"),
                Regex("(.+?)被重压地无法呼吸, 最终还是被(.+?)击败!?")
            )

            for (pattern in patterns) {
                val match = pattern.find(message) ?: continue
                val victim = match.groupValues[1].trim()
                val killer = match.groupValues[2].trim()

                if (killer == player.name.string) {
                    sayL(victim)
                    return@handler
                }
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

    private fun sayL(name: String) {
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
