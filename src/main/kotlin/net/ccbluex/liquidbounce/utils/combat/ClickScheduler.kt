/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.roundToInt

/**
 * An attack scheduler
 *
 * Minecraft is counting every click until it handles all inputs.
 * code:
 * while (this.options.keyAttack.wasPressed()) {
 *     this.doAttack();
 * }
 * @see [MinecraftClient.handleInputEvents]
 *
 * We are simulating this behaviour by calculating how many times we could have been clicked in the meantime of a tick.
 * This allows us to predict future actions and behave accordingly.
 */
class ClickScheduler<T>(val module: T, showCooldown: Boolean, maxCps: Int = 20, name: String = "ClickScheduler")
    : Configurable(name) where T : Module {

    private val cps by intRange("CPS", 5..8, 1..maxCps)

    class Cooldown<T>(module: T) : ToggleableConfigurable(module, "Cooldown", true)
        where T: Module {

        private val rangeCooldown by floatRange("CooldownRange", 0.9f..1f, 0f..1f)

        private var nextCooldown = rangeCooldown.random()

        fun readyToAttack(ticks: Int = 0) = !this.enabled || cooldownProgress(ticks) >= nextCooldown

        fun cooldownProgress(ticks: Int = 0) = player.getAttackCooldownProgress(
            player.attackCooldownProgressPerTick * ticks)

        /**
         * Generates a new cooldown based on the range that was set by the user.
         */
        fun newCooldown() {
            nextCooldown = rangeCooldown.random()
        }

    }

    companion object {
        /**
         * The usual time that a tick takes in Minecraft.
         */
        const val MINECRAFT_TIME_MS = 50L

        /**
         * The margin of error that is allowed for the click to pass as valid.
         */
        const val MARGIN_OF_ERROR = 20L

    }

    private data class ClickData(val previousClick: Long, val timeRequiredForClick: Long) {
        val timeSince: Long
            get() = System.currentTimeMillis() - previousClick
        val isWayTooLate: Boolean
            get() = ((timeSince - timeRequiredForClick) / 50.0).roundToInt() * 50 > 50
        val canClick: Boolean
            get() = canClickBasedOn(timeSince)

        fun canClickBasedOn(time: Long) = time >= timeRequiredForClick - MARGIN_OF_ERROR

    }

    /**
     * Contains the time when the last click was performed and when the next click is possible.
     */
    private var clickData = newClickData()
    private val cooldown: Cooldown<T>?

    val goingToClick: Boolean
        get() = isClickOnNextTick(0)

    init {
        cooldown = if (showCooldown) {
            tree(Cooldown(module))
        } else {
            null
        }
    }

    /**
     * Calculates if the next click is on the next tick or not.
     * Allows to predict future actions and behave accordingly.
     */
    fun isClickOnNextTick(ticks: Int = 1) = cooldown?.readyToAttack(ticks) != false && (clickData.isWayTooLate ||
        (clickData.timeSince + (MINECRAFT_TIME_MS * ticks)) >= clickData.timeRequiredForClick - MARGIN_OF_ERROR)

    fun clicks(click: () -> Boolean): Boolean {
        var timeLeft = clickData.timeSince

        ModuleDebug.debugParameter(module, "ClickScheduler->TimeSince", timeLeft)
        ModuleDebug.debugParameter(module, "ClickScheduler->TimeRequired",
            clickData.timeRequiredForClick.toString())
        ModuleDebug.debugParameter(module, "ClickScheduler->isWayTooLate", clickData.isWayTooLate)
        ModuleDebug.debugParameter(module, "ClickScheduler->PassingCooldown",
            cooldown?.readyToAttack() != false)

        // Does the clickTime need a forced update or are we a tick late?
        if (clickData.isWayTooLate) {
            clickData = newClickData()

            // If we are way too late, we should click once.
            return click()
        }

        var clicks = 0

        while (clickData.canClickBasedOn(timeLeft) && cooldown?.readyToAttack() != false) {
            if (!click()) {
                return false
            }

            timeLeft -= clickData.timeRequiredForClick
            clicks++

            clickData = newClickData()
            cooldown?.newCooldown()
        }

        ModuleDebug.debugParameter(module, "ClickScheduler->Clicks",
            clicks)

        return clicks > 0
    }

    /**
     * Generates the next click time based on the CPS.
     *
     * TODO: Add simulation for drag clicks
     */
    private fun newClickData() = ClickData(System.currentTimeMillis(), 1000L / cps.random())

}
