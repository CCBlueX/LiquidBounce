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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.deeplearn.combat.CombatController
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.clicker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.doesCollideAt
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

/**
 * Data class holding combat-related context
 */
data class CombatContext(
    val playerPosition: Vec3,
    val combatTarget: CombatTarget?
)

data class CombatTarget(
    val entity: Entity,
    val distance: Double,
    val range: Float,
    val outOfDistance: Boolean,
    val targetRotation: Rotation,
    val requiredTargetRotation: Rotation,
    val outOfDanger: Boolean
)

/**
 * A fight bot that handles combat and movement automatically
 */
object KillAuraFightBot : NavigationBaseValueGroup<CombatContext>(ModuleKillAura, "FightBot", false) {

    private val opponentRange by float("OpponentRange", 3f, 0.1f..10f)
    private val dangerousYawDiff by float("DangerousYaw", 55f, 0f..90f, suffix = "°")
    private val runawayOnCooldown by boolean("RunawayOnCooldown", true)
    private val ai by boolean("AI", false)

    /** Whether the model moves us within reach; the engine still brings us there. */
    val usesAi get() = running && ai

    internal object TargetFilter : ValueGroup("TargetFilter") {
        internal var range by float("Range", 50f, 10f..100f)
        internal var visibleOnly by boolean("VisibleOnly", true)
        internal var notWhenVoid by boolean("NotWhenVoid", true)
    }

    /**
     * Configuration for leader following functionality
     */
    internal object LeaderFollower : ToggleableValueGroup(this, "Leader", false) {
        internal val username by text("Username", "")
        internal val radius by float("Radius", 5f, 2f..10f)
    }

    init {
        tree(TargetFilter)
        tree(LeaderFollower)
    }

    fun updateTarget() {
        targetTracker.select { entity ->
            if (player.squaredBoxedDistanceTo(entity) > TargetFilter.range.sq()) {
                return@select null
            }

            if (TargetFilter.visibleOnly && !player.hasLineOfSight(entity)) {
                return@select null
            }

            if (TargetFilter.notWhenVoid && entity.doesNotCollideBelow()) {
                return@select null
            }

            entity
        }
    }

    /**
     * Creates combat context
     */
    override fun createNavigationContext(): CombatContext {
        val playerPosition = player.position()

        val combatTarget = targetTracker.target?.let { entity ->
            val distance = playerPosition.distanceTo(entity.position())
            val range = min(ModuleKillAura.range.interactionRange, distance.toFloat())
            val outOfDistance = distance > opponentRange

            val targetRotation = entity.rotation.copy(pitch = 0.0f)
            val requiredTargetRotation = Rotation.lookingAt(playerPosition, entity.eyePosition).copy(pitch = 0.0f)
            val outOfDanger = abs(targetRotation.rotationDeltaTo(requiredTargetRotation).deltaYaw) > dangerousYawDiff

            CombatTarget(entity, distance, range, outOfDistance, targetRotation, requiredTargetRotation, outOfDanger)
        }

        return CombatContext(
            playerPosition,
            combatTarget
        )
    }

    /**
     * Calculates the desired position to move towards
     *
     * @return Target position as Vec3d
     */
    override fun calculateGoalPosition(context: CombatContext): Vec3? {
        // Try to follow leader first
        if (LeaderFollower.running && LeaderFollower.username.isNotEmpty()) {
            val leader = world.players().find { it.gameProfile.name == LeaderFollower.username }
            if (leader != null) {
                return calculateLeaderGoalPosition(leader.position(), context.playerPosition)
            }
        }

        // Otherwise handle combat movement
        val combatTarget = context.combatTarget ?: return null
        return if (runawayOnCooldown && !clicker.willClickAt()) {
            calculateRunawayPosition(context, combatTarget)
        } else {
            calculateAttackPosition(context, combatTarget)
        }
    }

    /** The model needs this tick's decision before the movement input is read. */
    @Suppress("unused")
    private val decisionHandler = handler<GameTickEvent> {
        val target = targetTracker.target
        if (ai && target != null) {
            CombatController.decide(target)
        }
    }

    /**
     * Handles additional movement mechanics like swimming and jumping
     *
     * @param event Movement input event to modify
     */
    override fun handleMovementAssist(event: MovementInputEvent, context: CombatContext) {
        if (ai && steer(event, context)) {
            return
        }
        super.handleMovementAssist(event, context)

        val contextAllowsJump = context.combatTarget != null && context.combatTarget.outOfDistance
            && !context.combatTarget.outOfDanger
        val goal = calculateGoalPosition(context) ?: return
        val leaderAllowsJump = LeaderFollower.running && player.position().distanceTo(goal) > LeaderFollower.radius

        if (contextAllowsJump || leaderAllowsJump) {
            event.jump = true
        }
    }

    /** Within reach the model presses the keys, and jumps, the way the players it learned from did. */
    private fun steer(event: MovementInputEvent, context: CombatContext): Boolean {
        val target = context.combatTarget?.entity as? LivingEntity ?: return false
        if (!KillAuraAi.canAdjustMovement()) {
            return false
        }
        val live = KillAuraAi.live(target) ?: return false
        if (live.jump) {
            event.jump = true
        }
        event.directionalInput = KillAuraAi.keys(live, target) ?: return false
        return true
    }

    /**
     * Also in the air: players stop sprinting mid jump. Only the sprint key is pressed, so vanilla still refuses
     * to sprint in shallow water or while blocking; stopping is always fine. Runs after the engine's own sprint
     * and leaves KillAura's critical hit sprint stop alone.
     */
    @Suppress("unused")
    private val aiSprintHandler = handler<SprintEvent>(priority = MODEL_STATE) { event ->
        if (!ai || !KillAuraAi.canAdjustMovement(airborne = true) || ModuleKillAura.shouldBlockSprinting) {
            return@handler
        }
        val target = targetTracker.target ?: return@handler
        KillAuraAi.live(target)?.let { KillAuraAi.sprint(event, it) }
    }

    /**
     * Gets rotation based on movement and target
     *
     * @return Movement rotation or null if no target
     */
    override fun getMovementRotation(): Rotation {
        val movementRotation = super.getMovementRotation()
        val movementPitch = targetTracker.target?.let { entity ->
            Rotation.lookingAt(point = entity.boundingBox.center, from = player.eyePosition).pitch
        } ?: return movementRotation

        return movementRotation.copy(pitch = movementPitch)
    }

    private fun calculateLeaderGoalPosition(leaderPosition: Vec3, playerPosition: Vec3): Vec3 {
        return (-180..180 step 45)
            .mapNotNull { yaw ->
                val rotation = Rotation(yaw = yaw.toFloat(), pitch = 0.0F)
                val position = leaderPosition.fma(LeaderFollower.radius.toDouble(), rotation.directionVector)
                ModuleDebug.debugGeometry(
                    this,
                    "Possible Position $yaw",
                    ModuleDebug.DebuggedPoint(position, Color4b.MAGENTA)
                )
                position
            }
            .minByOrNull { it.distanceToSqr(playerPosition) } ?: leaderPosition
    }

    private fun calculateRunawayPosition(context: CombatContext, combatTarget: CombatTarget): Vec3 {
        return context.playerPosition.fma(
            combatTarget.range.toDouble(), combatTarget.requiredTargetRotation.directionVector
        )
    }

    private fun calculateAttackPosition(context: CombatContext, combatTarget: CombatTarget): Vec3 {
        val target = combatTarget.entity
        val targetLookPosition = target.position().fma(
            combatTarget.range.toDouble(), combatTarget.targetRotation.directionVector
        )

        return (-180..180 step 10)
            .mapNotNull { yaw ->
                val rotation = Rotation(yaw = yaw.toFloat(), pitch = 0.0F)
                val position = target.position().fma(combatTarget.range.toDouble(), rotation.directionVector)

                // Check if this point collides with a block
                if (player.doesCollideAt(position)) {
                    return@mapNotNull null
                }

                val isInAngle = abs(rotation.rotationDeltaTo(combatTarget.targetRotation).deltaYaw) <= dangerousYawDiff
                ModuleDebug.debugGeometry(
                    this,
                    "Possible Position $yaw",
                    ModuleDebug.DebuggedPoint(position, if (!isInAngle) Color4b.GREEN else Color4b.RED)
                )

                if (isInAngle) null else position
            }
            .sortedBy { pos -> pos.distanceToSqr(targetLookPosition) }
            .minByOrNull { pos -> pos.distanceToSqr(context.playerPosition) }
            ?: targetLookPosition
    }

}
