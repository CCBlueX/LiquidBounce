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
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.utils.percentageChance
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
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
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.balanceReset
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerSnapshot
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.entity.serverBox
import net.ccbluex.liquidbounce.utils.entity.serverPosition
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.InputTracker.wasPressedRecently
import net.ccbluex.liquidbounce.utils.item.armorKnockbackResistance
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
     * How full the attack cooldown has to be on the server when the hit lands, where the server has one. KillAura's
     * own minimum applies on top.
     */
    private val attackCooldown by float("AttackCooldown", 1f, 0f..1f)

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
        tree(Advantage)
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

        /**
         * Whether the server has the attack cooldown full enough for a hit landing [landingDelay] ticks of real time
         * from now.
         */
        protected fun cooldownReady(landingDelay: Int): Boolean {
            if (!player.hasCooldown) {
                return true
            }

            val killAura = ModuleKillAura.clicker.itemCooldown
                ?.takeIf { ModuleKillAura.running && ModuleKillAura.targetTracker.target != null }
            val required = max(attackCooldown, killAura?.nextCooldown ?: 0f)
            val progress = CombatManager.attackCooldownProgress(landingDelay)
            ModuleTickBase.debugParameter("Attack Cooldown At Landing") { progress }
            return progress >= required
        }

        override fun disable() {
            TickBaseVisuals.clear()
        }

    }

    /**
     * Speeds the game up once the balance reaches a hit.
     */
    private object TimerBoost : BoostMode("Timer") {

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
            // Each boosted tick takes 1 / speed ticks of real time
            val landingDelay = ceil((ticks ?: 0) / boostSpeed).toInt()
            val target = hit?.takeIf {
                val after = snapshotsFromNow().drop(path.size).take(Advantage.knockbackTicks).toList()
                requirementsMet(it.point) && cooldownReady(landingDelay) &&
                    Advantage.paysOff(it, path + after, path.lastIndex, landingDelay)
            }
            TickBaseVisuals.show(path.map { it.pos }, ticks.takeIf { target != null }, target?.point)
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
         * The snapshots of the boost the balance pays for, up to the hit landing on the last, and that hit.
         */
        private fun planBoost(): Pair<List<SimulatedPlayerSnapshot>, Hit?> {
            val hitAt = hitPointFrom()
            val path = ArrayList<SimulatedPlayerSnapshot>()

            for (snapshot in snapshotsFromNow().take(boostTicks() + 1)) {
                path += snapshot
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
            // Longer boosts are not worth simulating every tick
            return min(ticks.toInt(), 100)
        }

    }

    /**
     * Holds the game back until the balance pays for a burst that lands the hit, then bursts. Positions still
     * update while held, so the burst follows where the target is by then.
     */
    private object HoldFirst : BurstMode("HoldFirst", "Past", holdFirst = true)

    /**
     * Bursts right away. What the charged balance does not cover is held back afterwards.
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

        /**
         * While holding first, the normal ticks that will run between the hold and the burst.
         */
        private var walkAfterHold: Int? = null
        private var plannedAtBalance = Float.NaN

        override fun timerSpeed() = 1f.takeIf { busy }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            TickBaseVisuals.clear()

            // We do not want this module to conflict with blink
            if (player.vehicle != null || ModuleBlink.running) {
                return@tickHandler
            }

            // The current tick still runs. Holding first then runs the delay and the tick the burst follows,
            // so the player covers them at the normal speed
            val delayTicks = delay.random()
            val walk = if (holdFirst) delayTicks + 2 else 1
            val ticks = plan(snapshotsFromNow(), walk)
            if (ticks == null) {
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
                if (holdFirst) {
                    holdFirst(delayTicks)
                } else {
                    burstFirst(ticks - walk)
                }
            } finally {
                busy = false
                walkAfterHold = null
            }
        }

        /**
         * Holds back ticks until [holdHandler] finds the balance pays for the burst, then bursts after the delay.
         * Held ticks do not count towards the waited ticks, the burst ticks do.
         */
        private suspend fun holdFirst(delayTicks: Int) {
            val walk = delayTicks + 1
            walkAfterHold = walk
            plannedAtBalance = Float.NaN
            Timer.hold(this, maxTicks)

            // Resumes with the first tick after the hold
            waitTicks(1)
            walkAfterHold = null

            val ticks = plan(currentSnapshotsFromNow(), walk)?.minus(walk)
            if (ticks == null || ticks > Timer.balance) {
                rolled = null
                waitTicks(cooldown.random())
                return
            }

            waitTicks(delayTicks)
            val burst = Timer.burst(ticks, 0..maxBalance + maxTicks)
            TickBaseVisuals.clear()
            waitTicks(burst + cooldown.random())
        }

        /**
         * Bursts [ticks] ticks and holds back the ticks the charged balance does not cover after the delay.
         */
        private suspend fun burstFirst(ticks: Int) {
            val charged = Timer.balance.toInt().coerceIn(0, maxBalance)
            val burst = Timer.burst(ticks, -maxTicks..maxBalance)
            TickBaseVisuals.clear()
            waitTicks(burst + delay.random())
            Timer.hold(this, max(0, burst - charged))
            waitTicks(1 + cooldown.random())
        }

        /**
         * Plans the burst again with every tick held, against where the target is now. Releases the hold once the
         * balance pays for it, or the hit is gone.
         */
        @Suppress("unused")
        private val holdHandler = handler<TickPacketProcessEvent> {
            val walk = walkAfterHold ?: return@handler
            if (Timer.balance == plannedAtBalance) {
                return@handler
            }
            plannedAtBalance = Timer.balance

            val ticks = plan(currentSnapshotsFromNow(), walk)
            if (ticks == null || ticks - walk <= Timer.balance) {
                walkAfterHold = null
                Timer.hold(this, 0)
            }
        }

        override fun disable() {
            busy = false
            walkAfterHold = null
            super.disable()
        }

        /**
         * The tick of [snapshots] to land the hit on after [walk] normal ticks, shown in the visuals. Null when no
         * hit lands and pays off, or the requirements are not met.
         */
        private fun plan(snapshots: Sequence<SimulatedPlayerSnapshot>, walk: Int): Int? {
            val simulated = snapshots.take(walk + maxTicks + 1 + Advantage.knockbackTicks).toList()
            val path = simulated.take(walk + maxTicks + 1)
            val hitAt = hitPointFrom()
            val hits = path.map { hitAt(it.eyes()) }
            val ticks = ticksToHit(path, hits, walk, simulated)
            val hit = ticks?.let { hits[it] }?.takeIf { requirementsMet(it.point) }
            TickBaseVisuals.show(path.map { it.pos }, ticks.takeIf { hit != null }, hit?.point)
            return ticks.takeIf { hit != null }
        }

        /**
         * The tick of [path] to land the hit on, preferring a critical one, with [walk] normal ticks before the
         * burst. [hits] are the hits from each tick of [path], [simulated] the player simulated on past it. Null
         * when no hit lands on [path] and pays off, or one lands by just walking without a critical hit to wait for.
         */
        private fun ticksToHit(
            path: List<SimulatedPlayerSnapshot>,
            hits: List<Hit?>,
            walk: Int,
            simulated: List<SimulatedPlayerSnapshot>
        ): Int? {
            val inRange = path.indices.filter { hits[it] != null }
            val first = inRange.firstOrNull() ?: return null

            // The n-th game tick from now moves the player to path[n + 1], held ticks are no game ticks
            val landing = inRange.filter { tick ->
                val delay = landingDelay(tick, walk)
                tick > walk && killAuraClicksAt(tick - 1) && cooldownReady(delay) &&
                    hits[tick]?.let { Advantage.paysOff(it, simulated, tick, delay) } == true
            }
            val critical = if (prefersCritical) {
                landing.firstOrNull { path[it].falling }
            } else {
                null
            }

            return when {
                critical != null -> critical
                first <= walk -> null
                else -> landing.firstOrNull()
            }
        }

        /**
         * Ticks of real time until the hit on [tick] of the path lands: the normal ticks before the burst, and when
         * holding first, the held ticks the balance does not pay for yet.
         */
        private fun landingDelay(tick: Int, walk: Int) =
            if (holdFirst) walk + max(0, tick - walk - Timer.balance.toInt()) else walk

    }

    /**
     * A hit on [entity], landing on [point].
     */
    private class Hit(val entity: Entity, val point: Vec3)

    /**
     * Boosts only when the hit wins the exchange: the enemy is not invulnerable when it lands, and cannot hit back
     * within [knockbackTicks] ticks. That is the case when the knockback pushes it out of [enemyRange] of us, when its
     * own attack cooldown is not full yet, or when our hit is critical. The last two apply only where the server has
     * an attack cooldown.
     */
    private object Advantage : ToggleableValueGroup(ModuleTickBase, "Advantage", true) {

        private val enemyRange by float("EnemyRange", 3f, 0f..8f, "blocks")

        /**
         * Ticks after our hit lands in which the enemy must not be able to hit back.
         */
        val knockbackTicks by int("KnockbackTicks", 2, 0..10, "ticks")

        /**
         * For servers whose knockback differs from vanilla.
         */
        private val knockbackMultiplier by float("KnockbackMultiplier", 1f, 0f..3f)
        private val skipInvulnerable by boolean("SkipInvulnerable", true)

        /**
         * Where the server has an attack cooldown, a hit also wins when the enemy's cooldown is not full by the end of
         * [knockbackTicks], or when it is a critical hit.
         */
        private val enemyCooldown by boolean("EnemyCooldown", true)
        private val criticals by boolean("Criticals", true)

        /**
         * Whether [hit] pays off when it lands with the player at [landing] of [path], [landingDelay] ticks of real
         * time from now. [path] starts now and goes on past [landing] as far as it was simulated.
         */
        fun paysOff(hit: Hit, path: List<SimulatedPlayerSnapshot>, landing: Int, landingDelay: Int): Boolean {
            val enemy = hit.entity as? LivingEntity
            if (!enabled || enemy == null) {
                return true
            }

            // Invulnerable when the hit reaches the server, so it takes no knockback and hits back. The hurt time is a
            // round trip late, and did not count down while ticks were held since it arrived
            val hurtTime = enemy.hurtTime - Timer.heldTicksWithin(enemy.hurtDuration - enemy.hurtTime)
            ModuleTickBase.debugParameter("Enemy Hurt Time") { hurtTime }
            if (skipInvulnerable && hurtTime > landingDelay + player.ping / 50) {
                return false
            }

            val modern = player.hasCooldown
            val strength = if (modern) CombatManager.attackCooldownProgress(landingDelay) else 1f
            val critical = modern && criticals && isCritical(path[landing], strength)
            // Our hit reaches the server half a ping after we send it
            val knockbackEnd = landingDelay + player.ping / 2.0 / 50 + knockbackTicks
            val enemyStrength = (enemy as? Player)
                ?.takeIf { modern && enemyCooldown }
                ?.let { CombatManager.attackCooldownProgress(it, knockbackEnd) }
            // Vanilla's threshold for sprint knockback and crits
            val sprintHit = player.isSprinting && strength > 0.9f
            val range = rangeAfterKnockback(enemy, path, landing, landingDelay, sprintHit)
            ModuleTickBase.debugParameter("Critical") { critical }
            ModuleTickBase.debugParameter("Enemy Cooldown After Knockback") { enemyStrength }
            ModuleTickBase.debugParameter("Enemy Range After Knockback") { range }
            return range > enemyRange || critical || enemyStrength != null && enemyStrength <= 0.9f
        }

        /**
         * How far [enemy] is from us [knockbackTicks] ticks after the knockback of our hit, which lands with the
         * player at [landing] of [path], [landingDelay] ticks of real time from now.
         */
        private fun rangeAfterKnockback(
            enemy: LivingEntity,
            path: List<SimulatedPlayerSnapshot>,
            landing: Int,
            landingDelay: Int,
            sprintHit: Boolean
        ): Double {
            val prediction = PositionExtrapolation.getBestForEntity(enemy)
            // The prediction starts where the client draws the enemy, which falls behind the server while ticks
            // are held
            val offset = enemy.serverPosition.subtract(enemy.position())
            var feet = prediction.getPositionInTicks(landingDelay.toDouble()).add(offset)
            // The server halves the motion it has of the enemy. Of players it only has what is left of earlier
            // knockback, and their client takes the result as its new motion
            val before = if (enemy is Player) {
                Vec3.ZERO
            } else {
                prediction.getPositionInTicks(landingDelay + 1.0).add(offset).subtract(feet)
            }
            var motion = knockback(enemy, path[landing].pos, feet, before, sprintHit)

            repeat(knockbackTicks) {
                feet = feet.add(motion)
                // Knockback lifts the enemy off the ground, where only the air slows it down
                motion = motion.scale(0.91)
            }

            val box = player.getBoundingBoxAt(path[min(landing + knockbackTicks, path.lastIndex)].pos)
            return sqrt(box.distanceToSqr(feet.add(0.0, enemy.eyeHeight.toDouble(), 0.0)))
        }

        /**
         * Whether the hit is critical: falling, a full enough cooldown, and not sprinting, which KillAura and Criticals
         * stop for a critical hit.
         */
        private fun isCritical(landing: SimulatedPlayerSnapshot, strength: Float): Boolean {
            val canCrit = ModuleCriticals.allowsCriticalHit(ignoreOnGround = true) &&
                (!player.isSprinting || prefersCritical)
            return landing.falling && strength > 0.9f && canCrit
        }

        /**
         * The horizontal motion of [enemy] at [at] after our hit from [from], with the server having [motion] of it
         * before.
         */
        private fun knockback(enemy: LivingEntity, from: Vec3, at: Vec3, motion: Vec3, sprintHit: Boolean): Vec3 {
            val away = Vec3(at.x - from.x, 0.0, at.z - from.z).normalize()
            // Armor is the only knockback resistance known of other entities, the attribute is not synced
            val resistance = enemy.armorItems.sumOf { it.armorKnockbackResistance }.coerceIn(0.0, 1.0)
            val strength = knockbackMultiplier * (1.0 - resistance)
            val knocked = Vec3(motion.x, 0.0, motion.z).scale(0.5).add(away.scale(0.4 * strength))
            return if (sprintHit) knocked.scale(0.5).add(away.scale(0.5 * strength)) else knocked
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
                // Past the hit, as it lands on the surface of the hitbox
                enemyUnderCrosshair(eyes, player.lookAngle, eyes.distanceTo(hit) + 1.0) != null
            }
            CLICK -> mc.options.keyAttack.isPressedOnAny || mc.options.keyAttack.wasPressedRecently(250)
            ON_GROUND -> player.onGround()
        }
    }

    private fun requirementsMet(hit: Vec3) = requires.all { it.meets(hit) }

    private val prefersCritical
        get() = ModuleCriticals.running ||
            ModuleKillAura.running && ModuleKillAura.criticalsSelectionMode != CriticalsSelectionMode.IGNORE

    /**
     * Where a hit from the given eyes lands: on the target of KillAura or Aimbot, otherwise on an enemy under the
     * crosshair. Null when it misses. Uses where the server has the enemies, which the client only catches up with
     * while ticking.
     */
    private fun hitPointFrom(): (Vec3) -> Hit? {
        val killAuraTarget = ModuleKillAura.targetTracker.target?.takeIf { ModuleKillAura.running }
        if (killAuraTarget != null) {
            val box = killAuraTarget.serverBox()
            val range = ModuleKillAura.range.interactionRange.toDouble()
            val wallsRange = ModuleKillAura.range.interactionThroughWallsRange.toDouble()
            return { eyes -> box.hitPoint(eyes, range, wallsRange)?.let { Hit(killAuraTarget, it) } }
        }

        val range = if (ModuleReach.running) {
            ModuleReach.entity.interactionRange.toDouble()
        } else {
            player.entityInteractionRange()
        }

        val aimbotTarget = ModuleAimbot.targetTracker.target?.takeIf { ModuleAimbot.running }
        if (aimbotTarget != null) {
            val box = aimbotTarget.serverBox()
            return { eyes -> box.hitPoint(eyes, range, 0.0)?.let { Hit(aimbotTarget, it) } }
        }

        val direction = player.lookAngle
        return { eyes -> enemyUnderCrosshair(eyes, direction, range) }
    }

    private fun AABB.hitPoint(eyes: Vec3, range: Double, wallsRange: Double) = getNearestPoint(eyes).takeIf {
        it.closerThan(eyes, range) && canSeeBox(eyes, this, range, wallsRange)
    }

    /**
     * Like [net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult], against the server boxes.
     */
    private fun enemyUnderCrosshair(eyes: Vec3, direction: Vec3, range: Double): Hit? {
        val end = eyes.fma(range, direction)
        // The server box may be off the box on the client, which stays behind while ticks are held
        val candidates = world.getEntities(player, AABB(eyes, end).inflate(8.0)) {
            EntitySelector.CAN_BE_PICKED.test(it) && it.shouldBeAttacked()
        }

        return candidates.mapNotNull { entity ->
            val box = entity.serverBox().inflate(entity.pickRadius.toDouble())
            val point = if (box.contains(eyes)) eyes else box.clip(eyes, end).orElse(null)
            point?.let { Hit(entity, it) }
        }.minByOrNull { it.point.distanceToSqr(eyes) }?.takeIf { hasLineOfSight(eyes, it.point) }
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

    /**
     * Like [snapshotsFromNow], simulated from the current state of the player. While ticks are held, the cached
     * simulation misses what changed in the meantime, such as knockback.
     */
    private fun currentSnapshotsFromNow() = SimulatedPlayerCache(
        SimulatedPlayer.fromClientPlayer(SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input)))
    ).simulate()

    private fun SimulatedPlayerSnapshot.eyes() = pos.add(0.0, player.eyeHeight.toDouble(), 0.0)

    private val SimulatedPlayerSnapshot.falling
        get() = !onGround && fallDistance > 0.0

}
