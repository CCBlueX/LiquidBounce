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
package net.ccbluex.liquidbounce.gametest.navigation

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.player
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.Mth
import kotlin.math.abs

/**
 * Finds glances back in the yaw the player sends: more than [AWAY] degrees off and back within [BACK] degrees inside
 * [WINDOW] ticks. Mining and placing aim at their blocks, so a glance that spans them does not count.
 */
internal object TurnWatch : EventListener {

    private val yaws = FloatArray(WINDOW)
    private val acted = BooleanArray(WINDOW)
    private var ticks = 0
    private var acting: () -> Boolean = { false }
    private val glances = mutableListOf<String>()

    @Volatile
    private var active = false

    override val running: Boolean
        get() = active

    @Suppress("unused")
    private val tickHandler = handler<PlayerPostTickEvent> {
        val yaw = RotationManager.currentRotation?.yaw ?: player.yRot
        yaws[ticks % WINDOW] = yaw
        acted[ticks % WINDOW] = acting()
        ticks++

        val back = ticks - 1
        var spanActed = acted[back % WINDOW]
        for (from in back - 1 downTo maxOf(0, back - WINDOW + 1)) {
            spanActed = spanActed || acted[from % WINDOW]
            val start = yaws[from % WINDOW]
            if (spanActed || from == back - 1 || abs(Mth.wrapDegrees(yaw - start)) > BACK) {
                continue
            }
            val away = (from + 1 until back).count { abs(Mth.wrapDegrees(yaws[it % WINDOW] - start)) > AWAY }
            if (away > 0) {
                glances += "$away ticks at ${player.blockPosition().toShortString()}"
                // One glance is reported once.
                yaws.fill(yaw)
                break
            }
        }
    }

    fun <T> watch(
        context: ClientGameTestContext,
        what: String,
        acting: () -> Boolean = { false },
        block: () -> T,
    ): T {
        context.onClient {
            ticks = 0
            glances.clear()
            this.acting = acting
            active = true
        }
        val result = try {
            block()
        } finally {
            context.onClient { active = false }
        }

        val found = context.fromClient { glances.toList() }
        check(found.isEmpty()) { "$what: glanced back for ${found.joinToString()}" }
        return result
    }

    const val AWAY = 120f
    const val BACK = 45f
    const val WINDOW = 20

}
