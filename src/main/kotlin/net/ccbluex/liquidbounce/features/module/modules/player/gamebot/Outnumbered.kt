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
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.math.allEmpty
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Notices when two or more opponents close in while the bot is low or loses the trade, and decides how to get out:
 * [Focus] on the one standing apart from the others, which KillAura then attacks alone, or [Retreat] away from the
 * group over ground, so AutoBuff can heal. The games follow [plan]; it ends once the fight is one on one again or
 * the bot healed.
 *
 * Logs `outnumbered n=<n> hp=<hp> -> retreat|focus <name>` and `outnumbered over`.
 */
internal object Outnumbered : EventListener, MinecraftShortcuts {

    sealed interface Plan
    class Focus(val target: Player) : Plan
    class Retreat(val point: Vec3) : Plan

    var plan: Plan? = null
        private set

    /** Who the plan was made against; still counted while KillAura leaves all but the focus alone. */
    private var group = emptySet<String>()
    private var decidedAt = 0
    private var ticks = 0

    /** Ticks in a row the trigger held, before a plan; and the group stayed away, during one. */
    private var triggerTicks = 0
    private var clearTicks = 0

    override fun parent() = ModuleGameBot

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        if (!ModuleGameBot.games.activeMode.hasStarted) {
            end("round")
            return@handler
        }

        val hp = player.health
        val current = plan
        if (current == null) {
            val near = closeOpponents(GROUP_RANGE)
            val triggered = near.size >= MIN_GROUP && (hp < LOW_HP || isLosing())
            triggerTicks = if (triggered) triggerTicks + 1 else 0
            if (triggerTicks >= TRIGGER_TICKS) {
                decide(near, hp)
            }
            return@handler
        }

        val near = closeOpponents(CLEAR_RANGE)
        clearTicks = if (near.size < MIN_GROUP) clearTicks + 1 else 0
        val review = current is Focus && !current.target.isAlive || ticks - decidedAt >= REVIEW_TICKS
        when {
            current is Retreat && hp >= HEALED_HP -> end("healed")
            clearTicks >= CLEAR_TICKS -> end("one on one")
            review && near.size >= MIN_GROUP -> decide(near, hp)
        }
    }

    // While focusing, KillAura leaves the rest of the group alone.
    @Suppress("unused")
    private val tagHandler = handler<TagEntityEvent> { event ->
        val focus = (plan as? Focus)?.target ?: return@handler
        val entity = event.entity
        if (entity is Player && entity !== focus && entity !== player && entity.scoreboardName in group) {
            event.dontTarget()
        }
    }

    /** For when GameBot stops. */
    fun stop() {
        end("stopped")
    }

    private fun closeOpponents(range: Float): List<Player> = world.players().filter { other ->
        val counted = if (plan == null) Opponents.isOpponent(other) else other.scoreboardName in group
        counted && other.isAlive && !other.isSpectator && other.distanceTo(player) <= range
    }

    private fun isLosing() = Opponents.hitsLanded(TRADE_TICKS) < Opponents.hitsTaken(TRADE_TICKS)

    private fun decide(near: List<Player>, hp: Float) {
        decidedAt = ticks
        group = near.mapTo(HashSet()) { it.scoreboardName }
        // The opponent farthest from its closest ally.
        val apart = near.maxByOrNull { other -> near.filter { it !== other }.minOf { it.distanceTo(other) } }

        // Low on health the bot retreats, and keeps doing so until healed or the group stayed away; losing the
        // trade with health left, it takes on the one standing apart.
        val retreating = plan as? Retreat
        val retreat = if (retreating != null || hp < LOW_HP) retreatPoint(near) ?: retreating?.point else null
        val next = when {
            retreat != null -> Retreat(retreat)
            apart != null -> Focus(apart)
            else -> null
        }
        val line = when (next) {
            is Retreat -> "retreat"
            is Focus -> "focus ${next.target.scoreboardName}"
            null -> return end("no way out")
        }
        if (line != describe(plan)) {
            report("outnumbered n=${near.size} hp=${"%.1f".format(Locale.ROOT, hp)} -> $line")
        }
        plan = next
    }

    private fun describe(plan: Plan?) = when (plan) {
        is Retreat -> "retreat"
        is Focus -> "focus ${plan.target.scoreboardName}"
        null -> null
    }

    private fun end(reason: String) {
        if (plan != null) {
            report("outnumbered over reason=$reason")
        }
        plan = null
        group = emptySet()
        triggerTicks = 0
        clearTicks = 0
    }

    /** A spot [RETREAT_DISTANCE] away from [near], reached over ground; directions turn aside if the way is not. */
    fun retreatPoint(near: List<Player>): Vec3? {
        val position = player.position()
        val centerX = near.sumOf { it.x } / near.size
        val centerZ = near.sumOf { it.z } / near.size
        val away = atan2(position.z - centerZ, position.x - centerX)
        for (offset in RETREAT_OFFSETS) {
            val angle = away + Math.toRadians(offset)
            val dx = cos(angle)
            val dz = sin(angle)
            val groundAll = (1..RETREAT_DISTANCE).all { step ->
                hasGround(position.x + dx * step, position.y, position.z + dz * step)
            }
            if (groundAll) {
                return Vec3(position.x + dx * RETREAT_DISTANCE, position.y, position.z + dz * RETREAT_DISTANCE)
            }
        }
        return null
    }

    /** Ground within [GROUND_DEPTH] blocks below, so the way does not lead over a void edge. */
    private fun hasGround(x: Double, y: Double, z: Double): Boolean {
        val box = AABB(x - HALF_WIDTH, y - GROUND_DEPTH, z - HALF_WIDTH, x + HALF_WIDTH, y, z + HALF_WIDTH)
        return !world.getBlockCollisions(player, box).allEmpty()
    }

    private const val MIN_GROUP = 2
    private const val GROUP_RANGE = 6f
    private const val LOW_HP = 12f
    private const val HEALED_HP = 16f
    private const val TRADE_TICKS = 60
    private const val REVIEW_TICKS = 20
    private const val TRIGGER_TICKS = 20
    private const val CLEAR_RANGE = 10f
    private const val CLEAR_TICKS = 60

    private const val RETREAT_DISTANCE = 8
    private val RETREAT_OFFSETS = doubleArrayOf(0.0, 45.0, -45.0, 90.0, -90.0)
    private const val GROUND_DEPTH = 3.0
    private const val HALF_WIDTH = 0.3

}
