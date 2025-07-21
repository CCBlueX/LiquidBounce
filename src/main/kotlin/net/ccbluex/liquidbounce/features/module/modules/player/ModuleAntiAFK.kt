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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.angle
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.rotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.Hand
import kotlin.random.Random

/**
 * AntiAFK module
 *
 * Prevents you from being kicked for AFK.
 */

object ModuleAntiAFK : ClientModule("AntiAFK", Category.PLAYER) {
    private val modes = choices(
        "Mode", RandomInteraction, arrayOf(
            OldMode, RandomInteraction, CustomMode
        )
    )

    private object OldMode : Choice("Old") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val repeatable = tickHandler {
            waitTicks(10)
            player.yaw += 180f
        }

        @Suppress("unused")
        val movementInputEvent = handler<MovementInputEvent> {
            it.directionalInput = it.directionalInput.copy(
                forwards = true
            )
        }

    }

    private object RandomInteraction : Choice("RandomInteraction") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        var randomDirection = DirectionalInput.NONE

        private val interactions by multiEnumChoice("Interaction",
            Interaction.YAW,
            Interaction.PITCH,
            Interaction.SWING_HAND,
        )

        private val delay by intRange("Delay", 4..7, 0..20, suffix = "ticks")

        @Suppress("unused")
        val repeatable = tickHandler {
            interactions.randomOrNull()?.let {
                it.perform(this@tickHandler)
                waitTicks(delay.random())
            }
        }

        @Suppress("unused")
        val movementInputEvent = handler<MovementInputEvent> {
            it.directionalInput = randomDirection
        }

        @Suppress("unused", "MagicNumber")
        private enum class Interaction(
            override val choiceName: String,
            val perform: suspend Sequence.() -> Unit,
        ): NamedChoice {
            JUMP("Jump", {
                waitNext<MovementInputEvent> { event ->
                    event.jump = true
                }
            }),
            SWING_HAND("SwingHand", {
                if (!player.handSwinging) {
                    player.swingHand(Hand.MAIN_HAND)
                }
            }),
            CHANGE_SLOT("ChangeSlot", {
                player.inventory.selectedSlot = Random.nextInt(0, 9)
            }),
            YAW("Yaw", {
                player.yaw += (-180f..180f).random()
            }),
            PITCH("Pitch", {
                player.pitch = ((-5f..5f).random() + player.pitch).coerceIn(-90f, 90f)
            }),
            RANDOM_DIRECTION("RandomDirection", {
                randomDirection = DirectionalInput(
                    Random.nextBoolean(),
                    Random.nextBoolean(),
                    Random.nextBoolean(),
                    Random.nextBoolean()
                )
                waitTicks(delay.random())
                randomDirection = DirectionalInput.NONE
            })
        }
    }

    private object CustomMode : Choice("Custom") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes


        private object Rotate : ToggleableConfigurable(ModuleAntiAFK, "Rotate", true) {
            val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
            val rotationsConfigurable = tree(RotationsConfigurable(this))
            val delay by int("Delay", 5, 0..20, "ticks")
            val angle by float("Angle", 1f, -180f..180f)
        }

        private object Swing : ToggleableConfigurable(ModuleAntiAFK, "Swing", true) {
            val delay by int("Delay", 5, 0..20, "ticks")
        }

        init {
            tree(Rotate)
            tree(Swing)
        }

        val jump by boolean("Jump", true)
        val move by boolean("Move", true)

        @Suppress("unused")
        val swingRepeatable = tickHandler {
            if (Swing.enabled && !player.handSwinging) {
                waitTicks(Swing.delay)
                player.swingHand(Hand.MAIN_HAND)
            }
        }

        @Suppress("unused")
        val repeatable = tickHandler {
            if (move) {
                mc.options.forwardKey.isPressed = true
            }

            if (jump && player.isOnGround) {
                waitNext<MovementInputEvent> { event ->
                    event.jump = true
                }
            }

            if (Rotate.enabled) {
                waitTicks(Rotate.delay)
                val currentRotation = RotationManager.serverRotation
                val pitchRandomization = Random.nextDouble(-5.0, 5.0).toFloat()
                RotationManager.setRotationTarget(
                    Rotation(
                        currentRotation.yaw + angle, (currentRotation.pitch + pitchRandomization).coerceIn(-90f, 90f)
                    ), ignoreOpenInventory, rotationsConfigurable, Priority.IMPORTANT_FOR_USAGE_1, ModuleAntiAFK
                )
            }

        }
    }
}
