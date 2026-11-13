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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.Locale

/**
 * With ManageModules, Speed is on for the whole round, so walking, looting, evading, retreating and strafing all
 * go at Speed's pace. It is off outside a round, while a chest screen is open, while Scaffold or Fly moves the
 * player, and wherever the game holds it back, such as near void edges; it only comes back after [CALM_TICKS]
 * without a reason, so it does not flicker along an edge.
 *
 * Logs `speed on` and `speed off` lines.
 */
internal object Pace : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private var on = false
    private var calmTicks = 0
    private var lastReason: String? = null
    private var overshoots = 0

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        noteOvershoots()
        if (!ModuleGameBot.managesModules) {
            return@handler
        }

        val reason = holdReason()
        if (reason != null) {
            calmTicks = 0
            lastReason = reason
            if (on) {
                on = false
                report("speed off reason=$reason")
            }
        } else if (!on && ++calmTicks >= calmTicksAfter(lastReason)) {
            on = true
            report("speed on reason=clear")
        }

        if (ModuleSpeed.enabled != on) {
            ModuleSpeed.enabled = on
        }
    }

    /** A turn or the goal passes in a moment, so Speed comes back sooner than after a void edge. */
    private fun calmTicksAfter(reason: String?) =
        if (reason == TURN || reason == APPROACH) TURN_CALM_TICKS else CALM_TICKS

    private fun noteOvershoots() {
        val count = ModuleGameBot.games.activeMode.navigator?.overshoots ?: return
        if (count > overshoots) {
            val speed = player.deltaMovement.horizontalDistance() * TICKS_PER_SECOND
            report("follow overshoot n=$count speed=${"%.1f".format(Locale.ROOT, speed)} speedModule=$on")
        }
        overshoots = count
    }

    private fun holdReason(): String? {
        val game = ModuleGameBot.games.activeMode
        return when {
            !game.hasStarted -> "round"
            mc.gui.screen() is AbstractContainerScreen<*> -> "chest"
            ModuleScaffold.running -> "scaffold"
            ModuleFly.running || player.abilities.flying -> "fly"
            // Strafing goes around the target at any pace; the path's turns and goal only matter for walking.
            else -> game.speedHold() ?: game.navigator?.slowdown?.takeUnless { Strafing.isActive }
        }
    }

    /** For when GameBot stops; turns off the Speed it turned on. */
    fun stop() {
        if (on) {
            on = false
            ModuleSpeed.enabled = false
            report("speed off reason=stopped")
        }
        calmTicks = 0
    }

    private const val CALM_TICKS = 20
    private const val TURN_CALM_TICKS = 6
    private const val TURN = "turn"
    private const val APPROACH = "approach"
    private const val TICKS_PER_SECOND = 20

}
