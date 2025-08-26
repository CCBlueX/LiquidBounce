/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsConfigurable.KillAuraRotationTiming.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsConfigurable.KillAuraRotationTiming.SNAP
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.dealWithFakeSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.failedHits
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.renderFailedHits
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.ccbluex.liquidbounce.utils.inventory.isInContainerScreen
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import kotlin.math.pow

/**
 * KillAura module
 *
 * Automatically attacks enemies.
 */
@Suppress("MagicNumber")
object ModuleKillAura : ClientModule("KillAura", Category.COMBAT) {

    // Attack speed
    val clickScheduler = tree(KillAuraClicker)

    // Range
    internal val range by float("Range", 4.2f, 1f..8f)
    internal val wallRange by float("WallRange", 3f, 0f..8f).onChange { wallRange ->
        if (wallRange > range) {
            range
        } else {
            wallRange
        }
    }

    private val scanExtraRange by floatRange("ScanExtraRange", 2.0f..3.0f, 0.0f..7.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()

    // Target
    val targetTracker = tree(KillAuraTargetTracker)

    // Rotation
    private val rotations = tree(KillAuraRotationsConfigurable)

    private val pointTracker = tree(PointTracker())

    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")

    private val requirementsMet
        get() = requires.all { it.meets() }

    // Bypass techniques
    internal val raycast by enumChoice("Raycast", TRACE_ALL)
    private val criticalsSelectionMode by enumChoice("Criticals", CriticalsSelectionMode.SMART)
    private val keepSprint by boolean("KeepSprint", true)

    // Inventory Handling
    internal val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    internal val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)

    init {
        tree(KillAuraAutoBlock)
    }

    // Target rendering
    private val targetRenderer = tree(WorldTargetRenderer(this))

    init {
        tree(KillAuraFailSwing)
        tree(KillAuraFightBot)
    }

    override fun onDisabled() {
        targetTracker.reset()
        failedHits.clear()
        KillAuraAutoBlock.stopBlocking()
        KillAuraNotifyWhenFail.failedHitsIncrement = 0
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderTarget(matrixStack, event.partialTicks)
        renderFailedHits(matrixStack)
    }

    private fun renderTarget(matrixStack: MatrixStack, partialTicks: Float) {
        val target = targetTracker.target
            ?.takeIf { targetRenderer.enabled }
            ?.takeIf { !ModuleElytraTarget.isSameTargetRendering(it) }
            ?: return

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen =
            InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen

        val shouldResetTarget = player.isSpectator || player.isDead || !requirementsMet

        if (isInInventoryScreen && !ignoreOpenInventory || shouldResetTarget) {
            // Reset current target
            targetTracker.reset()
            return@handler
        }

        // Update current target tracker to make sure you attack the best enemy
        updateTarget()

        // Update Auto Weapon
        ModuleAutoWeapon.prepare(targetTracker.target)
    }

    @Suppress("unused")
    private val gameHandler = tickHandler {
        if (player.isDead || player.isSpectator) {
            return@tickHandler
        }

        // Check if there is target to attack
        val target = targetTracker.target

        if (CombatManager.shouldPauseCombat) {
            KillAuraAutoBlock.stopBlocking()
            return@tickHandler
        }

        if (target == null) {
            val hasUnblocked = KillAuraAutoBlock.stopBlocking()

            // Deal with fake swing when there is no target
            if (KillAuraFailSwing.enabled && requirementsMet) {
                if (hasUnblocked) {
                    waitTicks(KillAuraAutoBlock.currentTickOff)
                }
                dealWithFakeSwing(this, null)
            }
            return@tickHandler
        }

        // Check if the module should (not) continue after the blocking state is updated
        if (!requirementsMet) {
            return@tickHandler
        }

        val rotation = (if (rotations.rotationTiming == ON_TICK) {
            getSpot(target, range.toDouble(), PointTracker.AimSituation.FOR_NOW)?.rotation
        } else {
            null
        } ?: RotationManager.currentRotation ?: player.rotation).normalize()

        val crosshairTarget = when {
            raycast != TRACE_NONE -> {
                raytraceEntity(range.toDouble(), rotation, filter = {
                    when (raycast) {
                        TRACE_ONLYENEMY -> it.shouldBeAttacked()
                        TRACE_ALL -> true
                        else -> false
                    }
                })?.entity ?: target
            }
            else -> target
        }

        if (crosshairTarget is LivingEntity && crosshairTarget.shouldBeAttacked() && crosshairTarget != target) {
            targetTracker.target = crosshairTarget
        }

        attackTarget(this, crosshairTarget, rotation)
    }

    val shouldBlockSprinting
        get() = !ModuleElytraTarget.running
            && criticalsSelectionMode.shouldStopSprinting(clickScheduler, targetTracker.target)

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent> { event ->
        if (shouldBlockSprinting && (event.source == SprintEvent.Source.MOVEMENT_TICK ||
                event.source == SprintEvent.Source.INPUT)) {
            event.sprint = false
        }
    }

    @Suppress("CognitiveComplexMethod", "CyclomaticComplexMethod")
    private suspend fun attackTarget(sequence: Sequence, target: Entity, rotation: Rotation) {
        // Make it seem like we are blocking
        KillAuraAutoBlock.makeSeemBlock()

        // Are we actually facing the [chosenEntity]
        val isFacingEnemy = facingEnemy(toEntity = target, rotation = rotation,
            range = range.toDouble(),
            wallsRange = wallRange.toDouble()) || ModuleElytraTarget.canIgnoreKillAuraRotations

        ModuleDebug.debugParameter(ModuleKillAura, "Is Facing Enemy", isFacingEnemy)
        ModuleDebug.debugParameter(ModuleKillAura, "Rotation", rotation)
        ModuleDebug.debugParameter(ModuleKillAura, "Target", target.nameForScoreboard)

        // Check if our target is in range, otherwise deal with auto block
        if (!isFacingEnemy) {
            if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.onScanRange &&
                player.squaredBoxedDistanceTo(target) <= (range + currentScanExtraRange).pow(2)) {
                KillAuraAutoBlock.startBlocking()
                return
            }

            // Make sure we are not blocking
            val hasUnblocked = KillAuraAutoBlock.stopBlocking()

            // Deal with fake swing
            if (KillAuraFailSwing.enabled) {
                if (hasUnblocked) {
                    sequence.waitTicks(KillAuraAutoBlock.currentTickOff)
                }

                dealWithFakeSwing(sequence, target)
            }
            return
        }

        ModuleDebug.debugParameter(ModuleKillAura, "Valid Rotation", rotation)

        // Attack enemy, according to the attack scheduler
        if (clickScheduler.isClickTick && validateAttack(target)) {
            clickScheduler.attack(sequence, rotation) {
                // On each click, we check if we are still ready to attack
                if (!validateAttack(target)) {
                    return@attack false
                }

                // Attack enemy
                target.attack(true, keepSprint && !shouldBlockSprinting)
                currentScanExtraRange = scanExtraRange.random()
                KillAuraNotifyWhenFail.failedHitsIncrement = 0

                GenericDebugRecorder.recordDebugInfo(ModuleKillAura, "attackEntity", JsonObject().apply {
                    add("player", GenericDebugRecorder.debugObject(player))
                    add("targetPos", GenericDebugRecorder.debugObject(target))
                })

                true
            }
        } else if (KillAuraAutoBlock.currentTickOff > 0 && clickScheduler.willClickAt(KillAuraAutoBlock.currentTickOff)
            && KillAuraAutoBlock.shouldUnblockToHit) {
            KillAuraAutoBlock.stopBlocking(pauses = true)
        } else {
            KillAuraAutoBlock.startBlocking()
        }
    }

    private fun updateTarget() {
        // Determine aim situation based on click scheduler
        val situation = when {
            clickScheduler.isClickTick || clickScheduler.willClickAt(1)
                -> PointTracker.AimSituation.FOR_NEXT_TICK
            else -> PointTracker.AimSituation.FOR_THE_FUTURE
        }
        ModuleDebug.debugParameter(ModuleKillAura, "AimSituation", situation)

        // Calculate maximum range based on enemy distance
        val maximumRange = if (targetTracker.closestSquaredEnemyDistance > range.pow(2)) {
            range + currentScanExtraRange
        } else {
            range
        }

        ModuleDebug.debugParameter(ModuleKillAura, "Maximum Range", maximumRange)
        ModuleDebug.debugParameter(ModuleKillAura, "Range", range)
        val squaredMaxRange = maximumRange.pow(2)
        val squaredNormalRange = range.pow(2)

        // Find suitable target
        val target = targetTracker.targets()
            .filter { entity -> entity.squaredBoxedDistanceTo(player) <= squaredMaxRange }
            .sortedBy { entity -> if (entity.squaredBoxedDistanceTo(player) <= squaredNormalRange) 0 else 1 }
            .firstOrNull { entity -> processTarget(entity, maximumRange, situation) }

        if (target != null) {
            targetTracker.target = target
        } else if (KillAuraFightBot.enabled) {
            KillAuraFightBot.updateTarget()

            RotationManager.setRotationTarget(
                rotations.toRotationTarget(
                    KillAuraFightBot.getMovementRotation(),
                    considerInventory = !ignoreOpenInventory
                ),
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = ModuleKillAura
            )
        } else {
            targetTracker.reset()
        }
    }

    @Suppress("ReturnCount")
    private fun processTarget(
        entity: LivingEntity,
        maximumRange: Float,
        situation: PointTracker.AimSituation
    ): Boolean {
        val (rotation, _) = getSpot(entity, maximumRange.toDouble(), situation) ?: return false
        val ticks = rotations.calculateTicks(rotation)
        ModuleDebug.debugParameter(ModuleKillAura, "Rotation Ticks", ticks)

        when (rotations.rotationTiming) {

            // If our click scheduler is not going to click the moment we reach the target,
            // we should not start aiming towards the target just yet.
            SNAP -> if (!clickScheduler.willClickAt(ticks.coerceAtLeast(1))) {
                return true
            }

            // [ON_TICK] will always instantly aim onto the target on attack, however, if
            // our rotation is unable to be ready in time, we can at least start aiming towards
            // the target.
            ON_TICK -> if (ticks <= 1) {
                return true
            }

            else -> {
                // Continue with regular aiming
            }
        }

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(
                rotation,
                entity,
                considerInventory = !ignoreOpenInventory
            ),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleKillAura
        )

        return true
    }

    /**
     * Get the best spot to attack the entity
     *
     * @param entity The entity to attack
     * @param range The range to attack the entity (NOT SQUARED)
     * @param situation The aim situation we are in
     *  - [PointTracker.AimSituation.FOR_NOW] if we are going to attack the entity on the current tick (ON_TICK)
     *  - [PointTracker.AimSituation.FOR_THE_FUTURE] if we are going to attack the entity in the future
     *  - [PointTracker.AimSituation.FOR_NEXT_TICK] if we are going to attack the entity on the next tick
     *
     *  @return The best spot to attack the entity
     */
    private fun getSpot(entity: LivingEntity, range: Double,
                        situation: PointTracker.AimSituation): RotationWithVector? {
        val point = pointTracker.gatherPoint(
            entity,
            situation
        )

        val eyes = point.fromPoint
        val nextPoint = point.toPoint

        ModuleDebug.debugGeometry(this, "Box",
            ModuleDebug.DebuggedBox(point.box, Color4b.RED.with(a = 60)))
        ModuleDebug.debugGeometry(this, "CutOffBox",
            ModuleDebug.DebuggedBox(point.cutOffBox, Color4b.GREEN.with(a = 90)))
        ModuleDebug.debugGeometry(this, "Point", ModuleDebug.DebuggedPoint(nextPoint, Color4b.WHITE))

        val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, nextPoint)

        // find best spot
        val spot = raytraceBox(
            eyes, point.cutOffBox,
            // Since [range] is squared, we need to square root
            range = range,
            wallsRange = wallRange.toDouble(),
            rotationPreference = rotationPreference
        ) ?: raytraceBox(
            eyes, point.box,
            range = range,
            wallsRange = wallRange.toDouble(),
            rotationPreference = rotationPreference
        )

        return if (spot == null && rotations.aimThroughWalls) {
            val throughSpot = raytraceBox(
                eyes, point.cutOffBox,
                // Since [range] is squared, we need to square root
                range = range,
                wallsRange = range,
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                eyes, point.box,
                range = range,
                wallsRange = range,
                rotationPreference = rotationPreference
            )

            throughSpot
        } else {
            spot
        }
    }

    /**
     * Check if we can attack the target at the current moment
     */
    internal fun validateAttack(target: Entity? = null): Boolean {
        val criticalHit = target == null || player.isGliding || criticalsSelectionMode.isCriticalHit(target)
        val isInInventoryScreen = isInventoryOpen || isInContainerScreen

        return criticalHit && !(isInInventoryScreen && !ignoreOpenInventory && !simulateInventoryClosing)
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"),
        TRACE_ALL("All")
    }

}
