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

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.util.Util

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
 * Presses are planned in milliseconds by a [ClickTiming] and batched into ticks by a [ClickPlan],
 * so we can predict future clicks and behave accordingly.
 */
open class Clicker<T>(
    val parent: T,
    val keyBinding: KeyMapping,
    val itemCooldown: ItemCooldown? = ItemCooldown(),
    maxCps: Int = 30,
    name: String = "Clicker",
    simulateAttackKeyDown: Boolean = false,
) : ValueGroup(name, aliases = listOf("ClickScheduler")), EventListener where T : EventListener {

    companion object {
        private const val TICKS_AHEAD = 20
    }

    private val technique by enumChoice("Technique", ClickTechnique.HUMAN)
    private val cps by intRange("CPS", 11..14, 1..maxCps, "clicks")
    private val maxPerTick by int("MaxPerTick", 2, 1..5, "clicks")

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
        boolean("MissCooldown", true, aliases = listOf("AttackCooldown"))
    } else {
        null
    }

    private val passesMissCooldown
        get() = !(missCooldown?.get() == true && mc.missTime > 0)

    private val human = HumanClickTiming()

    private val plan = ClickPlan({ recent, comboMs, cps, random ->
        when (technique) {
            ClickTechnique.HUMAN -> human
            ClickTechnique.CONSTANT -> ConstantClickTiming
        }.nextInterval(recent, comboMs, cps, random)
    }).apply {
        // Once, on the tick the cooldown fills up; one that is always ready would otherwise click every tick
        enforced = { tick ->
            val cooldown = itemCooldown
            player.hasCooldown && cooldown != null &&
                cooldown.isCooldownPassed(tick) && !cooldown.isCooldownPassed(tick - 1)
        }
    }

    // Clicks that were executed by [click] in the current tick
    var clickAmount: Int? = null
        private set

    open val isClickTick: Boolean
        get() = willClickAt(0)

    val ticksUntilClick: Int
        get() = (0 until TICKS_AHEAD).firstOrNull(::willClickAt) ?: TICKS_AHEAD

    var ticksSinceLastClick = 0
        private set

    fun willClickAt(tick: Int = 1) = getClickAmount(tick) > 0

    /**
     * Presses consumed by the tick [tick] ticks from now.
     */
    fun getClickAmount(tick: Int = 0) = plan.clicksAt(tick)

    init {
        if (simulateAttackKeyDown && keyBinding == mc.options.keyAttack) {
            handler<KeybindIsPressedEvent> { event ->
                val clickAmount = this.clickAmount ?: return@handler

                // It turns out, we only want to do this with [attackKey], otherwise
                // [useKey] will do unexpected things.
                if (event.keyBinding == keyBinding) {
                    // We want to simulate the click in order to
                    // allow the game to handle the logic as if we clicked
                    event.isPressed = clickAmount > 0
                }
            }
        }
    }

    /**
     * Uses the presses of this tick. If the cooldown is not passed, the press is dropped.
     * [block] should return true if the click was successful. Otherwise, it will not count as a click.
     */
    fun click(block: () -> Boolean) {
        debugParameter("Current Clicks") { getClickAmount() }
        debugParameter("Peek Clicks") { getClickAmount(1) }
        debugParameter("Combo") { plan.comboMs }
        debugParameter("Miss Cooldown") { mc.missTime }
        debugParameter("Item Cooldown") { itemCooldown?.cooldownProgress() ?: 0.0f }

        val clicks = plan.consume({ passesMissCooldown && itemCooldown?.isCooldownPassed() != false }) {
            block().also { success ->
                if (success) {
                    itemCooldown?.newCooldown()
                    ticksSinceLastClick = 0
                }
            }
        }

        this.clickAmount = (this.clickAmount ?: 0) + clicks
    }

    /**
     * Returns true when a click attempt can be executed right now.
     * This uses the same gating logic as [click] before invoking [block].
     */
    fun canExecuteClickNow(): Boolean {
        if (getClickAmount() <= 0) {
            return false
        }

        if (!passesMissCooldown) {
            return false
        }

        return itemCooldown?.isCooldownPassed() != false
    }

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) {
        ticksSinceLastClick++
        clickAmount = null

        plan.cps = cps
        plan.maxPerTick = maxPerTick
        plan.tick(Util.getMillis())
    }

    override fun parent() = parent

}
