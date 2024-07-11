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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.random.Random
import kotlin.random.nextInt

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
open class ClickScheduler<T>(val parent: T, showCooldown: Boolean, maxCps: Int = 60, name: String = "ClickScheduler")
    : Configurable(name), Listenable where T : Listenable {

    companion object {
        val RNG = java.util.Random()
        var lastClickTime = 0L
    }

    private val cps by intRange("CPS", 5..8, 1..maxCps, "clicks")
        .onChanged {
            newClickCycle()
        }
    private val clickTechnique by enumChoice("Technique", ClickTechnique.STABILIZED)

    class Cooldown<T>(module: T) : ToggleableConfigurable(module, "Cooldown", true)
        where T: Listenable {

        val rangeCooldown by floatRange("Timing", 1.0f..1.0f, 0.1f..1f)

        private var nextCooldown = rangeCooldown.random()

        fun readyToAttack(ticks: Int = 0) = !this.enabled || cooldownProgress(ticks) >= nextCooldown

        fun cooldownProgress(ticks: Int = 0) = player.getAttackCooldownProgress(ticks.toFloat())

        /**
         * Generates a new cooldown based on the range that was set by the user.
         */
        fun newCooldown() {
            nextCooldown = rangeCooldown.random()
        }

    }

    private var clickCycle: ClickCycle? = null
        set(value) {
            pastClickCycle = field
            field = value
        }
    private var pastClickCycle: ClickCycle? = null
        get() = field ?: clickCycle

    private val lastClickPassed
        get() = System.currentTimeMillis() - lastClickTime

    val cooldown: Cooldown<T>? = if (showCooldown) {
        tree(Cooldown(parent))
    } else {
        null
    }

    val goingToClick: Boolean
        get() = isClickOnNextTick(0)

    /**
     * Calculates if the next click is on the next tick or not.
     * Allows to predict future actions and behave accordingly.
     */
    fun isClickOnNextTick(ticks: Int = 1) = cooldown?.readyToAttack(ticks) != false
        && (clickCycle ?: newClickCycle()).clicksAt(ticks, isOvertime(ticks)) > 0

    private fun isOvertime(ticks: Int = 0) = lastClickPassed + (ticks * 50L) > 1000L ||
        (cooldown?.enabled == true && cooldown.readyToAttack(ticks))

    fun clicks(click: () -> Boolean) {
        val clicks = clickCycle?.clicksAt(isOvertime = isOvertime()) ?: return

        clickCycle?.let { cycle ->
            ModuleDebug.debugParameter(this, "Click Cycle Index", cycle.index)
            ModuleDebug.debugParameter(this, "Click Cycle Length", cycle.clickArray.size)
            ModuleDebug.debugParameter(this, "Click Cycle Sum", cycle.clickArray.sum())
            ModuleDebug.debugParameter(this, "Click Cycle Finished", cycle.isFinished())
            ModuleDebug.debugParameter(this, "Click Cycle Clicks", clicks)
        }

        if (clicks > 0) {
            repeat(clicks) {
                if (cooldown?.readyToAttack() != false && click()) {
                    cooldown?.newCooldown()

                    ModuleDebug.debugParameter(this, "Last Click Passed", lastClickPassed)
                    lastClickTime = System.currentTimeMillis()
                }
            }
        }
    }

    /**
     * Generates a new click cycle based on the current CPS.
     *
     * @see [clickCycle]
     */
    private fun newClickCycle(): ClickCycle {
        return clickTechnique.generate(cps, this).apply {
            clickCycle = this
            ModuleDebug.debugParameter(this@ClickScheduler, "Click Technique", clickTechnique.choiceName)
            ModuleDebug.debugParameter(this@ClickScheduler, "Click Cycle Length", clickArray.size)
            ModuleDebug.debugParameter(this@ClickScheduler, "Click Array", clickArray.joinToString())
        }
    }

    val gameHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        clickCycle?.next()

        if (clickCycle == null || clickCycle?.isFinished() == true) {
            clickCycle = newClickCycle()
        }
    }

    override fun parent() = parent

    /**
     * A click cycle is 20 ticks long, which is the length of a second.
     */
    data class ClickCycle(var index: Int, val clickArray: Array<Int>, val totalClicks: Int) {

        fun next() = index++

        fun isFinished() = index >= clickArray.size

        fun clicksAt(future: Int = 0, isOvertime: Boolean): Int {
            // dirty fix to get immediate clicks
            if (isOvertime) {
                val possibleEntry = clickArray.filter { it > 0 }

                // If there are no clicks in the array, return 1
                if (possibleEntry.isEmpty()) {
                    return 1
                }
                return possibleEntry.random()
            }

            return clickArray.getOrNull(index + future) ?: 0
        }

    }

    enum class ClickTechnique(
        override val choiceName: String,
        val generate: (IntRange, ClickScheduler<*>) -> ClickCycle,
        val legitimate: Boolean = true,
    ) : NamedChoice {

        /**
         * Normal clicking but with a stabilized click cycle.
         */
        STABILIZED("Stabilized", { cps, _ ->
            val clicks = cps.random()
            val clickArray = Array(20) { 0 }

            // Calculate the interval and distribute the remainder to spread evenly
            val interval = clickArray.size / clicks
            var remainder = clickArray.size % clicks

            var currentIndex = 0

            @Suppress("UnusedPrivateProperty")
            for (i in 0 until clicks) {
                if (remainder > 0) {
                    clickArray[currentIndex]++
                    currentIndex += interval
                    currentIndex++
                    remainder--
                    continue
                }

                // Choose random index
                val indexWithLeastClicks = clickArray.withIndex().shuffled().minByOrNull { it.value }?.index
                    ?: clickArray.indices.random()
                clickArray[indexWithLeastClicks]++
            }

            // Return the click cycle
            ClickCycle(0, clickArray, clicks)
        }),

        /**
         * Normal clicking is the most common clicking method and usually
         * results in a CPS of 5-8 and sometimes when aggressive 10-12.
         *
         *
         *
         * It is when clicking normally with your finger.
         *
         * @note I was not able to press faster than 8 CPS. @1zuna
         */
        SPAMMING("Spamming", { cps, _ ->
            val clicks = cps.random()

            // Generate a random click array lasting 20 ticks
            // Make sure to support more than 20 clicks
            val clickArray = Array(20) { 0 }

            repeat(clicks) {
                // Increase random index inside click array by 1
                clickArray.indices.random().let { index ->
                    clickArray[index]++
                }
            }

            // Return the click cycle
            ClickCycle(0, clickArray, clicks)
        }),

        /**
         * Double clicking is NOT a method but a button on a few cheater mouses.
         * This button is called the FIRE button and will result in two clicks when pressed once.
         *
         * This is a method that is not allowed on most servers and is considered cheating.
         * Unlikely to bypass and will result in twice the CPS (!!!).
         *
         * @note In the past I had a mouse with this feature and I always used it. @1zuna
         */
        DOUBLE_CLICK("DoubleClick", { cps, _ ->
            val clicks = cps.random()

            // Generate a random click array lasting 20 ticks
            // Make sure to support more than 20 clicks
            val clickArray = Array(20) { 0 }

            repeat(clicks) {
                // Increase random index inside click array by 1
                clickArray.indices.random().let { index ->
                    clickArray[index] += 2
                }
            }

            // Return the click cycle
            ClickCycle(0, clickArray, clicks)
        }, legitimate = false),

        /**
         * Drag clicking is a method that is used to bypass the CPS limit of 20.
         *
         * It can be done by gliding your finger over the mouse button and causing friction
         * to click very fast.
         *
         * Is is not very easy to do as it requires a lot of practice and a good mouse,
         * as well as a good grip on the mouse. Sweaty hands are a big no-no.
         *
         * This is very hard to implement as I am not able to do this method myself
         * so I will simply guess how it works.
         */
        DRAG("Drag", { cps, _ ->
            val clicks = cps.random()

            /**
             * The travel time is the time it takes to move the finger
             * from the top of the mouse to the bottom.
             *
             * After this travel time we need to move the finger back to the top and cannot click.
             * This is more consistent usually.
             *
             * TODO: Implement option to set travel time
             *  and travel return time.
             */
            val travelTime = Random.nextInt(7..12)
            val travelReturnTime = Random.nextInt(2..4)

            val clickArray = Array(travelTime + travelReturnTime) { 0 }

            // Fit the clicks into the travel time of the
            while (clickArray.sum() < clicks) {
                // Fill the travel time area in the click array with clicks

                // Get index with the lowest clicks on the click array
                val index = clickArray.copyOf(travelTime).indices.minByOrNull { clickArray[it] }!!

                // Increase the click count at the index
                clickArray[index]++
            }

            // Return the click cycle
            ClickCycle(0, clickArray, clicks)
        }),

        /**
         * Butterfly clicking is a method that is used to bypass the CPS limit of 20.
         *
         * It will often result in double click (very similar to the double click technique - but randomized).
         *
         */
        BUTTERFLY("Butterfly", { cps, _ ->
            // Generate a random click array lasting 120 ticks
            val clickArray = Array(20) { 0 }
            val clicks = cps.random()

            while (clickArray.sum() < clicks) {
                // Increase random index inside click array by 1
                val indices = clickArray.withIndex()
                    .filter { (_, c) -> c == 0 }

                if (indices.isNotEmpty()) {
                    // Increase a random index which is not yet clicked
                    indices.random().let { (index, _) ->
                        clickArray[index] = Random.nextInt(1..2)
                    }
                } else {
                    // Randomly increase an index
                    clickArray.indices.random().let { index ->
                        clickArray[index]++
                    }
                }
            }

            // Return the click cycle
            ClickCycle(0, clickArray, clicks)
        }),
        NORMAL_DISTRIBUTION("NormalDistribution", { cps, scheduler ->
            data class Band(val top: Double, val mean: Double, val std: Double)

            val frequencyBands = arrayOf(
                Band(10.0 / 110.0, 179.5242718446602, 20.416937885616676),
                Band(0.0, 87.88, 13.420088130563776)
            )

            var t = 0.0

            val clickArray = Array(20) { 0 }

            while (true) {
                val v = RNG.nextDouble()

                val band = frequencyBands.first { v >= it.top }

                t += RNG.nextGaussian(band.mean, band.std) * 20.0 / 1000.0

                // Second is over
                if (t > 20.0) {
                    break
                }

                clickArray[t.toInt()]++
            }

            ClickCycle(0, clickArray, clickArray.sum())
        }),

        /**
         * Random switching between normal, butterfly and abuse clicking.
         */
        RANDOM("Random", { cps, scheduler ->
            entries.filter { it.legitimate }.random().generate(cps, scheduler)
        });

    }

}
