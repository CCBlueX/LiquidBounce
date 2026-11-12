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
package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.Readiness
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.entity.getEyePositionAt
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.Route
import net.ccbluex.liquidbounce.utils.navigation.teleport.StepsValueGroup
import net.ccbluex.liquidbounce.utils.navigation.teleport.Teleport
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportManager
import net.ccbluex.liquidbounce.utils.navigation.teleport.Trip
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.reachPositions
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

/**
 * TpAura
 *
 * Teleports next to enemies, attacks them, then returns or stays.
 */
@Suppress("TooManyFunctions")
object ModuleTpAura : ClientModule("TpAura", ModuleCategories.COMBAT, disableOnQuit = true) {

    private val rangeValue = float("Range", 30f, 6f..100f, "blocks")
    private val range by rangeValue
    private val afterAttack by enumChoice("AfterAttack", AfterAttack.RETURN)
    private val hold by int("Hold", 10, 1..40, "ticks")

    private val targetSelector = tree(TargetSelector(TargetPriority.DISTANCE, rangeValue))
    private val clicker = tree(Clicker(this, mc.options.keyAttack))
    private val rotations = tree(RotationsValueGroup(this))
    private val steps = tree(StepsValueGroup())

    private val trip = Trip(this, steps)
    private val view = trip.view

    init {
        tree(trip.renderer)
    }

    private enum class AfterAttack(override val tag: String) : Tagged {
        RETURN("Return"),
        STAY("Stay"),
    }

    private enum class Phase { IDLE, ROUTING, READY, APPROACH, ATTACK, RETURN }

    private class Engagement(val target: LivingEntity) {
        var route: Route? = null
        var waited = 0
        var holdLeft = 0
        var retries = 0
        var attacked = false
    }

    private var phase = Phase.IDLE
    private var engagement: Engagement? = null
    private var ticks = 0
    private var restUntil = 0

    /** Whether the rotation going out this tick hits the target from where the server has the player. */
    private var facing = false

    /** Targets without a route, by entity id, until the tick they may be tried again. */
    private val unreachable = Int2IntOpenHashMap()

    private val reach: Double
        get() = player.entityInteractionRange() - REACH_MARGIN

    /** The engagement while its target is still one. */
    private val liveEngagement: Engagement?
        get() = engagement?.takeIf { targetSelector.validate(it.target) }

    override fun onDisabled() {
        TeleportManager.undo(this)
        reset()
        restUntil = 0
        unreachable.clear()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
        unreachable.clear()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        ticks++
        if (player.isDeadOrDying || player.isSpectator || player.isPassenger) {
            leave()
            return@handler
        }

        when (phase) {
            Phase.IDLE -> engage()
            Phase.ROUTING -> route()
            Phase.READY -> launch()
            Phase.APPROACH -> approach()
            Phase.ATTACK -> attack()
            Phase.RETURN -> if (!trip.isAway) {
                view.done()
                phase = Phase.IDLE
            }
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        facing = false
        val engagement = engagement ?: return@handler
        val feet = when (phase) {
            Phase.READY, Phase.APPROACH -> engagement.route?.destination
            Phase.ATTACK -> trip.feet
            else -> null
        } ?: return@handler

        val eyes = player.getEyePositionAt(feet)
        val box = engagement.target.boundingBox
        val rotation = raytraceBox(eyes, box, range = reach, wallsRange = 0.0)?.rotation
            ?: Rotation.lookingAt(box.center, eyes)

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(
                rotation,
                engagement.target,
                whenReady = Readiness({ going -> going.takeIf { isFacing(eyes, engagement.target, it) } }) {
                    facing = true
                },
            ),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleTpAura
        )
    }

    private fun engage() {
        if (ticks < restUntil || !trip.isAvailable) {
            return
        }

        val origin = trip.feet
        val target = targetSelector.targets().firstOrNull { unreachable.getOrDefault(it.id, 0) <= ticks }
        engagement = target?.let(::Engagement)
        if (target == null) {
            view.clear()
            return
        }

        view.target = target
        if (isInReach(origin, target)) {
            startAttack()
            return
        }

        val goals = attackPositions(target, origin)
        if (goals.isEmpty()) {
            fail(target)
        } else {
            requestRoute(origin, goals)
        }
    }

    private fun requestRoute(origin: Vec3, goals: List<BlockPos>) {
        trip.request(origin, goals, maxLength = range * ROUTE_DETOUR)
        phase = Phase.ROUTING
    }

    private fun route() {
        val engagement = liveEngagement ?: return leave()
        when (val result = trip.poll()) {
            RouteResult.Searching -> {}
            RouteResult.AlreadyThere -> startAttack()
            is RouteResult.Found -> {
                val route = result.value
                engagement.route = route
                engagement.waited = 0
                view.active(route.path)
                view.attackPosition = route.destination
                phase = Phase.READY
                launch()
            }
            is RouteResult.Failed -> fail(engagement.target)
        }
    }

    /** Sets off once the clicker is about to click when the route arrives. */
    private fun launch() {
        val engagement = liveEngagement ?: return leave()
        val route = engagement.route ?: return leave()
        if (!isInReach(route.destination, engagement.target)) {
            return replan()
        }

        val arrival = route.batches(steps.packetsPerTick).size
        if (!clicker.willClickAt(arrival) && engagement.waited++ < MAX_WAIT) {
            return
        }

        val finish = if (afterAttack == AfterAttack.RETURN) Teleport.Finish.HOLD else Teleport.Finish.LAND
        trip.dispatch(route, finish) ?: return leave()
        phase = Phase.APPROACH
    }

    private fun approach() {
        when (trip.run?.state ?: return leave()) {
            Teleport.State.ACTIVE -> {}
            Teleport.State.DONE -> startAttack()
            Teleport.State.REJECTED -> rejected()
            Teleport.State.INVALID, Teleport.State.CANCELLED -> {
                view.invalid()
                trip.showReturn()
                phase = Phase.RETURN
            }
        }
    }

    private fun startAttack() {
        engagement?.run {
            holdLeft = hold
            attacked = false
            retries = 0
        }
        phase = Phase.ATTACK
    }

    private fun attack() {
        if (trip.run?.state == Teleport.State.REJECTED) {
            return rejected()
        }

        val engagement = liveEngagement?.takeUnless { it.attacked && afterAttack == AfterAttack.RETURN }
            ?: return leave()
        val target = engagement.target
        val feet = trip.feet
        if (!isInReach(feet, target)) {
            return if (engagement.retries++ < MAX_RETRIES) replan() else leave()
        }

        view.engaged = true
        if (clicker.isClickTick && facing &&
            !player.cannotAttackWithItem(player.mainHandItem, 0)
        ) {
            clicker.click {
                attackEntity(target, SwingMode.DO_NOT_HIDE, keepSprint = true)
                engagement.attacked = true
                true
            }
        }

        if (afterAttack == AfterAttack.RETURN && --engagement.holdLeft <= 0) {
            leave()
        }
    }

    /** Routes again from where the server has the player, because the target moved. */
    private fun replan() {
        val target = engagement?.target ?: return leave()
        val origin = trip.feet
        val goals = attackPositions(target, origin)
        if (goals.isEmpty()) {
            return leave()
        }

        view.engaged = false
        requestRoute(origin, goals)
    }

    /** Ends the engagement and brings the server back to the client. */
    private fun leave() {
        if (phase == Phase.RETURN) {
            return
        }

        phase = if (trip.comeBack()) Phase.RETURN else Phase.IDLE
        engagement = null
    }

    private fun fail(target: LivingEntity) {
        unreachable.put(target.id, ticks + UNREACHABLE_TICKS)
        view.invalid()
        view.target = null
        engagement = null
        restUntil = ticks + FAIL_REST_TICKS
        phase = Phase.IDLE
    }

    /** The server set the player back, so it gets some rest before trying again. */
    private fun rejected() {
        reset()
        view.invalid()
        restUntil = ticks + SETBACK_REST_TICKS
    }

    private fun reset() {
        trip.reset()
        phase = Phase.IDLE
        engagement = null
        view.clear()
    }

    private fun isInReach(feet: Vec3, target: LivingEntity) =
        target.boundingBox.distanceToSqr(player.getEyePositionAt(feet)) <= Mth.square(reach)

    /** Whether [rotation] hits [target] from [eyes]. */
    private fun isFacing(eyes: Vec3, target: LivingEntity, rotation: Rotation): Boolean {
        val box = target.boundingBox.inflate(target.pickRadius.toDouble())
        if (box.contains(eyes)) {
            return true
        }

        val end = eyes.fma(reach + REACH_MARGIN, rotation.directionVector)
        val hit = box.clip(eyes, end).orElse(null) ?: return false
        return hasLineOfSight(eyes, hit)
    }

    /** Standable positions around [target] that see it within reach, closest to [from] first. */
    private fun attackPositions(target: LivingEntity, from: Vec3) =
        Terrain.live().reachPositions(target.boundingBox, reach - TARGET_MOVEMENT_MARGIN, from)

    private const val REACH_MARGIN = 0.1
    private const val TARGET_MOVEMENT_MARGIN = 0.4
    private const val ROUTE_DETOUR = 2.0
    private const val MAX_WAIT = 10
    private const val MAX_RETRIES = 2
    private const val FAIL_REST_TICKS = 10
    private const val SETBACK_REST_TICKS = 20
    private const val UNREACHABLE_TICKS = 40

}
