package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.MovementType

/**
 * tested on mineblaze.net
 * made for intave 14.8.4
 * why does this even work?
 */

class SpeedIntave14(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave14", parent) {

    private class Strafe(parent: Listenable?) : ToggleableConfigurable(parent, "Strafe", true) {
        private var strength by float("Strength", 0.29f, 0.0f..0.29f)

        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (event.type == MovementType.SELF) {
                val movement = event.movement

                if (player.isSprinting && player.isOnGround) {
                    movement.strafe(player.directionYaw, strength = strength.toDouble())
                }
            }
        }
    }

    init {
        tree(Strafe(this))
    }

    private var boost by boolean("Boost", true)

    val repeatable = repeatable {
        if (player.velocity.y > 0.003 && player.isSprinting && boost) {
            player.velocity = player.velocity.multiply(
                1.003,
                1.00,
                1.003
            )
        }
    }
}
