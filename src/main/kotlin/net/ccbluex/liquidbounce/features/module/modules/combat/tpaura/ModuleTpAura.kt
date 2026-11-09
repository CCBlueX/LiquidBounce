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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.navigation.render.RouteRenderer
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.RunFinish
import net.ccbluex.liquidbounce.utils.navigation.teleport.RunState
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportDispatcher
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportPlan
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportRun
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportStepSettings
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportSteps
import net.ccbluex.liquidbounce.utils.navigation.teleport.reachPositions
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
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
    private val rotations = tree(RotationsValueGroup(this, combatSpecific = true))
    private val steps = tree(TeleportStepSettings())

    private val view = RouteView().apply {
        ghost = { partialTicks -> TeleportDispatcher.serverPosition(ModuleTpAura, partialTicks) }
    }
    private val planner = RoutePlanner<TeleportPlan>(this)

    init {
        tree(RouteRenderer(this, view))
    }

    private enum class AfterAttack(override val tag: String) : Tagged {
        RETURN("Return"),
        STAY("Stay"),
    }

    private enum class Phase {
        IDLE,
        ROUTING,
        READY,
        APPROACH,
        ATTACK,
        RETURN,
    }

    private var phase = Phase.IDLE
    private var target: LivingEntity? = null
    private var found: TeleportPlan? = null
    private var run: TeleportRun? = null
    private var waited = 0
    private var holdLeft = 0
    private var retries = 0
    private var attacked = false
    private var cooldown = 0
    private var ticks = 0

    /**
     * Targets without a route, by entity id, until the tick they may be tried again.
     */
    private val unreachable = Int2IntOpenHashMap()

    private val reach: Double
        get() = player.entityInteractionRange() - REACH_MARGIN

    /**
     * Where the server has the player.
     */
    private val feet: Vec3
        get() = TeleportDispatcher.serverPosition?.takeIf { TeleportDispatcher.holds(this) } ?: player.position()

    override fun onDisabled() {
        TeleportDispatcher.undo(this)
        reset()
        cooldown = 0
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
        if (cooldown > 0) {
            cooldown--
        }

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
            Phase.RETURN -> if (!TeleportDispatcher.holds(this)) {
                view.done()
                phase = Phase.IDLE
            }
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        val target = target ?: return@handler
        val from = when (phase) {
            Phase.READY, Phase.APPROACH -> found?.route?.destination ?: return@handler
            Phase.ATTACK -> feet
            else -> return@handler
        }

        val eyes = from.add(0.0, player.eyeHeight.toDouble(), 0.0)
        val box = target.boundingBox
        val rotation = raytraceBox(eyes, box, range = reach, wallsRange = 0.0)?.rotation
            ?: Rotation.lookingAt(box.center, eyes)

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(rotation, target),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleTpAura
        )
    }

    private fun engage() {
        if (cooldown > 0 || !TeleportDispatcher.isAvailable(this)) {
            return
        }

        val origin = feet
        val target = targetSelector.targets().firstOrNull { unreachable.getOrDefault(it.id, 0) <= ticks }
        this.target = target
        if (target == null) {
            view.clear()
            return
        }

        view.target = target
        if (isInReach(origin, target)) {
            startAttack()
            return
        }

        val goals = attackPositions(NavigationTerrain(LiveWorld(world)), target, origin)
        if (goals.isEmpty()) {
            fail(target)
            return
        }

        requestRoute(origin, goals)
        phase = Phase.ROUTING
    }

    private fun requestRoute(origin: Vec3, goals: List<BlockPos>) {
        val limits = RouteLimits(maxLength = range * ROUTE_DETOUR)
        val goal = BlockGoal(goals)
        val stepLength = steps.stepLength
        planner.request(
            BlockPos.containing(origin) to goal,
            goal.region(origin, limits.maxLength, limits.maxLength.toInt()),
        ) { terrain -> TeleportRouteSearch(terrain, origin, goals, limits, stepLength) }
        view.pending(origin, Vec3.atBottomCenterOf(goals.first()))
    }

    private fun route() {
        val target = target
        if (target == null || !targetSelector.validate(target)) {
            leave()
            return
        }

        when (val result = planner.poll()) {
            RouteResult.Searching -> showSearch()
            RouteResult.AlreadyThere -> {
                planner.cancel()
                startAttack()
            }
            is RouteResult.Found -> {
                planner.cancel()
                found = result.value
                waited = 0
                view.active(result.value.route.path)
                view.attackPosition = result.value.route.destination
                phase = Phase.READY
                launch()
            }
            is RouteResult.Failed -> {
                planner.cancel()
                fail(target)
            }
        }
    }

    private fun showSearch() {
        val start = view.start
        val destination = view.destination
        if (start != null && destination != null) {
            view.pending(start, destination, planner.preview)
        }
    }

    /**
     * Sets off once the clicker is about to click when the route arrives.
     */
    private fun launch() {
        val target = target
        val found = found
        if (target == null || found == null || !targetSelector.validate(target)) {
            leave()
            return
        }

        if (!isInReach(found.route.destination, target)) {
            replan()
            return
        }

        val arrival = TeleportSteps.batches(found.route.start, found.steps, steps.packetsPerTick).size
        if (!clicker.willClickAt(arrival) && waited++ < MAX_WAIT) {
            return
        }

        val finish = if (afterAttack == AfterAttack.RETURN) RunFinish.HOLD else RunFinish.LAND
        val run = TeleportDispatcher.dispatch(this, found.route, found.steps, finish, steps)
        if (run == null) {
            leave()
            return
        }

        this.run = run
        view.active(run.route.path, run)
        phase = Phase.APPROACH
    }

    private fun approach() {
        val run = run ?: return leave()
        when (run.state) {
            RunState.ACTIVE -> {}
            RunState.DONE -> startAttack()
            RunState.REJECTED -> rejected()
            RunState.INVALID, RunState.CANCELLED -> {
                view.invalid()
                showReturn()
                phase = Phase.RETURN
            }
        }
    }

    private fun startAttack() {
        holdLeft = hold
        attacked = false
        retries = 0
        phase = Phase.ATTACK
    }

    private fun attack() {
        if (run?.state == RunState.REJECTED) {
            rejected()
            return
        }

        val target = target
        if (target == null || !targetSelector.validate(target) || attacked && afterAttack == AfterAttack.RETURN) {
            leave()
            return
        }

        val feet = feet
        if (!isInReach(feet, target)) {
            if (retries++ < MAX_RETRIES) replan() else leave()
            return
        }

        view.engaged = true
        val eyes = feet.add(0.0, player.eyeHeight.toDouble(), 0.0)
        if (clicker.isClickTick && isFacing(eyes, target) && !player.cannotAttackWithItem(player.mainHandItem, 0)) {
            clicker.click {
                attackEntity(target, SwingMode.DO_NOT_HIDE, keepSprint = true)
                attacked = true
                true
            }
        }

        if (afterAttack == AfterAttack.RETURN && --holdLeft <= 0) {
            leave()
        }
    }

    /**
     * Routes again from where the server has the player, because the target moved.
     */
    private fun replan() {
        val target = target ?: return leave()
        val origin = feet
        val goals = attackPositions(NavigationTerrain(LiveWorld(world)), target, origin)
        if (goals.isEmpty()) {
            leave()
            return
        }

        view.engaged = false
        requestRoute(origin, goals)
        phase = Phase.ROUTING
    }

    /**
     * Ends the engagement and brings the server back to the client.
     */
    private fun leave() {
        if (phase == Phase.RETURN) {
            return
        }

        planner.cancel()
        view.engaged = false
        if (TeleportDispatcher.holds(this)) {
            TeleportDispatcher.undo(this)
            showReturn()
            phase = Phase.RETURN
        } else {
            phase = Phase.IDLE
        }

        target = null
        found = null
    }

    private fun showReturn() {
        TeleportDispatcher.run?.takeIf { it.owner === this && it.finish == RunFinish.RESYNC }?.let {
            view.active(it.route.path, it)
        }
    }

    private fun fail(target: LivingEntity) {
        unreachable.put(target.id, ticks + UNREACHABLE_TICKS)
        view.invalid()
        view.target = null
        this.target = null
        found = null
        cooldown = FAIL_COOLDOWN
        phase = Phase.IDLE
    }

    /**
     * The server set the player back, so it gets some rest before trying again.
     */
    private fun rejected() {
        reset()
        view.invalid()
        cooldown = SETBACK_COOLDOWN
    }

    private fun reset() {
        planner.cancel()
        phase = Phase.IDLE
        target = null
        found = null
        run = null
        view.clear()
    }

    private fun isInReach(feet: Vec3, target: LivingEntity) =
        target.boundingBox.distanceToSqr(feet.add(0.0, player.eyeHeight.toDouble(), 0.0)) <= Mth.square(reach)

    /**
     * Whether the rotation the server has hits [target] from [eyes].
     */
    private fun isFacing(eyes: Vec3, target: LivingEntity): Boolean {
        val box = target.boundingBox.inflate(target.pickRadius.toDouble())
        if (box.contains(eyes)) {
            return true
        }

        val end = eyes.fma(reach + REACH_MARGIN, RotationManager.serverRotation.directionVector)
        val hit = box.clip(eyes, end).orElse(null) ?: return false
        return hasLineOfSight(eyes, hit)
    }

    /**
     * Standable positions around [target] that see it within reach, closest to [from] first.
     */
    private fun attackPositions(terrain: NavigationTerrain, target: LivingEntity, from: Vec3) =
        reachPositions(terrain, target.boundingBox, reach - TARGET_MOVEMENT_MARGIN, from)

    private const val REACH_MARGIN = 0.1
    private const val TARGET_MOVEMENT_MARGIN = 0.4
    private const val ROUTE_DETOUR = 2.0
    private const val MAX_WAIT = 10
    private const val MAX_RETRIES = 2
    private const val FAIL_COOLDOWN = 10
    private const val SETBACK_COOLDOWN = 20
    private const val UNREACHABLE_TICKS = 40

}
