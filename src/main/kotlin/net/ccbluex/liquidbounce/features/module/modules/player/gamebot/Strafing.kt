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
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTargetStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.world.entity.player.Player

/**
 * Hands the movement to TargetStrafe once the KillAura target is within its follow range: holds forward so it
 * circles the target, overriding the walk and FightBot, and turns the camera to the target. [VoidSafety] runs after
 * and still holds the player back at void edges.
 *
 * Logs `strafe on` and `strafe off` lines.
 */
internal object Strafing : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private var target: Player? = null

    /** Whether the movement is handed to TargetStrafe right now. */
    val isActive: Boolean
        get() = target != null

    private var missTicks = 0

    // Before VoidSafety and TargetStrafe, after the walk and FightBot.
    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        val target = nextTarget()
        if (target !== this.target) {
            this.target?.let { report("strafe off target=${it.scoreboardName}") }
            target?.let { report("strafe on target=${it.scoreboardName}") }
            this.target = target
            ModuleTargetStrafe.inputFromFeatures = target != null
        }
        target ?: return@handler

        event.directionalInput = DirectionalInput.FORWARDS
        event.jump = player.isInWater
        event.sneak = false
        ModuleGameBot.games.activeMode.navigator?.lookAt(target.eyePosition)
    }

    /**
     * The opponent to strafe around: the KillAura target once within TargetStrafe's range sphere, then kept while
     * TargetStrafe plans a strafe around it and it stays within [KEEP_MARGIN] past the follow range. A moment in which
     * strafing cannot go on, such as the target out of sight for TargetStrafe, is waited out for [HOLD_TICKS].
     */
    private fun nextTarget(): Player? {
        val game = ModuleGameBot.games.activeMode
        // A flight start walks into the fly; opening a chest stands still. Neither waits out the hold.
        if (game.isOpeningChest || ModuleFly.enabled) {
            missTicks = 0
            return null
        }
        val canStrafe = game.hasStarted && !game.keepsAway && ModuleKillAura.running && ModuleTargetStrafe.isReady &&
            game.navigator?.isBuilding != true && !player.abilities.flying && Outnumbered.plan !is Outnumbered.Retreat

        target?.let { current ->
            val keeps = canStrafe && current.isAlive && Opponents.isOpponent(current) &&
                ModuleTargetStrafe.isStrafing &&
                distanceTo(current) <= ModuleTargetStrafe.followRange + KEEP_MARGIN
            if (keeps) {
                missTicks = 0
                return current
            }
            if (++missTicks < HOLD_TICKS) {
                return current
            }
        }
        missTicks = 0

        val candidate = ModuleKillAura.targetTracker.target as? Player ?: return null
        val fits = canStrafe && Opponents.isOpponent(candidate) && ModuleTargetStrafe.isInRange(candidate)
        return candidate.takeIf { fits }
    }

    private fun distanceTo(other: Player) = other.position().horizontalDistanceTo(player.position())

    /** For when GameBot stops; the next round starts without a target. */
    fun stop() {
        target?.let { report("strafe off target=${it.scoreboardName}") }
        target = null
        missTicks = 0
        ModuleTargetStrafe.inputFromFeatures = false
    }

    private const val KEEP_MARGIN = 1.5f
    private const val HOLD_TICKS = 10

}
