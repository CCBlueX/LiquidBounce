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
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Turns Scaffold on for the level bridges the walk crosses, so they are run across instead of built block by block.
 * Telly goes on with it: the server sets back sprinting away from where it sees the player look, so the look only
 * turns to the blocks in the air. Both are put back as they were after the crossing.
 *
 * Logs a `bridge start` and a `bridge end` line for every crossing.
 */
internal class Bridge(
    private val owner: EventListener,
    private val navigator: Navigator,
) : EventListener, MinecraftShortcuts {

    private class Crossing(val from: Vec3, val startedAt: Int, val blocks: Int, val tellyWas: Boolean?) {
        var lastAt = startedAt
        var last = from

        /** The heading of the straight line run along, how often it changed, and the largest camera turn a tick. */
        var heading: Float? = null
        var lines = 0
        var lastYaw = Float.NaN
        var maxTurn = 0f
    }

    private var ticks = 0
    private var crossing: Crossing? = null

    val isActive: Boolean
        get() = crossing != null

    override fun parent() = owner

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        val ahead = navigator.bridgeAhead
        val crossing = crossing
        when {
            ahead > 0 && crossing != null -> {
                crossing.lastAt = ticks
                crossing.last = player.position()
                track(crossing)
            }

            ahead > 0 && ModuleScaffold.blockCount > 0 -> start(ahead)

            // Replanning drops the path for a moment; the crossing only ends once back on the ground.
            crossing != null && ticks - crossing.lastAt >= CALM_TICKS && player.onGround() -> stop()
        }
    }

    private fun start(blocks: Int) {
        // A Scaffold already on is the user's, and so are its settings.
        val owns = !ModuleScaffold.enabled
        val tellyWas = if (owns) ScaffoldTellyFeature.enabled else null
        if (owns) {
            ScaffoldTellyFeature.enabled = true
            ModuleScaffold.enabled = true
        }

        crossing = Crossing(player.position(), ticks, ModuleScaffold.blockCount, tellyWas)
        // The motion is already slowed by friction; the step moved last tick tells the speed.
        val speed = player.position().horizontalDistanceTo(Vec3(player.xo, player.yo, player.zo)) * TICKS_PER_SECOND
        report("bridge start blocks=$blocks bps=${format(speed)}")
    }

    /** Ends the crossing, also for when the round ends or the walk stops. */
    fun stop() {
        val crossing = crossing ?: return
        this.crossing = null
        crossing.tellyWas?.let {
            ModuleScaffold.enabled = false
            ScaffoldTellyFeature.enabled = it
        }

        val seconds = (crossing.lastAt - crossing.startedAt).toDouble() / TICKS_PER_SECOND
        val speed = if (seconds > 0.0) crossing.last.horizontalDistanceTo(crossing.from) / seconds else 0.0
        val used = (crossing.blocks - ModuleScaffold.blockCount).coerceAtLeast(0)
        val heading = crossing.heading?.let { format(it.toDouble()) } ?: "none"
        report(
            "bridge end blocks=$used bps=${format(speed)} time=${format(seconds)}s heading=$heading " +
                "lines=${crossing.lines} max_turn=${format(crossing.maxTurn.toDouble())}"
        )
    }

    private fun track(crossing: Crossing) {
        navigator.bridgeHeading?.let { heading ->
            if (crossing.heading != heading) {
                crossing.heading = heading
                crossing.lines++
                report("bridge heading=${format(heading.toDouble())}")
            }
        }
        val yaw = player.yRot
        if (!crossing.lastYaw.isNaN()) {
            crossing.maxTurn = max(crossing.maxTurn, abs(Mth.wrapDegrees(yaw - crossing.lastYaw)))
        }
        crossing.lastYaw = yaw
    }

    private fun format(value: Double) = "%.2f".format(Locale.ROOT, value)

    private companion object {
        const val TICKS_PER_SECOND = 20
        const val CALM_TICKS = 10
    }

}
