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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SubmoduleIdPredict
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.utils.findClosestPointOnBlockInLineWithCrystal
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import kotlin.math.max

object SubmoduleCrystalPlacer : ToggleableConfigurable(ModuleCrystalAura, "Place", true) {

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
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
    private var blockHitResult: BlockHitResult? = null

    // this is shit, but i can't think of a better way right now
    // the problem with only one rotation is
    // that when the ca switches between two players very fast and one place is invalid it would fail
    private var previousRotations = ArrayDeque<Pair<Rotation, Rotation>>(2)

    @Suppress("LongMethod", "CognitiveComplexMethod")
    fun tick(excludeIds: IntArray? = null) {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        // if we don't have crystals, we don't need to run the method
        getSlot() ?: return

        CrystalAuraPlaceTargetFactory.updateTarget(excludeIds)

        removeFromRenderer()

        val targetPos = CrystalAuraPlaceTargetFactory.placementTarget ?: return

        val notSameRotation = RotationManager.serverRotation != previousRotations.lastOrNull()?.first
        val rotationsNotToMatch = if (notSameRotation && jitter) {
            previousRotations.map { it.second }
        } else {
            null
        }

        var side = Direction.UP
        val rotation = if (onlyAbove) {
            raytraceUpperBlockSide(
                player.eyePos,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
                rotationsNotToMatch = rotationsNotToMatch
            )
        } else {
            val data = findClosestPointOnBlockInLineWithCrystal(
                player.eyePos,
                range.toDouble(),
                wallsRange.toDouble(),
                targetPos,
                notFacingAway,
                rotationsNotToMatch
            ) ?: return
            side = data.second

            data.first
        } ?: return

        if (ModuleCrystalAura.rotationMode.activeChoice is NoRotationMode) {
            blockHitResult = raytraceBlock(
                getMaxRange().toDouble(),
                rotation.rotation,
                targetPos,
                targetPos.getState()!!
            ) ?: return
        }

        addToRenderer()
        updatePrevious(rotation)
        queuePlacing(rotation, targetPos, side)
    }

    private fun queuePlacing(rotation: RotationWithVector, targetPos: BlockPos, side: Direction) {
        ModuleCrystalAura.rotationMode.activeChoice.rotate(rotation.rotation, isFinished = {
            blockHitResult = raytraceBlock(
                getMaxRange().toDouble(),
                RotationManager.serverRotation,
                targetPos,
                targetPos.getState()!!
            ) ?: return@rotate false

            return@rotate blockHitResult!!.type == HitResult.Type.BLOCK && blockHitResult!!.blockPos == targetPos
        }, onFinished = {
            if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                return@rotate
            }

            clickBlockWithSlot(
                player,
                blockHitResult?.withSide(side) ?: return@rotate,
                getSlot() ?: return@rotate,
                swingMode,
                switchMode,
                sequenced
            )

            SubmoduleIdPredict.run(targetPos)

            chronometer.reset()
        })
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
        return Slots.OffhandWithHotbar.findClosestSlot(Items.END_CRYSTAL)?.hotbarSlotForServer
    }

    fun getMaxRange() = max(range, wallsRange)

}
