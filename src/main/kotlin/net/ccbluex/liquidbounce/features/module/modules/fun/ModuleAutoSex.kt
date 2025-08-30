@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.player.PlayerEntity
import kotlin.random.Random


object ModuleAutoSex : ClientModule("AutoSex", Category.FUN) {

    private val targetRange by float("TargetRange", 5f, 1f..10f)
    private val mode by enumChoice("SexMode", SexMode.Active)
    private val msgDelay by int("MessageDelay", 1, 0..50, "seconds")

    private enum class SexMode(
        override val choiceName: String,
    ) : NamedChoice {
        Active("Active"),
        Passive("Passive"),
    }

    private val PASSIVE_MESSAGES = arrayOf(
        "It's so Biiiiiiig",
        "Be careful daddy <3",
        "Oh, I feel it inside me!"
    )
    private val ACTIVE_MESSAGES = arrayOf(
        "Oh, I'm cumming!",
        "Oh, ur pussy is so nice!",
        "Yeah, yeah",
        "I feel u!",
        "Oh, im inside u"
    )

    private var target: PlayerEntity? = null
    private var lastMessageTime = 0L
    private var lastSneakToggleTime = 0L

    private val movementInputHandler = handler<MovementInputEvent> { event ->
        when (mode) {
            SexMode.Active -> {
                if (System.currentTimeMillis() - lastSneakToggleTime > Random.nextLong(200, 1200)) {
                    event.sneak = !event.sneak
                    lastSneakToggleTime = System.currentTimeMillis()
                }
            }
            SexMode.Passive -> event.sneak = true
        }
    }


    private fun getNearestPlayer(range: Float): PlayerEntity? {
        return mc.world?.players
            ?.filter { it != mc.player && it.isAlive && it.distanceTo(mc.player) <= range }
            ?.minByOrNull { it.distanceTo(mc.player) }
    }


    private val tickHandler = tickHandler {
        if (player.isSpectator || player.isCreative) return@tickHandler

        target = if (target == null || target!!.squaredDistanceTo(player) > targetRange * targetRange) {
            getNearestPlayer(targetRange)
        } else {
            target
        }

        target ?: return@tickHandler

        if (System.currentTimeMillis() - lastMessageTime >= msgDelay * 1000L) {
            val messages = if (mode == SexMode.Active) ACTIVE_MESSAGES else PASSIVE_MESSAGES
            val message = messages.random()
            network.sendChatCommand("msg ${target!!.name.literalString} $message")
            lastMessageTime = System.currentTimeMillis()
        }
    }

}
