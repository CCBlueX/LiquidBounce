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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Timer module
 *
 * Changes the speed of the entire game.
 */
object ModuleTimer : ClientModule("Timer", Category.WORLD, disableOnQuit = true) {

    val modes = choices("Mode", Classic, arrayOf(Classic, Pulse, Boost)).apply { tagBy(this) }

    object Classic : Choice("Classic") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val speed by float("Speed", 2f, 0.1f..10f)

        val repeatable = tickHandler {
            Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)
        }

    }

    object Pulse : Choice("Pulse") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val normalSpeed: Float by float("NormalSpeed", 0.5f, 0.1f..10f)
        private val normalSpeedTicks by int("NormalSpeedTicks", 20, 1..500, "ticks")
        private val boostSpeed by float("BoostSpeed", 2f, 0.1f..10f)
        private val boostSpeedTicks by int("BoostSpeedTicks", 20, 1..500, "ticks")
        private val onMove by boolean("OnMove", false)
        private var currentState: TimerState = TimerState.NORMAL_SPEED

        override fun enable() {
            currentState = TimerState.NORMAL_SPEED
        }

        val repeatable = tickHandler {
            if (onMove && !ModuleTimer.player.moving) {
                return@tickHandler
            }

            val (nextState, currentSpeed, expirationTicks) = when (currentState) {
                TimerState.NORMAL_SPEED -> Triple(TimerState.BOOST_SPEED, normalSpeed, normalSpeedTicks)
                TimerState.BOOST_SPEED -> Triple(TimerState.NORMAL_SPEED, boostSpeed, boostSpeedTicks)
            }

            currentState = nextState

            Timer.requestTimerSpeed(
                timerSpeed = currentSpeed,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = ModuleTimer,
                resetAfterTicks = expirationTicks
            )

            waitTicks(expirationTicks)

            return@tickHandler
        }

        enum class TimerState {
            NORMAL_SPEED, BOOST_SPEED
        }

    }

    object Boost : Choice("Boost") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val boostSpeed by float("BoostSpeed", 1.3f, 0.1f..10f)
        private val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..10f)

        private val timeBoostTicks by int("TimeBoostTicks", 12, 1..60, "ticks")
        private var boostCapable = 0

        // basically timer balance
        private val accountTimerValue by boolean("AccountTimerValues", true)

        private val normalizeDuringCombat by boolean("NormalizeDuringCombat", true)
        private val allowNegative by boolean("AllowNegative", false)

        val repeatable = tickHandler {
            if (normalizeDuringCombat && CombatManager.isInCombat) {
                Timer.requestTimerSpeed(1f, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)
                return@tickHandler
            }

            if (boostCapable < 0) {
                val ticks = abs(boostCapable)
                Timer.requestTimerSpeed(
                    slowSpeed,
                    Priority.IMPORTANT_FOR_USAGE_1,
                    ModuleTimer,
                    resetAfterTicks = ticks
                )

                notification(
                    "Timer", "Slowing down for $ticks ticks",
                    NotificationEvent.Severity.INFO
                )
                boostCapable = 0
                waitTicks(ticks)
            }

            if (!player.moving) {
                if (mc.currentScreen is InventoryScreen || mc.currentScreen is GenericContainerScreen) {
                    boostCapable = 0
                    return@tickHandler
                }

                Timer.requestTimerSpeed(slowSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)

                val addition = if (accountTimerValue) (1 / slowSpeed).toInt() else 1
                boostCapable = (boostCapable + addition).toInt().coerceAtMost(timeBoostTicks)
            } else {
                val speedUp = boostCapable > 0 ||
                        (allowNegative && (CombatManager.isInCombat || ModuleScaffold.running))

                if (!speedUp) {
                    return@tickHandler
                }

                val ticks = if (boostCapable > 0) boostCapable else timeBoostTicks
                val speedUpTicks = if (accountTimerValue) ceil(ticks / boostSpeed).toInt() else ticks

                if (speedUpTicks == 0) {
                    return@tickHandler
                }

                Timer.requestTimerSpeed(
                    boostSpeed,
                    Priority.IMPORTANT_FOR_USAGE_1,
                    ModuleTimer,
                    resetAfterTicks = speedUpTicks
                )
                notification(
                    "Timer", "Boosted for $speedUpTicks ticks",
                    NotificationEvent.Severity.INFO
                )
                boostCapable -= ticks
                waitTicks(speedUpTicks)
            }
        }

    }

    override fun onDisabled() {
        Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, this@ModuleTimer)
    }

}
