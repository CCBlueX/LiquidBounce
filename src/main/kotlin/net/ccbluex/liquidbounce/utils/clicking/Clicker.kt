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
package net.ccbluex.liquidbounce.utils.clicking

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.clicking.pattern.ClickPattern
import net.ccbluex.liquidbounce.utils.clicking.pattern.patterns.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import java.util.*

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
open class Clicker<T>(val parent: T, showCooldown: Boolean, maxCps: Int = 60, name: String = "Clicker")
    : Configurable(name, aliases = arrayOf("ClickScheduler")), EventListener where T : EventListener {

    companion object {
        internal val RNG = Random()
        private const val DEFAULT_CYCLE_LENGTH = 20
        private var lastClickTime = 0L
        private val lastClickPassed
            get() = System.currentTimeMillis() - lastClickTime
    }

    // Options
    private val cps by intRange("CPS", 5..8, 1..maxCps, "clicks")
        .onChanged {
            fill()
        }
    private val pattern by enumChoice("Technique", ClickPatterns.STABILIZED)
        .onChanged {
            fill()
        }
    val cooldown: ClickCooldown<T>? = if (showCooldown) {
        tree(ClickCooldown(parent))
    } else {
        null
    }

    private val clickArray = RollingClickArray(DEFAULT_CYCLE_LENGTH, 2)

    val isGoingToClick: Boolean
        get() = isClickOnNextTick(0)

    init {
        fill()
    }

    /**
     * Calculates if the next click is on the next tick or not.
     * Allows to predict future actions and behave accordingly.
     */
    fun isClickOnNextTick(ticks: Int = 1): Boolean {
        if (cooldown?.readyToAttack(ticks) == false) {
            return false
        }

        return clickArray.get(ticks) > 0 || isOvertime(ticks)
    }

    private fun isOvertime(ticks: Int = 0) = lastClickPassed + (ticks * 50L) >= 1000L ||
        (cooldown?.enabled == true && cooldown.readyToAttack(ticks))

    fun clicks(click: () -> Boolean) {
        val clicks = if (isOvertime()) {
            clickArray.nextCycleList().maxBy { it }.coerceAtLeast(1)
        } else {
            clickArray.get(0)
        }

        ModuleDebug.apply {
            debugParameter(this@Clicker, "Current Clicks", clicks)
            debugParameter(this@Clicker, "Peek Clicks", clickArray.get(1))
            debugParameter(this@Clicker, "Last Click Passed", lastClickPassed)
        }

        if (clicks > 0) {
            repeat(clicks) {
                if (cooldown?.readyToAttack() != false && click()) {
                    cooldown?.newCooldown()
                    lastClickTime = System.currentTimeMillis()
                }
            }
        }
    }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) {
        if (clickArray.advance()) {
            val cycleArray = IntArray(DEFAULT_CYCLE_LENGTH)
            pattern.pattern.fill(cycleArray, cps, this)
            clickArray.push(cycleArray)
        }

        ModuleDebug.debugParameter(this@Clicker, "Click Technique", pattern.choiceName)
        ModuleDebug.debugParameter(
            this@Clicker,
            "Click Array",
            clickArray.array.withIndex().joinToString { (i, v) ->
                if (i == clickArray.head) "*$v" else v.toString()
            }
        )
    }

    private fun fill() {
        clickArray.clear()
        val cycleArray = IntArray(DEFAULT_CYCLE_LENGTH)
        repeat(clickArray.iterations) {
            Arrays.fill(cycleArray, 0)
            pattern.pattern.fill(cycleArray, cps, this)
            clickArray.push(cycleArray)
            clickArray.advance(DEFAULT_CYCLE_LENGTH)
        }
    }

    override fun parent() = parent

    @Suppress("unused")
    enum class ClickPatterns(
        override val choiceName: String,
        val pattern: ClickPattern
    ) : NamedChoice {
        STABILIZED("Stabilized", StabilizedPattern()),
        EFFICIENT("Efficient", EfficientPattern()),
        SPAMMING("Spamming", SpammingPattern()),
        DOUBLE_CLICK("DoubleClick", DoubleClickPattern()),
        DRAG("Drag", DragPattern()),
        BUTTERFLY("Butterfly", ButterflyPattern()),
        NORMAL_DISTRIBUTION("NormalDistribution", NormalDistributionPattern());
    }

}
