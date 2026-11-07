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
import net.ccbluex.liquidbounce.config.utils.percentageChance
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.client.BalanceValueGroup
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerSnapshot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.AABB
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

    private val chance = percentageChance("Chance", 100f)
    private val cooldown by intRange("Cooldown", 0..0, 0..100, "ticks")
    private val balance = tree(BalanceValueGroup(this))

    /**
     * Whether [chance] lets us take the current chance to hit earlier, rolled once until the chance is gone.
     */
    private var rolled: Boolean? = null

    private fun rollChance() = rolled ?: chance.asBoolean.also { rolled = it }

    override fun onDisabled() {
        rolled = null
    }

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

        private val maxTicksAtATime by int("MaxTicksAtATime", 4, 1..20, "ticks")
        private val lineColor by color("Line", Color4b.WHITE)
            .doNotIncludeAlways()

        private var path = emptyList<SimulatedPlayerSnapshot>()

        override fun disable() {
            path = emptyList()
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            path = emptyList()

            // We do not want this module to conflict with blink
            if (player.vehicle != null || ModuleBlink.running) {
                return@tickHandler
            }

            // Holding the ticks back first earns what running them costs
            val available = if (holdFirst) {
                balance.range.last - balance.range.first
            } else {
                Timer.spendableTicks(balance.range)
            }
            val maxTicks = min(maxTicksAtATime, available)
            if (maxTicks <= 0) {
                return@tickHandler
            }

            path = snapshotsFromNow().take(maxTicks + 1).toList()
            val ticks = ticksToHit()
            if (ticks == null) {
                rolled = null
                return@tickHandler
            }

            if (!rollChance()) {
                return@tickHandler
            }
            rolled = null
            debugParameter("Ticks") { ticks }

            // The burst ticks count towards the waited ticks as well
            if (holdFirst) {
                Timer.hold(this@TickMode, ticks)
                waitTicks(ticks)
                waitTicks(Timer.burst(ticks, balance.range) + cooldown.random())
            } else {
                val burst = Timer.burst(ticks, balance.range)
                Timer.hold(this@TickMode, burst)
                waitTicks(burst * 2 + cooldown.random())
            }
        }

        /**
         * The ticks to burst so that a hit lands on the last of them, preferring a critical one.
         * Null when no hit lands on [path], or one lands right away without a critical hit to wait for.
         */
        private fun ticksToHit(): Int? {
            val hits = hitCheck()
            val inRange = path.indices.filter { hits(path[it].eyes()) }

            // Holding first delays the burst by the ticks it holds
            val landing = inRange.filter { killAuraClicksAt(if (holdFirst) it * 2 else it) }
            val critical = if (prefersCritical) {
                landing.firstOrNull { !path[it].onGround && path[it].fallDistance > 0.0 }
            } else {
                null
            }

            val ticks = when {
                critical != null -> critical
                inRange.firstOrNull() == 0 -> return null
                else -> landing.firstOrNull() ?: return null
            }
            return ticks.takeIf { it > 0 }
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (lineColor.a <= 0) {
                return@handler
            }

            event.renderEnvironment {
                drawLineStrip(
                    argb = lineColor.argb,
                    positions = MutableVertexList(path.size)
                        .addAllRelativeToCamera(path, camera) { it.pos }
                )
            }
        }

    }

    /**
     * Runs the game slower while an enemy is near to save up balance, and faster once the balance reaches a hit.
     */
    private object TimerBoost : Mode("Timer") {

        /**
         * Longer boosts are not worth simulating every tick.
         */
        private const val MAX_BOOST_TICKS = 100

        override val parent: ModeValueGroup<*>
            get() = modes

        private val chargeRange by float("ChargeRange", 100f, 0f..500f)
        private val chargeSpeed by float("ChargeSpeed", 0.9f, 0.1f..1f)
        private val chargeWhileUsingItem by boolean("ChargeWhileUsingItem", false)
        private val boostSpeed by float("BoostSpeed", 2f, 1f..10f)
        private val onlyOnGround by boolean("OnlyOnGround", false)

        private var boosting = false
        private var cooldownTicks = 0

        override fun enable() {
            boosting = false
            cooldownTicks = 0
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (cooldownTicks > 0) {
                cooldownTicks--
            }

            if (onlyOnGround && !player.onGround()) return@tickHandler

            val speed = timerSpeed() ?: return@tickHandler
            Timer.requestBalancedTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTickBase, balance.range)
        }

        private fun timerSpeed(): Float? {
            val ticks = ticksToHit()
            debugParameter("Ticks To Hit") { ticks }

            val boost = ticks != null && ticks > 0 && cooldownTicks <= 0 && rollChance()
            if (boosting && !boost) {
                cooldownTicks = cooldown.random()
            }
            boosting = boost

            if (ticks == null || ticks == 0) {
                rolled = null
            }

            return when {
                boost -> boostSpeed
                ticks == 0 -> 1f
                (chargeWhileUsingItem || !player.isUsingItem) && world.findEnemy(0f, chargeRange) != null ->
                    chargeSpeed
                else -> null
            }
        }

        /**
         * The ticks until a hit lands within the boost the balance pays for.
         */
        private fun ticksToHit(): Int? {
            val hits = hitCheck()
            return snapshotsFromNow()
                .take(boostTicks() + 1)
                .indexOfFirst { hits(it.eyes()) }
                .takeIf { it >= 0 }
        }

        /**
         * Ticks the balance lets run at [boostSpeed]. Each takes 1 / speed ticks of real time and spends the rest
         * of a tick of balance.
         */
        private fun boostTicks(): Int {
            if (boostSpeed <= 1f) {
                return 0
            }

            val ticks = Timer.spendableTicks(balance.range) / (1f - 1f / boostSpeed)
            return min(ticks.toInt(), MAX_BOOST_TICKS)
        }

    }

    private val prefersCritical
        get() = ModuleCriticals.running ||
            ModuleKillAura.running && ModuleKillAura.criticalsSelectionMode != CriticalsSelectionMode.IGNORE

    /**
     * Whether a hit from the given eyes lands: on the target of KillAura or Aimbot, otherwise on an enemy
     * under the crosshair.
     */
    private fun hitCheck(): (Vec3) -> Boolean {
        val killAuraTarget = ModuleKillAura.targetTracker.target?.takeIf { ModuleKillAura.running }
        if (killAuraTarget != null) {
            val range = ModuleKillAura.range.interactionRange.toDouble()
            val wallsRange = ModuleKillAura.range.interactionThroughWallsRange.toDouble()
            return { eyes -> killAuraTarget.boundingBox.isInRange(eyes, range, wallsRange) }
        }

        val range = if (ModuleReach.running) {
            ModuleReach.entity.interactionRange.toDouble()
        } else {
            player.entityInteractionRange()
        }

        val aimbotTarget = ModuleAimbot.targetTracker.target?.takeIf { ModuleAimbot.running }
        if (aimbotTarget != null) {
            return { eyes -> aimbotTarget.boundingBox.isInRange(eyes, range, 0.0) }
        }

        val direction = player.lookAngle
        return { eyes -> isEnemyUnderCrosshair(eyes, direction, range) }
    }

    private fun AABB.isInRange(eyes: Vec3, range: Double, wallsRange: Double) =
        distanceToSqr(eyes) <= range.sq() && canSeeBox(eyes, this, range, wallsRange)

    private fun isEnemyUnderCrosshair(eyes: Vec3, direction: Vec3, range: Double): Boolean {
        val end = eyes.fma(range, direction)
        val hit = ProjectileUtil.getEntityHitResult(
            player,
            eyes,
            end,
            AABB(eyes, end).inflate(1.0),
            EntitySelector.CAN_BE_PICKED.and { it.shouldBeAttacked() },
            range.sq()
        ) ?: return false

        return hasLineOfSight(eyes, hit.location)
    }

    /**
     * Whether KillAura clicks the given ticks from now, if it is the one attacking.
     */
    private fun killAuraClicksAt(ticks: Int) = !ModuleKillAura.running ||
        ModuleKillAura.targetTracker.target == null || ModuleKillAura.clicker.willClickAt(ticks)

    /**
     * Where the player is in the coming ticks, starting with now. The simulation starts at the movement input of
     * the last tick, so its first step is now.
     */
    private fun snapshotsFromNow() = PlayerSimulationCache.getSimulationForLocalPlayer().simulate().drop(1)

    private fun SimulatedPlayerSnapshot.eyes() = pos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0)

}
