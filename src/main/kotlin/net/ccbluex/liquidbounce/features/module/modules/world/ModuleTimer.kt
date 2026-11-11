/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.session.sessionState
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.balanceReset
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * Timer module
 *
 * Changes the speed of the entire game.
 */
object ModuleTimer : ClientModule("Timer", ModuleCategories.WORLD, disableOnQuit = true) {

    val modes = choices("Mode", Classic, arrayOf(Classic, Pulse, Boost)).apply { tagBy(this) }

    object Classic : Mode("Classic") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val speed by float("Speed", 2f, 0.1f..20f)

        val repeatable = handler<GameTickEvent> {
            Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)
        }

    }

    object Pulse : Mode("Pulse") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val normalSpeed: Float by float("NormalSpeed", 0.5f, 0.1f..20f)
        private val normalSpeedTicks by int("NormalSpeedTicks", 20, 1..500, "ticks")
        private val boostSpeed by float("BoostSpeed", 2f, 0.1f..20f)
        private val boostSpeedTicks by int("BoostSpeedTicks", 20, 1..500, "ticks")
        private val onMove by boolean("OnMove", false)
        private var currentState: TimerState by sessionState { TimerState.NORMAL_SPEED }

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

    object Boost : Mode("Boost") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val boostSpeed by float("BoostSpeed", 1.3f, 0.1f..20f)
        private val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..20f)

        private val normalizeDuringCombat by boolean("NormalizeDuringCombat", true)

        /**
         * Acceleration may spend the balance down to the start, which is below 0 for anticheats that let the client
         * run ahead. Slowing down stops paying off at the end.
         */
        private val balance by intRange("Balance", 0..20, -100..200, "ticks")

        @Suppress("unused")
        private val balanceReset by sessionState { balanceReset() }

        val repeatable = handler<GameTickEvent> {
            if (normalizeDuringCombat && CombatManager.isInCombat) {
                Timer.requestTimerSpeed(1f, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)
                return@handler
            }

            val speed = when {
                player.moving -> boostSpeed
                mc.gui.screen() is InventoryScreen || mc.gui.screen() is ContainerScreen -> return@handler
                else -> slowSpeed
            }
            Timer.requestBalancedTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer, balance)
        }

    }

    override fun onDisabled() {
        Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, this@ModuleTimer)
    }

}
