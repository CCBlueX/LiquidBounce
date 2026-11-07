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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
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
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.balanceReset
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerSnapshot
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.InputTracker.wasPressedRecently
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

/**
 * TickBase
 *
 * Saves up timer balance near an enemy and spends it to reach the enemy earlier, by speeding up the timer or by
 * bursting ticks.
 */
internal object ModuleTickBase : ClientModule(
    "TickBase",
    ModuleCategories.COMBAT,
    aliases = listOf("TimerRange")
) {

    private val modes = choices("Mode", TimerBoost, arrayOf(TimerBoost, HoldFirst, BurstFirst))
        .apply { tagBy(this) }

    /**
     * What has to be met to spend the balance. Saving it up does not depend on it.
     */
    private val requires by multiEnumChoice("Requires", Requirement.KILLAURA)
    private val chance = percentageChance("Chance", 100f)
    private val cooldown by intRange("Cooldown", 0..0, 0..100, "ticks")

    /**
     * Charging slows the game down while an enemy is near, until the balance reaches [maxBalance].
     */
    private val maxBalance by int("MaxBalance", 3, 0..40, "ticks")
    private val chargeRange by float("ChargeRange", 50f, 0f..500f)
    private val chargeSpeed by float("ChargeSpeed", 0.9f, 0.1f..1f)
    private val chargeWhileUsingItem by boolean("ChargeWhileUsingItem", false)

    @Suppress("unused")
    private val balanceReset = balanceReset()

    init {
        tree(TickBaseVisuals)
    }

    /**
     * The balance charging saves up.
     */
    internal val balance
        get() = 0..maxBalance

    /**
     * Whether [chance] lets us take the current chance to hit earlier, rolled once until the chance is gone.
     */
    private var rolled: Boolean? = null

    private fun rollChance() = rolled ?: chance.asBoolean.also { rolled = it }

    override fun onDisabled() {
        rolled = null
        TickBaseVisuals.clear()
    }

    @Suppress("unused")
    private val speedHandler = handler<GameTickEvent> {
        val speed = modes.activeMode.timerSpeed() ?: chargingSpeed() ?: return@handler
        Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleTickBase)
    }

    /**
     * Charges while an enemy is near but out of reach. Plain [Timer.requestTimerSpeed], since clamping the balance
     * would drop what [BurstFirst] still has to pay back.
     */
    private fun chargingSpeed() = chargeSpeed.takeIf {
        chargeSpeed < 1f && Timer.balance < maxBalance &&
            (chargeWhileUsingItem || !player.isUsingItem) &&
            world.findEnemy(0f, chargeRange) != null &&
            hitPointFrom()(player.eyePosition) == null
    }

    private sealed class BoostMode(name: String, aliases: List<String> = emptyList()) : Mode(name, aliases) {

        override val parent: ModeValueGroup<*>
            get() = modes

        /**
         * The speed this mode needs the game at this tick, null to leave it to charging.
         */
        abstract fun timerSpeed(): Float?

        override fun disable() {
            TickBaseVisuals.clear()
        }

    }

    /**
     * Speeds the game up once the balance reaches a hit.
     */
    private object TimerBoost : BoostMode("Timer") {

        /**
         * Longer boosts are not worth simulating every tick.
         */
        private const val MAX_BOOST_TICKS = 100

        private val boostSpeed by float("BoostSpeed", 5f, 1f..10f)

        private var boosting = false
        private var cooldownTicks = 0

        override fun enable() {
            boosting = false
            cooldownTicks = 0
        }

        override fun timerSpeed(): Float? {
            if (cooldownTicks > 0) {
                cooldownTicks--
            }

            val (path, hit) = planBoost()
            val ticks = if (hit != null) path.lastIndex else null
            val target = hit?.takeIf(::requirementsMet)
            TickBaseVisuals.show(path, ticks.takeIf { target != null }, target)
            debugParameter("Ticks To Hit") { ticks }

            val boost = ticks != null && ticks > 0 && target != null && cooldownTicks <= 0 && rollChance()
            if (boosting && !boost) {
                cooldownTicks = cooldown.random()
            }
            boosting = boost

            if (ticks == null || ticks == 0) {
                rolled = null
            }

            return when {
                boost -> boostSpeed
                // A boost ending in reach goes back to the normal speed right away
                ticks == 0 -> 1f
                else -> null
            }
        }

        /**
         * The positions of the boost the balance pays for, up to the hit landing on the last, and that hit.
         */
        private fun planBoost(): Pair<List<Vec3>, Vec3?> {
            val hitAt = hitPointFrom()
            val path = ArrayList<Vec3>()

            for (snapshot in snapshotsFromNow().take(boostTicks() + 1)) {
                path += snapshot.pos
                val hit = hitAt(snapshot.eyes())
                if (hit != null) {
                    return path to hit
                }
            }

            return path to null
        }

        /**
         * Ticks the balance lets run at [boostSpeed]. Each takes 1 / speed ticks of real time and spends the rest
         * of a tick of balance.
         */
        private fun boostTicks(): Int {
            if (boostSpeed <= 1f) {
                return 0
            }

            val ticks = Timer.spendableTicks(balance) / (1f - 1f / boostSpeed)
            return min(ticks.toInt(), MAX_BOOST_TICKS)
        }

    }

    /**
     * Runs the ticks in one burst. What the charged balance does not cover is held back right before.
     */
    private object HoldFirst : BurstMode("HoldFirst", "Past", holdFirst = true)

    /**
     * Runs the ticks in one burst. What the charged balance does not cover is held back right after.
     */
    private object BurstFirst : BurstMode("BurstFirst", "Future", holdFirst = false)

    private open class BurstMode(name: String, alias: String, private val holdFirst: Boolean) :
        BoostMode(name, listOf(alias)) {

        private val maxTicks by int("MaxTicks", 4, 1..20, "ticks", aliases = listOf("MaxTicksAtATime"))

        /**
         * Ticks the game runs normally between holding back and bursting.
         */
        private val delay by intRange("Delay", 0..0, 0..20, "ticks")

        /**
         * Set while bursting and paying for it, when the game has to run at the normal speed.
         */
        private var busy = false

        override fun timerSpeed() = 1f.takeIf { busy }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            TickBaseVisuals.clear()

            // We do not want this module to conflict with blink
            if (player.vehicle != null || ModuleBlink.running) {
                return@tickHandler
            }

            val charged = Timer.balance.toInt().coerceIn(0, maxBalance)

            // The current tick still runs. Holding first then runs the delay and the tick the burst follows,
            // so the player covers them at the normal speed
            val walk = if (holdFirst) delay.random() + 2 else 1
            val path = snapshotsFromNow().take(walk + maxTicks + 1).toList()
            val hitAt = hitPointFrom()
            val ticks = ticksToHit(path, hitAt, walk)
            val hit = ticks?.let { hitAt(path[it].eyes()) }?.takeIf(::requirementsMet)
            TickBaseVisuals.show(path.map { it.pos }, ticks.takeIf { hit != null }, hit)

            if (ticks == null || hit == null) {
                rolled = null
                return@tickHandler
            }

            if (!rollChance()) {
                return@tickHandler
            }
            rolled = null
            debugParameter("Ticks") { ticks }

            busy = true
            try {
                boost(ticks - walk, charged, walk)
            } finally {
                busy = false
            }
        }

        /**
         * Bursts [ticks] ticks and holds back the ticks the [charged] balance does not cover, with the delay
         * between. Held ticks do not count towards the waited ticks, the burst ticks do.
         */
        private suspend fun boost(ticks: Int, charged: Int, walk: Int) {
            if (holdFirst) {
                Timer.hold(this, max(0, ticks - charged))
                waitTicks(walk - 1)
                val burst = Timer.burst(ticks, 0..maxBalance + maxTicks)
                TickBaseVisuals.clear()
                waitTicks(burst + cooldown.random())
            } else {
                val burst = Timer.burst(ticks, -maxTicks..maxBalance)
                TickBaseVisuals.clear()
                waitTicks(burst + delay.random())
                Timer.hold(this, max(0, burst - charged))
                waitTicks(1 + cooldown.random())
            }
        }

        override fun disable() {
            busy = false
            super.disable()
        }

        /**
         * The tick of [path] to land the hit on, preferring a critical one, with [walk] normal ticks before the
         * burst. Null when no hit lands on [path], or one lands by just walking without a critical hit to wait for.
         */
        private fun ticksToHit(path: List<SimulatedPlayerSnapshot>, hitAt: (Vec3) -> Vec3?, walk: Int): Int? {
            val inRange = path.indices.filter { hitAt(path[it].eyes()) != null }
            val first = inRange.firstOrNull() ?: return null

            // The n-th game tick from now moves the player to path[n + 1], held ticks are no game ticks
            val landing = inRange.filter { tick -> tick > walk && killAuraClicksAt(tick - 1) }
            val critical = if (prefersCritical) {
                landing.firstOrNull { !path[it].onGround && path[it].fallDistance > 0.0 }
            } else {
                null
            }

            return when {
                critical != null -> critical
                first <= walk -> null
                else -> landing.firstOrNull()
            }
        }

    }

    @Suppress("unused")
    private enum class Requirement(override val tag: String) : Tagged {
        KILLAURA("KillAura"),
        LOOKING_AT("LookingAt"),
        CLICK("Click"),
        ON_GROUND("OnGround");

        fun meets(hit: Vec3) = when (this) {
            KILLAURA -> ModuleKillAura.running && ModuleKillAura.targetTracker.target != null
            LOOKING_AT -> player.eyePosition.let { eyes ->
                enemyUnderCrosshair(eyes, player.lookAngle, eyes.distanceTo(hit) + LOOK_MARGIN) != null
            }
            CLICK -> mc.options.keyAttack.isPressedOnAny || mc.options.keyAttack.wasPressedRecently(CLICK_MILLIS)
            ON_GROUND -> player.onGround()
        }
    }

    /**
     * Past the hit, as it lands on the surface of the hitbox.
     */
    private const val LOOK_MARGIN = 1.0
    private const val CLICK_MILLIS = 250L

    private fun requirementsMet(hit: Vec3) = requires.all { it.meets(hit) }

    private val prefersCritical
        get() = ModuleCriticals.running ||
            ModuleKillAura.running && ModuleKillAura.criticalsSelectionMode != CriticalsSelectionMode.IGNORE

    /**
     * Where a hit from the given eyes lands: on the target of KillAura or Aimbot, otherwise on an enemy under the
     * crosshair. Null when it misses.
     */
    private fun hitPointFrom(): (Vec3) -> Vec3? {
        val killAuraTarget = ModuleKillAura.targetTracker.target?.takeIf { ModuleKillAura.running }
        if (killAuraTarget != null) {
            val range = ModuleKillAura.range.interactionRange.toDouble()
            val wallsRange = ModuleKillAura.range.interactionThroughWallsRange.toDouble()
            return { eyes -> killAuraTarget.boundingBox.hitPoint(eyes, range, wallsRange) }
        }

        val range = if (ModuleReach.running) {
            ModuleReach.entity.interactionRange.toDouble()
        } else {
            player.entityInteractionRange()
        }

        val aimbotTarget = ModuleAimbot.targetTracker.target?.takeIf { ModuleAimbot.running }
        if (aimbotTarget != null) {
            return { eyes -> aimbotTarget.boundingBox.hitPoint(eyes, range, 0.0) }
        }

        val direction = player.lookAngle
        return { eyes -> enemyUnderCrosshair(eyes, direction, range) }
    }

    private fun AABB.hitPoint(eyes: Vec3, range: Double, wallsRange: Double) = getNearestPoint(eyes).takeIf {
        it.distanceToSqr(eyes) <= range.sq() && canSeeBox(eyes, this, range, wallsRange)
    }

    private fun enemyUnderCrosshair(eyes: Vec3, direction: Vec3, range: Double): Vec3? {
        val end = eyes.fma(range, direction)
        val hit = ProjectileUtil.getEntityHitResult(
            player,
            eyes,
            end,
            AABB(eyes, end).inflate(1.0),
            EntitySelector.CAN_BE_PICKED.and { it.shouldBeAttacked() },
            range.sq()
        ) ?: return null

        return hit.location.takeIf { hasLineOfSight(eyes, it) }
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

    private fun SimulatedPlayerSnapshot.eyes() = pos.add(0.0, player.eyeHeight.toDouble(), 0.0)

}
