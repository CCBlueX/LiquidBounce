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
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleIdPredict
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.utils.findClosestPointOnBlockInLineWithCrystal
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.network.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.raytracing.isFacingBlock
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.max

object SubmoduleCrystalPlacer : ToggleableValueGroup(ModuleCrystalAura, "Place", true) {

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)
    private val switchMode by enumChoice("Switch", SwitchMode.SILENT)
    val oldVersion by boolean("1_12_2", false)
    private val delay by int("Delay", 0, 0..1000, "ms")
    val range by float("Range", 4.5f, 1f..6f).onChanged {
        CrystalAuraPlaceTargetFactory.updateSphere()
    }

    val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChanged {
        CrystalAuraPlaceTargetFactory.updateSphere()
    }

    /**
     * Prevents duplicate UseItemOn packets on the same block within a single tick before crystal spawn confirmation.
     */
    private val dedupPerTick by boolean("DedupPerTick", true)

    /**
     * Only place crystals above the block.
     * Outdated setting.
     * Using this is normally not recommended.
     */
    val onlyAbove by boolean("OnlyAbove", false)

    private val sequenced by boolean("Sequenced", false)

    // only applies without OnlyAbove
    private val notFacingAway by boolean("NotFacingAway", false)

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

    // this is shit, but I can't think of a better way right now.
    // the problem with only one rotation is
    // that when the ca switches between two players very fast and one place is invalid it would fail
    private val previousRotations = ArrayDeque<Pair<Rotation, Rotation>>(2)

    @Volatile
    var pendingPlacePos: BlockPos? = null

    @Volatile
    var pendingPlaceTick: Int = -1

    override fun onDisabled() {
        pendingPlacePos = null
        pendingPlaceTick = -1
        synchronized(previousRotations) {
            previousRotations.clear()
        }
    }

    @Suppress("LongMethod", "CognitiveComplexMethod")
    fun tick(excludeIds: IntCollection? = null) {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        // if we don't have crystals, we don't need to run the method
        getSlot() ?: return

        CrystalAuraPlaceTargetFactory.updateTarget(excludeIds)

        removeFromRenderer()

        val targetPos = CrystalAuraPlaceTargetFactory.placementTarget ?: return
        if (dedupPerTick && pendingPlacePos == targetPos && pendingPlaceTick == player.tickCount) {
            return
        }

        val rotationsNotToMatch = synchronized(previousRotations) {
            val notSameRotation = RotationManager.serverRotation != previousRotations.lastOrNull()?.first
            if (notSameRotation && jitter) {
                previousRotations.map { it.second }
            } else {
                null
            }
        }

        var side = Direction.UP
        val rotation = if (onlyAbove) {
            raytraceUpperBlockSide(
                eyes = player.eyePosition,
                range = range.toDouble(),
                wallsRange = wallsRange.toDouble(),
                expectedTarget = targetPos,
                rotationsNotToMatch = rotationsNotToMatch
            )
        } else {
            val data = findClosestPointOnBlockInLineWithCrystal(
                eyes = player.eyePosition,
                range = range.toDouble(),
                wallsRange = wallsRange.toDouble(),
                expectedTarget = targetPos,
                notFacingAway = notFacingAway,
                rotationsNotToMatch = rotationsNotToMatch
            ) ?: return
            side = data.second

            data.first
        } ?: return

        addToRenderer()
        updatePrevious(rotation)
        queuePlacing(rotation, targetPos, side)
    }

    private fun resolveValidBlockHit(checkRotation: Rotation, targetPos: BlockPos): BlockHitResult? {
        val hit = raytraceBlock(
            getMaxRange().toDouble(),
            checkRotation,
            targetPos,
            targetPos.stateOrEmpty
        ) ?: return null

        if (hit.type != HitResult.Type.BLOCK || hit.blockPos != targetPos) {
            return null
        }

        val eyes = player.eyePosition
        val distSq = eyes.distanceToSqr(hit.location)
        val visible = player.isFacingBlock(eyes, hit.location, targetPos, hit.direction)
        val allowedRangeSq = if (visible) range.toDouble().sq() else wallsRange.toDouble().sq()
        return hit.takeIf { distSq <= allowedRangeSq }
    }

    private fun queuePlacing(rotation: RotationWithVector, targetPos: BlockPos, side: Direction) {
        val noRotationHit = if (ModuleCrystalAura.rotationMode.activeMode is NoRotationMode) {
            resolveValidBlockHit(rotation.rotation, targetPos) ?: return
        } else {
            null
        }

        var verifiedHit: BlockHitResult? = noRotationHit

        ModuleCrystalAura.rotationMode.activeMode.rotate(rotation.rotation, isFinished = {
            verifiedHit = resolveValidBlockHit(RotationManager.serverRotation, targetPos)
            verifiedHit != null
        }, onFinished = {
            if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                return@rotate
            }

            if (dedupPerTick && pendingPlacePos == targetPos && pendingPlaceTick == player.tickCount) {
                return@rotate
            }

            val finalHit = verifiedHit ?: resolveValidBlockHit(rotation.rotation, targetPos) ?: return@rotate

            player.clickBlockWithSlot(
                rayTraceResult = finalHit.withDirection(side),
                slot = getSlot() ?: return@rotate,
                swingMode = swingMode,
                switchMode = switchMode,
                sequenced = sequenced
            )

            pendingPlacePos = targetPos
            pendingPlaceTick = player.tickCount

            SubmoduleIdPredict.run(targetPos)

            chronometer.reset()
        })
    }

    private fun updatePrevious(rotation: RotationWithVector) {
        synchronized(previousRotations) {
            if (previousRotations.size == 2) {
                previousRotations.removeFirst()
            }

            // stores the mutable rotation and a copy to compare with the produced rotations
            previousRotations.addLast(rotation.rotation to rotation.rotation.copy())
        }
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
