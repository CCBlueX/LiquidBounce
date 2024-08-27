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

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler

/**
 * A rotation manager
 */
object CombatManager : Listenable {

    // useful for something like autoSoup
    private var pauseCombat: Int = 0

    // useful for something like autopot
    private var pauseRotation: Int = 0

    // useful for autoblock
    private var pauseBlocking: Int = 0

    private var duringCombat: Int = 0

    private fun updatePauseRotation() {
        if (pauseRotation <= 0) return

        pauseRotation--
    }

    private fun updatePauseCombat() {
        if (pauseCombat <= 0) return

        pauseCombat--
    }

    private fun updatePauseBlocking() {
        if (pauseBlocking <= 0) return

        pauseBlocking--
    }

    private fun updateDuringCombat() {
        if (duringCombat <= 0) return

        duringCombat--
    }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        updatePauseRotation()
        updatePauseCombat()
        // TODO: implement this for killaura autoblock and other
        updatePauseBlocking()
        updateDuringCombat()
    }

    val tickHandler = handler<GameTickEvent> {
        update()
    }

    @Suppress("unused")
    val attackHandler = handler<AttackEvent> {
        // 40 ticks = 2 seconds
        duringCombat = 40
    }

    val shouldPauseCombat: Boolean
        get() = this.pauseCombat > 0
    val shouldPauseRotation: Boolean
        get() = this.pauseRotation > 0
    val shouldPauseBlocking: Boolean
        get() = this.pauseBlocking > 0
    val isInCombat: Boolean
        get() = this.duringCombat > 0

    fun pauseCombatForAtLeast(pauseTime: Int) {
        this.pauseCombat = this.pauseCombat.coerceAtLeast(pauseTime)
    }

    fun pauseRotationForAtLeast(pauseTime: Int) {
        this.pauseRotation = this.pauseRotation.coerceAtLeast(pauseTime)
    }

    fun pauseBlockingForAtLeast(pauseTime: Int) {
        this.pauseBlocking = this.pauseBlocking.coerceAtLeast(pauseTime)
    }

}
