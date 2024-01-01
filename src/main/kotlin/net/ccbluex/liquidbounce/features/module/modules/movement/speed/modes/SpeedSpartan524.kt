package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.zeroXZ


/**
 * @anticheat Spartan
 * @anticheatVersion phase 524
 * @testedOn minecraft.vagdedes.com
 * @note it might flag a bit at the start, but then stops for some reason
 */
object SpeedSpartan524 : Choice("Spartan524") {
    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    val repeatable = repeatable {
        if (!player.moving) {
            return@repeatable
        }

        Timer.requestTimerSpeed(1.1f, priority = Priority.IMPORTANT_FOR_USAGE_1)

        when {
            player.isOnGround -> {
                player.strafe(speed = 0.83)
                player.velocity.y = 0.16
            }
        }
        player.strafe()
    }

    override fun enable() {
        player.zeroXZ()
        player.velocity.y = 0.0
    }
}

/**
 * @anticheat Spartan
 * @anticheatVersion phase 524
 * @testedOn minecraft.vagdedes.com
 * @note it will flag you for jumping
 */
object SpeedSpartan524GroundTimer : Choice("Spartan524GroundTimer") {
    val additionalTicks by int("AdditionalTicks", 2, 1..10)
    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    val repeatable = handler<PlayerPostTickEvent> {
        repeat(additionalTicks) {
            player.tickMovement()
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> { event ->
        event.cancelEvent()
    }
}

