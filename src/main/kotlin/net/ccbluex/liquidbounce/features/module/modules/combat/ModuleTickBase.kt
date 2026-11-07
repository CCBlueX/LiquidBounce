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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.utils.percentageChance
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.utils.client.BalanceValueGroup
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * TickBase
 *
 * Spends the timer balance to reach an enemy earlier, either by bursting ticks or by speeding up the timer.
 */
internal object ModuleTickBase : ClientModule(
    "TickBase",
    ModuleCategories.COMBAT,
    aliases = listOf("TimerRange")
) {

    private val modes = choices("Mode", HoldFirst, arrayOf(HoldFirst, BurstFirst, TimerBoost))
        .apply { tagBy(this) }

    private val requiresKillAura by boolean("RequiresKillAura", true)
    private val balance = tree(BalanceValueGroup(this))

    /**
     * Holds the ticks back first and runs them in one burst afterwards. Holding earns what the burst costs.
     */
    private object HoldFirst : TickMode("HoldFirst", "Past", holdFirst = true)

    /**
     * Runs the ticks in one burst first and holds them back afterwards, which needs spendable balance.
     */
    private object BurstFirst : TickMode("BurstFirst", "Future", holdFirst = false)

    private open class TickMode(name: String, alias: String, private val holdFirst: Boolean) :
        Mode(name, listOf(alias)) {

        override val parent: ModeValueGroup<*>
            get() = modes

        private val call by enumChoice("Call", TickBaseCall.GAME)

        /**
         * The range defines where we want to tickbase into. The first value is the minimum range, which we can
         * tick into, and the second value is the range where we cannot tickbase at all.
         */
        private val range by floatRange("Range", 2.5f..4f, 0f..8f)

        private val maxTicksAtATime by int("MaxTicksAtATime", 4, 1..20, "ticks")
        private val pause by int("Pause", 0, 0..20, "ticks")
        private val cooldown by int("Cooldown", 0, 0..100, "ticks")
        private val forceGround by boolean("ForceGround", false)
        private val lineColor by color("Line", Color4b.WHITE)
            .doNotIncludeAlways()

        private val tickBuffer = ArrayList<TickData>()

        override fun disable() {
            tickBuffer.clear()
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            // We do not want this module to conflict with blink
            if (player.vehicle != null || ModuleBlink.running || tickBuffer.isEmpty()) {
                return@tickHandler
            }

            val nearbyEnemy = world.findEnemy(0f, range.endInclusive) ?: return@tickHandler
            val currentDistance = player.position().distanceToSqr(nearbyEnemy.position())
            val rangeSq = range.start.sq()..range.endInclusive.sq()

            // Find the best tick that is able to hit the target and is not too far away from the player, as well as
            // able to crit the target
            var possibleTicks = tickBuffer
                .withIndex()
                .filter { (_, tick) ->
                    val distSq = tick.position.distanceToSqr(nearbyEnemy.position())
                    distSq < currentDistance && distSq in rangeSq
                }

            if (forceGround) {
                possibleTicks = possibleTicks.filter { (_, tick) ->
                    tick.onGround
                }
            }

            val criticalTick = possibleTicks.firstOrNull { (_, tick) ->
                tick.fallDistance > 0.0f
            }

            val (bestTick, _) = criticalTick ?: possibleTicks.firstOrNull() ?: return@tickHandler

            if (bestTick == 0) {
                return@tickHandler
            }

            // We do not want to tickbase if killaura is not ready to attack
            fun breakRequirement() = requiresKillAura && !(ModuleKillAura.running &&
                    ModuleKillAura.clicker.willClickAt(bestTick))

            if (breakRequirement()) {
                return@tickHandler
            }

            debugParameter("Recommended Skip") { bestTick }

            if (holdFirst) {
                val hold = bestTick + pause
                Timer.hold(this@TickMode, hold)
                waitTicks(hold)
                Timer.burst(bestTick, balance.range) { call.tick() }
            } else {
                var burst = 0

                while (burst < bestTick && Timer.burst(1, balance.range) { call.tick() } == 1) {
                    burst++

                    if (breakRequirement()) {
                        break
                    }
                }

                debugParameter("Total Skipped") { burst }

                val hold = burst + pause
                Timer.hold(this@TickMode, hold)
                waitTicks(hold)
            }

            waitTicks(cooldown)
        }

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent> {
            // We do not want this module to conflict with blink
            if (player.vehicle != null || ModuleBlink.running) {
                return@handler
            }

            tickBuffer.clear()

            // Holding the ticks back first earns what running them costs
            val available = if (holdFirst) {
                balance.range.last - balance.range.first
            } else {
                Timer.spendableTicks(balance.range)
            }
            val ticks = min(maxTicksAtATime, available)
            if (ticks <= 0) {
                return@handler
            }

            val simulatedPlayer = PlayerSimulationCache.getSimulationForLocalPlayer()
            val snapshots = simulatedPlayer.getSnapshotsBetween(0 until ticks)

            tickBuffer.ensureCapacity(snapshots.size)
            snapshots.mapTo(tickBuffer) { snapshot ->
                TickData(
                    snapshot.pos,
                    snapshot.fallDistance,
                    snapshot.velocity,
                    snapshot.onGround
                )
            }
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (lineColor.a <= 0) {
                return@handler
            }

            event.renderEnvironment {
                drawLineStrip(
                    argb = lineColor.argb,
                    positions = MutableVertexList(tickBuffer.size)
                        .addAllRelativeToCamera(tickBuffer, camera) { it.position }
                )
            }
        }

    }

    /**
     * Runs the game slower while an enemy is near to save up balance, and faster once it is in range.
     */
    private object TimerBoost : Mode("Timer") {

        override val parent: ModeValueGroup<*>
            get() = modes

        private val chance = percentageChance("Chance", 100f)

        /**
         * Boosts while an enemy is closer than the end, but keeps the normal speed once it is closer than the start.
         */
        private val range by floatRange("Range", 3f..3.5f, 0f..10f)
        private val chargeRange by float("ChargeRange", 100f, 0f..500f)
        private val chargeSpeed by float("ChargeSpeed", 0.9f, 0.1f..1f)
        private val boostSpeed by float("BoostSpeed", 2f, 1f..10f)
        private val recoverSpeed by float("RecoverSpeed", 0.95f, 0.1f..1f)
        private val onlyOnGround by boolean("OnlyOnGround", false)

        /**
         * Set once boosting used up the balance, until the balance is full again.
         */
        private var exhausted = false

        override fun enable() {
            exhausted = false
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (onlyOnGround && !player.onGround()) return@tickHandler
            if (requiresKillAura && (!ModuleKillAura.running || ModuleKillAura.targetTracker.target == null)) {
                return@tickHandler
            }

            val speed = timerSpeed() ?: return@tickHandler
            Timer.requestBalancedTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTickBase, balance.range)
            debugParameter("Exhausted") { exhausted }
        }

        private fun timerSpeed(): Float? {
            if (world.findEnemy(0f, range.start) != null) {
                return 1f
            }

            if (world.findEnemy(0f, chargeRange) == null || !chance.asBoolean) {
                return null
            }

            if (world.findEnemy(0f, range.endInclusive) == null) {
                return chargeSpeed
            }

            val current = Timer.balanceWithin(balance.range)
            if (current <= balance.range.first) {
                exhausted = true
            } else if (current >= balance.range.last) {
                exhausted = false
            }

            return if (exhausted) recoverSpeed else boostSpeed
        }

    }

    @JvmRecord
    private data class TickData(
        val position: Vec3,
        val fallDistance: Double,
        val velocity: Vec3,
        val onGround: Boolean
    )

    @Suppress("unused")
    private enum class TickBaseCall(
        override val tag: String,
        private val tick: Runnable
    ) : Tagged {

        /**
         * Runs a full game tick.
         *
         * TODO: Cancel full game ticks after this,
         *   not just the player ticks.
         */
        GAME("Game", { mc.tick() }),

        /**
         * This will NOT update the game tick,
         * but only the player tick - that means
         * e.g. Rotation Manager will not update either.
         *
         * This was the previous default behavior of the TickBase,
         * so it is kept for compatibility reasons.
         */
        PLAYER("Player", { player.tick() });

        fun tick() = tick.run()
    }

}
