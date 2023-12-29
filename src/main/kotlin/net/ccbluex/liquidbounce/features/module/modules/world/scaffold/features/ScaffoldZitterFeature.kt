package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.cos
import kotlin.math.sin

object ScaffoldZitterFeature {
    object Off : Choice("Off") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes
    }

    object Teleport : Choice("Teleport") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes

        private val speed by float("Speed", 0.13f, 0.1f..0.3f)
        private val strength by float("Strength", 0.05f, 0f..0.2f)
        private val groundOnly by boolean("GroundOnly", true)
        private var zitterDirection = false

        val repeatable =
            repeatable {
                if (player.isOnGround || !groundOnly) {
                    player.strafe(speed = speed.toDouble())
                    val yaw = Math.toRadians(player.yaw + if (zitterDirection) 90.0 else -90.0)
                    player.velocity.x -= sin(yaw) * strength
                    player.velocity.z += cos(yaw) * strength
                    zitterDirection = !zitterDirection
                }
            }
    }

    object Smooth : Choice("Smooth") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes

        // Move direction (false = right, true = left)
        private var moveDirection = false
        private var movesAway = false

        val moveInputHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) { event ->
            val directionalInput = event.directionalInput

            // Check if we are moving forwards or backwards
            if (!directionalInput.forwards && !directionalInput.backwards) {
                return@handler
            }

            if (player.isOnGround) {
                // Check if we are too close to the right or left of the block we are scaffold walking on
                val onlySideDirection = DirectionalInput(
                    right = moveDirection,
                    left = !moveDirection,
                    forwards = false,
                    backwards = false
                )

                val isCloseToEdge = player.isCloseToEdge(onlySideDirection, 0.9)
                ModuleDebug.debugParameter(ModuleScaffold, "Zitter->isCloseToEdge", isCloseToEdge)

                if (isCloseToEdge) {
                    if (!movesAway) {
                        // Set the move direction to the opposite of the current move direction
                        moveDirection = !moveDirection
                        movesAway = true
                    }
                } else {
                    movesAway = false
                }

                // Set the move keys to the opposite of the current move direction
                event.directionalInput = event.directionalInput.copy(
                    right = !moveDirection,
                    left = moveDirection
                )
            }
        }

    }
}
