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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import it.unimi.dsi.fastutil.ints.IntCollection
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleIdPredict
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.utils.findClosestPointOnBlockInLineWithCrystal
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntitiesReturnCrystal
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.network.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.ccbluex.liquidbounce.utils.raytracing.isFacingBlock
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.max

internal class CrystalAuraActionGeneration {
    private var generation = 0

    @Synchronized
    fun next(): Int = ++generation

    @Synchronized
    fun invalidate() {
        generation++
    }

    @Synchronized
    fun isCurrent(token: Int): Boolean = token == generation

    @Synchronized
    fun runIfCurrent(token: Int, action: () -> Boolean): Boolean = token == generation && action()
}

internal fun completeCrystalAuraDispatch(dispatch: () -> Boolean, afterDispatch: () -> Unit): Boolean {
    if (!dispatch()) {
        return false
    }

    afterDispatch()
    return true
}

object SubmoduleCrystalPlacer : ToggleableValueGroup(ModuleCrystalAura, "Place", true) {

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)
    private val switchMode by enumChoice("Switch", SwitchMode.SILENT)
    val oldVersion by boolean("1_12_2", false)
    private val delay by int("Delay", 0, 0..1000, "ms")
    private val dedupPerTick by boolean("DedupPerTick", false)
    val range by float("Range", 4.5f, 1f..6f).onChanged {
        CrystalAuraPlaceTargetFactory.updateSphere()
    }

    val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChanged {
        CrystalAuraPlaceTargetFactory.updateSphere()
    }

    /**
     * Only place crystals above the block.
     * Outdated setting.
     * Using this is normally not recommended.
     */
    val onlyAbove by boolean("OnlyAbove", false)

    private val sequenced by boolean("Sequenced", false)

    // only applies without OnlyAbove
    internal val notFacingAway by boolean("NotFacingAway", false)

    // only applies without OnlyAbove
    private val jitter by boolean("Jitter", false)

    val placementRenderer = tree(
        PlacementRenderer( // TODO slide
            "TargetRendering",
            true,
            ModuleCrystalAura,
            clump = false,
            defaultColor = Color4b.WHITE.with(a = 90)
        )
    )

    private val chronometer = Chronometer()
    private val actions = CrystalAuraActionGeneration()

    @Volatile
    internal var pendingPlacePos: BlockPos? = null
        private set

    @Volatile
    internal var pendingPlaceTick: Long = 0L
        private set

    // this is shit, but I can't think of a better way right now.
    // the problem with only one rotation is
    // that when the ca switches between two players very fast and one place is invalid it would fail
    private var previousRotations = ArrayDeque<Pair<Rotation, Rotation>>(2)

    @Suppress("LongMethod", "CognitiveComplexMethod", "ReturnCount")
    fun tick(excludeIds: IntCollection? = null) {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        val generation = actions.next()

        // if we don't have crystals, we don't need to run the method
        getSlot() ?: return

        CrystalAuraPlaceTargetFactory.updateTarget(excludeIds)

        removeFromRenderer()

        val targetPos = CrystalAuraPlaceTargetFactory.placementTarget ?: return
        val enemyTargetId = ModuleCrystalAura.targetTracker.target?.id ?: return
        if (dedupPerTick && pendingPlacePos == targetPos && pendingPlaceTick == world.gameTime) {
            return
        }

        val notSameRotation = RotationManager.serverRotation != previousRotations.lastOrNull()?.first
        val rotationsNotToMatch = if (notSameRotation && jitter) {
            previousRotations.map { it.second }
        } else {
            null
        }

        var side = Direction.UP
        val rotation = if (onlyAbove) {
            raytraceUpperBlockSide(
                player.eyePosition,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
                rotationsNotToMatch = rotationsNotToMatch
            )
        } else {
            val data = findClosestPointOnBlockInLineWithCrystal(
                player.eyePosition,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
                notFacingAway,
                rotationsNotToMatch
            ) ?: return
            side = data.second

            data.first
        } ?: return

        if (ModuleCrystalAura.rotationMode.activeMode is NoRotationMode) {
            raytraceBlock(
                getMaxRange().toDouble(),
                rotation.rotation,
                targetPos,
                targetPos.stateOrEmpty
            ) ?: return
        }

        addToRenderer()
        updatePrevious(rotation)
        queuePlacing(generation, enemyTargetId, rotation, targetPos, side, excludeIds)
    }

    private fun queuePlacing(
        generation: Int,
        enemyTargetId: Int,
        rotation: RotationWithVector,
        targetPos: BlockPos,
        side: Direction,
        excludeIds: IntCollection?
    ) {
        ModuleCrystalAura.rotationMode.activeMode.rotate(rotation.rotation, isFinished = {
            isCurrentAction(generation) && findBlockHitResult(rotation, targetPos) != null
        }, onFinished = {
            if (mc.isSameThread) {
                place(generation, enemyTargetId, rotation, targetPos, side, excludeIds)
            } else {
                mc.execute { place(generation, enemyTargetId, rotation, targetPos, side, excludeIds) }
            }
        })
    }

    @Suppress("CognitiveComplexMethod", "ComplexCondition")
    private fun place(
        generation: Int,
        enemyTargetId: Int,
        rotation: RotationWithVector,
        targetPos: BlockPos,
        side: Direction,
        excludeIds: IntCollection?
    ) {
        actions.runIfCurrent(generation) action@{
            if (!running || !CrystalAuraTriggerer.running ||
                ModuleCrystalAura.targetTracker.target?.id != enemyTargetId ||
                CrystalAuraPlaceTargetFactory.placementTarget != targetPos ||
                dedupPerTick && pendingPlacePos == targetPos && pendingPlaceTick == world.gameTime ||
                !chronometer.hasAtLeastElapsed(delay.toLong())
            ) {
                return@action false
            }

            val cache = CandidateCache(targetPos)
            if (!cache.canPlace) {
                return@action false
            }

            val aboveState = cache.up.stateOrEmpty
            if (!aboveState.isAir && (!oldVersion || !aboveState.canBeReplaced())) {
                return@action false
            }

            if (oldVersion) {
                val secondAboveState = targetPos.above(2).stateOrEmpty
                if (!secondAboveState.isAir && !secondAboveState.canBeReplaced()) {
                    return@action false
                }
            }

            val expectedCrystal = if (oldVersion) FULL_BOX.setMaxY(2.0) else FULL_BOX
            val blocked = cache.up.isBlockedByEntitiesReturnCrystal(
                box = expectedCrystal,
                excludeIds = excludeIds,
                buildingOnly = false
            )
            if (blocked.keyBoolean()) {
                return@action false
            }

            val hitResult = findBlockHitResult(rotation, targetPos) ?: return@action false
            val eyePos = player.eyePosition
            val visible = player.isFacingBlock(eyePos, hitResult.location, targetPos)
            val allowedRange = if (visible) range else wallsRange
            if (!hitResult.location.closerThan(eyePos, allowedRange.toDouble())) {
                return@action false
            }

            CrystalAuraDamageOptions.approximateExplosionDamage(
                targetPos.topCenter,
                CrystalAuraDamageOptions.RequestingSubmodule.PLACE,
                useCache = false
            ) ?: return@action false

            val slot = getSlot() ?: return@action false
            completeCrystalAuraDispatch(
                dispatch = {
                    player.clickBlockWithSlot(hitResult.withDirection(side), slot, swingMode, switchMode, sequenced)
                },
                afterDispatch = {
                    pendingPlaceTick = world.gameTime
                    pendingPlacePos = targetPos
                    SubmoduleIdPredict.run(targetPos)
                    chronometer.reset()
                }
            )
        }
    }

    private fun findBlockHitResult(rotation: RotationWithVector, targetPos: BlockPos): BlockHitResult? {
        val rayRotation = if (ModuleCrystalAura.rotationMode.activeMode is NoRotationMode) {
            rotation.rotation
        } else {
            RotationManager.serverRotation
        }

        return raytraceBlock(
            getMaxRange().toDouble(),
            rayRotation,
            targetPos,
            targetPos.stateOrEmpty
        )?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == targetPos }
    }

    private fun isCurrentAction(generation: Int) = actions.isCurrent(generation)

    override fun onDisabled() {
        actions.invalidate()
        pendingPlacePos = null
        pendingPlaceTick = 0L
        previousRotations.clear()
    }

    private fun updatePrevious(rotation: RotationWithVector) {
        if (previousRotations.size == 2) {
            previousRotations.removeFirst()
        }

        // stores the mutable rotation and a copy to compare with the produced rotations
        previousRotations.addLast(rotation.rotation to rotation.rotation.copy())
    }

    private fun addToRenderer() = with(CrystalAuraPlaceTargetFactory) {
        if (placementTarget == previousTarget) {
            return@with
        }

        placementTarget?.let {
            mc.execute { placementRenderer.addBlock(it) }
        }
    }

    private fun removeFromRenderer() = with(CrystalAuraPlaceTargetFactory) {
        if (placementTarget == previousTarget) {
            return@with
        }

        previousTarget?.let {
            mc.execute { placementRenderer.removeBlock(it) }
        }
    }

    private fun getSlot(): Int? {
        return Slots.OffhandWithHotbar.findClosestSlot(Items.END_CRYSTAL)?.inventorySlot
    }

    fun getMaxRange() = max(range, wallsRange)

}
