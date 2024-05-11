/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * SafeWalk module
 *
 * Prevents you from falling down as if you were sneaking.
 */
object ModuleSafeWalk : Module("SafeWalk", Category.MOVEMENT) {

    @Suppress("UnusedPrivateProperty")
    private val modes = choices("Mode", {
        it.choices[1] // Safe mode
    }, this::createChoices)

    fun createChoices(it: ChoiceConfigurable<Choice>) =
        arrayOf(NoneChoice(it), Safe(it), Simulate(it), OnEdge(it))

    class Safe(override val parent: ChoiceConfigurable<Choice>) : Choice("Safe") {

        private val eagleOnLedge by boolean("EagleOnLedge", false)

        val inputHandler = handler<MovementInputEvent> { event ->
            if (eagleOnLedge) {
                val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
                    SimulatedPlayer.SimulatedPlayerInput(
                        event.directionalInput,
                        event.jumping,
                        player.isSprinting,
                        true
                    ))
                simulatedPlayer.tick()

                if (simulatedPlayer.clipLedged) {
                    event.sneaking = true
                }
            }
        }

        @Suppress("unused")
        val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
            event.isSafeWalk = true
        }

    }

    class Simulate(override val parent: ChoiceConfigurable<Choice>) : Choice("Simulate") {

        private val predict by int("Ticks", 5, 0..20, "ticks")

        /**
         * The input handler tracks the movement of the player and calculates the predicted future position.
         */
        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent> { event ->
            if (player.isOnGround && !player.isSneaking) {
                val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
                    SimulatedPlayer.SimulatedPlayerInput(
                        event.directionalInput,
                        event.jumping,
                        player.isSprinting,
                        true
                    ))

                // TODO: Calculate the required ticks early that prevent the player from falling off the edge
                //  instead of relying on the static predict value.
                repeat(predict) {
                    simulatedPlayer.tick()

                    if (simulatedPlayer.clipLedged) {
                        event.directionalInput = DirectionalInput.NONE
                    }
                }
            }
        }

    }

    class OnEdge(override val parent: ChoiceConfigurable<Choice>) : Choice("OnEdge") {

        private val edgeDistance by float("EdgeDistance", 0.01f, 0.01f..0.5f)

        /**
         * The input handler tracks the movement of the player and calculates the predicted future position.
         */
        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent>(
            priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
        ) { event ->
            val shouldBeActive = player.isOnGround && !player.isSneaking

            if (shouldBeActive && player.isCloseToEdge(event.directionalInput, edgeDistance.toDouble())) {
                event.directionalInput = DirectionalInput.NONE
            }
        }

    }

}
