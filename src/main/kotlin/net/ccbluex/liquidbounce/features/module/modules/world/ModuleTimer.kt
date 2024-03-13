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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import kotlin.math.abs

/**
 * Timer module
 *
 * Changes the speed of the entire game.
 */
object ModuleTimer : Module("Timer", Category.WORLD, disableOnQuit = true) {

    val modes = choices("Mode", Classic, arrayOf(Classic, Pulse, Boost))

    object Classic : Choice("Classic") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val speed by float("Speed", 2f, 0.1f..10f)

        val repeatable = repeatable {
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
        private var currentTimerState: TimerState = TimerState.NormalSpeed

        val repeatable = repeatable {
            if (onMove && !ModuleTimer.player.moving) {
                return@repeatable
            }

            when (currentTimerState) {
                TimerState.NormalSpeed -> {
                    Timer.requestTimerSpeed(
                        normalSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer, resetAfterTicks = normalSpeedTicks
                    )
                    waitTicks(normalSpeedTicks)
                    currentTimerState = TimerState.BoostSpeed
                }

                TimerState.BoostSpeed -> {
                    Timer.requestTimerSpeed(
                        boostSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer, resetAfterTicks = boostSpeedTicks
                    )
                    waitTicks(boostSpeedTicks)
                    currentTimerState = TimerState.NormalSpeed
                }
            }

            return@repeatable
        }

        override fun disable() {
            currentTimerState = TimerState.NormalSpeed
            super.disable()
        }

        enum class TimerState {
            NormalSpeed, BoostSpeed
        }

    }

    object Boost : Choice("Boost") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val boostSpeed by float("BoostSpeed", 1.3f, 0.1f..10f)
        private val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..10f)

        private val timeBoostTicks by int("TimeBoostTicks", 12, 1..60, "ticks")
        private var boostCapable = 0

        private val normalizeDuringCombat by boolean("NormalizeDuringCombat", true)
        private val allowNegative by boolean("AllowNegative", false)

        val repeatable = repeatable {
            if (normalizeDuringCombat && CombatManager.isInCombat()) {
                Timer.requestTimerSpeed(1f, Priority.IMPORTANT_FOR_USAGE_1, ModuleTimer)
                return@repeatable
            }

            if (boostCapable < 0) {
                val ticks = abs(boostCapable)
                Timer.requestTimerSpeed(
                    slowSpeed,
                    Priority.IMPORTANT_FOR_USAGE_1,
                    ModuleSpeed,
                    resetAfterTicks = ticks
                )

                notification("Timer", "Slowing down for $ticks ticks",
                    NotificationEvent.Severity.INFO)
                boostCapable = 0
                waitTicks(ticks)
            }

            if (!player.moving) {
                if (mc.currentScreen is InventoryScreen || mc.currentScreen is GenericContainerScreen) {
                    boostCapable = 0
                    return@repeatable
                }

                Timer.requestTimerSpeed(slowSpeed, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
                boostCapable = (boostCapable + 1).coerceAtMost(timeBoostTicks)
            }else {
                val speedUp = boostCapable > 0 ||
                    (allowNegative && (CombatManager.isInCombat() || ModuleScaffold.enabled))

                if (speedUp) {
                    val ticks = if (boostCapable > 0) boostCapable else timeBoostTicks

                    Timer.requestTimerSpeed(
                        boostSpeed,
                        Priority.IMPORTANT_FOR_USAGE_1,
                        ModuleSpeed,
                        resetAfterTicks = ticks
                    )
                    notification(
                        "Timer", "Boosted for $ticks ticks",
                        NotificationEvent.Severity.INFO
                    )
                    boostCapable -= ticks
                    waitTicks(ticks)
                }
            }

            return@repeatable
        }


    }

    override fun disable() {
        Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, this@ModuleTimer)
    }


}
