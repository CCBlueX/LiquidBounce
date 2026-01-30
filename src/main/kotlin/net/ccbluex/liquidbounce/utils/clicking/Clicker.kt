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
package net.ccbluex.liquidbounce.utils.clicking

import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.vector2f
import net.ccbluex.liquidbounce.utils.input.InputTracker
import net.ccbluex.liquidbounce.utils.input.InputTracker.timeSinceComboStart
import net.ccbluex.liquidbounce.utils.input.InputTracker.timeSinceLastPress
import net.ccbluex.liquidbounce.utils.input.InputTracker.updateInputPress
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

/**
 * An attack scheduler
 *
 * Minecraft is counting every click until it handles all inputs.
 * code:
 * while (this.options.keyAttack.wasPressed()) {
 *     this.doAttack();
 * }
 * @see [Minecraft.handleKeybinds]
 *
 * We are simulating this behaviour by calculating how many times we could have been clicked in the meantime of a tick.
 * This allows us to predict future actions and behave accordingly.
 */
open class Clicker<T>(
    val parent: T,
    val keyBinding: KeyMapping,
    val itemCooldown: ItemCooldown? = ItemCooldown(),
    maxCps: Int = 60,
    name: String = "Clicker"
) : ValueGroup(name), EventListener where T : EventListener {

    companion object {
        private const val TICK_DURATION_MS = 50L
        private const val TICKS_IN_A_SECOND = 20
        private const val DEFAULT_CYCLE_LENGTH_MS = (TICKS_IN_A_SECOND * TICK_DURATION_MS).toInt()
        private const val DEFAULT_CURVE_WINDOW_SECONDS = 10f
    }

    private val cps: IntRange by intRange("CPS", 11..14, 1..maxCps, "clicks").onChanged {
        currentCps = cps.random()
        nextClickDelayMs = sampleIntervalMs()
    }

    private val fatigue = curve(
        "Fatigue",
        mutableListOf(
            0f vector2f 2f,
            DEFAULT_CURVE_WINDOW_SECONDS / 2 vector2f -2f,
            DEFAULT_CURVE_WINDOW_SECONDS vector2f 1f,
        ),
        xAxis = "Seconds" axis 0f..DEFAULT_CURVE_WINDOW_SECONDS,
        yAxis = "CPS" axis -5f..5f,
    ).apply {
        onChanged {
            nextClickDelayMs = sampleIntervalMs()
        }
    }

    /**
     * When we break the combo, we reset our fatigue. If set to 0, we stay in the combo indefinitely.
     */
    private val breakCombo by intRange("BreakCombo", 0..0, 0..20, "ticks")

    private var pauseTicks = 0

    init {
        itemCooldown?.let(this::tree)
    }

    /**
     * When missing a hit, Minecraft has a cooldown before you can attack again.
     * This option will consider the cooldown before attacking again.
     *
     * This is useful for anti-cheats that detect if you are ignoring this cooldown.
     * Applies to the FailSwing feature as well.
     */
    private val missCooldown: Value<Boolean>? = if (keyBinding == mc.options.keyAttack) {
        boolean("MissCooldown", true)
    } else {
        null
    }

    private val passesMissCooldown
        get() = !(missCooldown?.get() == true && mc.missTime > 0)

    /**
     * With each combo, we start with a random CPS value.
     * This allows us to have a different CPS value for each combo.
     */
    private var currentCps = cps.random()
    private var nextClickDelayMs = sampleIntervalMs()

    // Clicks that were executed by [click] in the current tick
    var clickAmount: Int? = null
        private set

    // todo: find better name
    val isClickTick: Boolean
        get() = timeUntilNextClickMs(0) <= 0

    // todo: find better name
    fun willClickAt(tick: Int = 1) = timeUntilNextClickMs(tick) <= 0

    /**
     * Clicks on a curve-driven schedule per tick. If the cooldown is not passed, it will not click.
     * [block] should return true if the click was successful. Otherwise, it will not count as a click.
     */
    fun click(block: () -> Boolean): Boolean {
        debugParameter("Current CPS") { currentCps }
        debugParameter("Time Since Last Press") { keyBinding.timeSinceLastPress }
        debugParameter("Time Since Combo Start") { keyBinding.timeSinceComboStart }
        debugParameter("Time Until Next Click") { timeUntilNextClickMs(1) }
        debugParameter("Miss Cooldown") { mc.missTime }
        debugParameter("Item Cooldown") { itemCooldown?.cooldownProgress() ?: 0.0f }

        var clicks = 0
        // todo: make double clicking work
        while (timeUntilNextClickMs(1) <= 0) {
            if (!passesMissCooldown) {
                break
            }

            if (itemCooldown?.isCooldownPassed() == false) {
                break
            }

            if (block()) {
                itemCooldown?.newCooldown()
                updateInputPress(keyBinding.key)
                nextClickDelayMs = sampleIntervalMs()
                clicks++
            } else {
                break
            }
        }

        this.clickAmount = clicks
        debugParameter("Next Click Delay") { nextClickDelayMs }
        debugParameter("Current Clicks") { clicks }

        return clicks > 0
    }

    @Suppress("unused")
    private val keybindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        val clickAmount = this.clickAmount ?: return@handler

        // It turns out we only want to do this with [attackKey]; otherwise
        // [useKey] will do unexpected things.
        if (keyBinding == mc.options.keyAttack && event.keyBinding == keyBinding) {
            // We want to simulate the click to allow the game to handle the logic as if we clicked.
            event.isPressed = clickAmount > 0
        }
    }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        clickAmount = null

        if (pauseTicks > 0) {
            pauseTicks--
        }
    }

    override fun parent() = parent

    private fun timeUntilNextClickMs(tickOffset: Int): Long {
        val timeSince = timeSinceLastClickMs()
        val offsetMs = tickOffset * TICK_DURATION_MS
        val baseRemainingMs = nextClickDelayMs.toLong() - (timeSince + offsetMs)
        val pauseRemainingMs = (pauseTicks - tickOffset).coerceAtLeast(0) * TICK_DURATION_MS
        return if (pauseRemainingMs > 0) pauseRemainingMs else baseRemainingMs
    }

    private fun timeSinceLastClickMs(): Long {
        val timeSince = keyBinding.timeSinceLastPress
        return if (timeSince == Long.MAX_VALUE) DEFAULT_CYCLE_LENGTH_MS.toLong() else timeSince
    }

    private fun sampleIntervalMs(): Int {
        var comboMs = keyBinding.timeSinceComboStart
        if (comboMs == Long.MAX_VALUE) {
            currentCps = cps.random()
            comboMs = 0L
        }

        val maxComboSeconds = fatigue.xAxis.range.endInclusive
        val elapsedSeconds = comboMs / 1000f

        // If we exceeded the max combo, we break the combo and reset fatigue.
        if (elapsedSeconds > maxComboSeconds) {
            pauseTicks = breakCombo.random()
            if (pauseTicks > 0) {
                InputTracker.resetCombo(keyBinding.key)
                currentCps = cps.random()
            }
        }

        val cpsValue = (currentCps + fatigue.transform(elapsedSeconds.coerceIn(0f, maxComboSeconds)))
            .coerceAtLeast(1f)
        val intervalMs = 1000f / cpsValue
        return intervalMs.roundToInt().coerceIn(1, DEFAULT_CYCLE_LENGTH_MS)
    }

}
