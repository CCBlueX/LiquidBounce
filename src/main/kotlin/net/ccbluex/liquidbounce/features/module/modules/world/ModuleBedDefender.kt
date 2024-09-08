/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.BED_BLOCKS
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object ModuleBedDefender : Module("BedDefender", category = Category.WORLD) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val maxLayers by int("MaxLayers", 5, 1..5)

    private val isSelfBedMode = choices<IsSelfBedChoice>("SelfBed", { it.choices[0] }, { arrayOf(
        IsSelfBedNoneChoice(it),
        IsSelfBedColorChoice(it),
        IsSelfBedSpawnLocationChoice(it)
    )})

    private val rotations = tree(RotationsConfigurable(this))

    private val requiresSneak by boolean("RequiresSneak", false)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val ignoreUsingItem by boolean("IgnoreUsingItem", true)

    // todo: sort by hardness
    private val slotsToUse
        get() = HOTBAR_SLOTS
            .filter { slot -> !ScaffoldBlockItemSelection.isBlockUnfavourable(slot.itemStack) }

    private var target: BlockPos? = null

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        this.target = null

        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        if (!ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        if (requiresSneak && !player.isSneaking) {
            return@handler
        }

        val slotsToUse = slotsToUse
        if (slotsToUse.isEmpty()) {
            return@handler
        }

        val eyesPos = player.eyes
        val bedBlocks = searchBlocksInCuboid(range + 1, eyesPos) { pos, state ->
            getNearestPoint(eyesPos, Box.enclosing(pos, pos.add(1, 1, 1))).distanceTo(eyesPos) <= range
            && state.block in BED_BLOCKS
        }

        if (bedBlocks.isEmpty()) {
            return@handler
        }

        // With [getPlacementPositions] we should be fine with only one bed block
        val (blockPos, state) = bedBlocks.minByOrNull { (blockPos, _) -> blockPos.getSquaredDistance(eyesPos) }
            ?: return@handler

        val placementPositions = getPlacementPositions(blockPos, state)
            .sortedBy { pos -> pos.getSquaredDistance(blockPos) }
        if (placementPositions.isEmpty()) {
            return@handler
        }

        ModuleDebug.debugGeometry(this, "PlacementPosition", ModuleDebug.DebugCollection(
            placementPositions.map { pos -> ModuleDebug.DebuggedBox(Box.enclosing(pos, pos.add(1, 1, 1)), Color4b.WHITE.alpha(20)) }
        ))
        for (position in placementPositions) {
            val (rotation, _) = raytracePlaceBlock(eyesPos, position, range.toDouble(), 0.0)
                ?: continue

            RotationManager.aimAt(
                rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                provider = this@ModuleBedDefender,
                priority = Priority.NOT_IMPORTANT
            )
            target = position


        }


    }

    @Suppress("unused")
    private val repeatable = repeatable {
        val target = target ?: return@repeatable

        // Check if we are facing the target
        val hitResult = raycast(range = range.toDouble()) ?: return@repeatable
        if (hitResult.type != HitResult.Type.BLOCK || hitResult.blockPos != target) {
            return@repeatable
        }

        val itemStackInHand = player.mainHandStack
        val itemStackInOffHand = player.offHandStack
        if (ScaffoldBlockItemSelection.isBlockUnfavourable(itemStackInHand)
            && ScaffoldBlockItemSelection.isBlockUnfavourable(itemStackInOffHand)) {
            return@repeatable
        }

        // Place block
        doPlacement(hitResult)
    }

    private fun getPlacementPositions(pos: BlockPos, state: BlockState): List<BlockPos> {
        val pos2 = pos.offset(BedBlock.getOppositePartDirection(state))
        if (pos2.getState()?.block !in BED_BLOCKS) {
            return emptyList()
        }

        return (pos.getPlacementPositionsAround() + pos2.getPlacementPositionsAround())
            .distinct()
    }

    private fun BlockPos.getPlacementPositionsAround(): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()

        for (offsetX in x - maxLayers..<x + maxLayers) {
            for (offsetZ in z - maxLayers..<z + maxLayers) {
                for (offsetY in y..<y + maxLayers) {
                    val blockPos = BlockPos(offsetX, offsetY, offsetZ)
                    val blockState = blockPos.getState() ?: continue

                    if (!blockState.isAir) {
                        continue
                    }

                    positions += blockPos
                }
            }
        }

        return positions
    }




}
