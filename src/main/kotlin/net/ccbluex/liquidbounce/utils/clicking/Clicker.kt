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

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.clicking.pattern.ClickPattern
import net.ccbluex.liquidbounce.utils.clicking.pattern.patterns.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.option.KeyBinding
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
open class Clicker<T>(
    val parent: T,
    val keyBinding: KeyBinding,

    showCooldown: Boolean,
    maxCps: Int = 60,
    name: String = "Clicker"
) : Configurable(name, aliases = arrayOf("ClickScheduler")), EventListener where T : EventListener {

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

    private val itemCooldown: ItemCooldown<T>? = if (showCooldown) {
        tree(ItemCooldown(parent))
    } else {
        null
    }

    /**
     * When missing a hit, Minecraft has a cooldown before you can attack again.
     * This option will consider the cooldown before attacking again.
     *
     * This is useful for anti-cheats that detect if you are ignoring this cooldown.
     * Applies to the FailSwing feature as well.
     */
    private val attackCooldown: Value<Boolean>? = if (keyBinding == mc.options.attackKey) {
        boolean("AttackCooldown", true)
    } else {
        null
    }

    private val passesAttackCooldown
        get() = !(attackCooldown?.get() == true && mc.attackCooldown > 0)

    private val clickArray = RollingClickArray(DEFAULT_CYCLE_LENGTH, 2)

    init {
        fill()
    }

    // Clicks that were executed by [click] in the current tick
    var clickAmount: Int? = null
        private set

    val isClickTick: Boolean
        get() = willClickAt(0)

    fun willClickAt(tick: Int = 1) = getClickAmount(tick) > 0

    fun getClickAmount(tick: Int = 0): Int {
        if (isEnforcedClick()) {
            return 1
        }

        if (itemCooldown?.isCooldownPassed(tick) == false) {
            return 0
        }

        return clickArray.get(tick)
    }

    private fun isEnforcedClick(tick: Int = 0): Boolean {
        // Check if our last click is over 1000ms ago,
        if (lastClickPassed + (tick * 50L) >= 1000L) {
            return true
        }

        // Our cooldown is over, we want to click now!
        if (itemCooldown?.enabled == true && itemCooldown.isCooldownPassed(tick)) {
            return true
        }

        // Otherwise, follow our pattern
        return false
    }

    @Suppress("unused")
    private val keybindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        val clickAmount = this.clickAmount ?: return@handler

        // It turns out, we only want to do this with [attackKey], otherwise
        // [useKey] will do unexpected things.
        if (keyBinding == mc.options.attackKey && event.keyBinding == keyBinding) {
            // We want to simulate the click in order to
            // allow the game to handle the logic as if we clicked
            event.isPressed = clickAmount > 0
        }
    }

    /**
     * Clicks [cps] times per call (tick). If the cooldown is not passed, it will not click.
     * [block] should return true if the click was successful. Otherwise, it will not count as a click.
     */
    fun click(block: () -> Boolean) {
        val clicks = getClickAmount()

        debugParameter("Current Clicks") { clicks }
        debugParameter("Peek Clicks") { clickArray.get(1) }
        debugParameter("Last Click Passed") { lastClickPassed }
        debugParameter("Attack Cooldown") { mc.attackCooldown }
        debugParameter("Item Cooldown") { itemCooldown?.cooldownProgress() ?: 0.0f }

        var clickAmount = 0

        repeat(clicks) {
            if (!passesAttackCooldown) {
                return@repeat
            }

            if (itemCooldown?.isCooldownPassed() != false && block()) {
                clickAmount++
                itemCooldown?.newCooldown()
                lastClickTime = System.currentTimeMillis()
            }
        }

        this.clickAmount = clickAmount
    }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) {
        clickAmount = null

        if (clickArray.advance()) {
            val cycleArray = IntArray(DEFAULT_CYCLE_LENGTH)
            pattern.pattern.fill(cycleArray, cps, this)
            clickArray.push(cycleArray)
        }

        debugParameter("Click Technique") { pattern.choiceName }
        debugParameter("Click Array") {
            clickArray.array.withIndex().joinToString { (i, v) ->
                if (i == clickArray.head) "*$v" else v.toString()
            }
        }
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
        STABILIZED("Stabilized", StabilizedPattern),
        EFFICIENT("Efficient", EfficientPattern),
        SPAMMING("Spamming", SpammingPattern),
        DOUBLE_CLICK("DoubleClick", DoubleClickPattern),
        DRAG("Drag", DragPattern),
        BUTTERFLY("Butterfly", ButterflyPattern),
        NORMAL_DISTRIBUTION("NormalDistribution", NormalDistributionPattern);
    }

}
