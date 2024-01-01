package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.TickJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.MovementType

/**
 * @anticheat Verus
 * @anticheatVersion b3882
 * @testedOn eu.anticheat-test.com
 */
object SpeedVerusB3882 : Choice("VerusB3882") {

    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    val tickJumpHandler = handler<TickJumpEvent> {
        if (player.isOnGround && player.moving) {
            player.jump()
            player.velocity.x *= 1.1
            player.velocity.z *= 1.1
        }
    }

    val moveHandler = handler<PlayerMoveEvent> { event ->
        // Might just strafe when player controls itself
        if (event.type == MovementType.SELF && player.moving) {
            val movement = event.movement
            movement.strafe(player.directionYaw, strength = 1.0)
        }
    }

    val timerRepeatable = repeatable {
        Timer.requestTimerSpeed(2.0F, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        waitTicks(101)
    }
}
