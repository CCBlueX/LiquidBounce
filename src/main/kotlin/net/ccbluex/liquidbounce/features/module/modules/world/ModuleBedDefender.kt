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
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i
import kotlin.math.max

object ModuleBedDefender : Module("BedDefender", category = Category.WORLD) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallRange by float("WallRange", 4.5f, 1f..6f)
    private val maxLayers by int("MaxLayers", 1, 1..5)
    private val placeCooldown by int("PlaceCooldown", 4, 0..10, "ticks")

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

    private var placementTarget: BlockPlacementTarget? = null

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        this.placementTarget = null

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
            && (state.block as? BedBlock)?.let {
                bedBlock -> (isSelfBedMode.activeChoice.isSelfBed(bedBlock, pos))
            } == true
        }

        if (bedBlocks.isEmpty()) {
            return@handler
        }

        // With [getPlacementPositions] we should be fine with only one bed block
        val (blockPos, state) = bedBlocks.minByOrNull { (blockPos, _) -> blockPos.getSquaredDistance(eyesPos) }
            ?: return@handler

        val playerPosition = player.pos
        val playerPose = player.pose

        val placementPositions = getPlacementPositions(blockPos, state)
            // todo: sort by usefulness instead, currently it just places the block as far as possible
            //   so we prevent placing blocks in front of our face
            .sortedBy { pos -> -pos.getSquaredDistance(playerPosition) }
        if (placementPositions.isEmpty()) {
            return@handler
        }

        ModuleDebug.debugGeometry(this, "PlacementPosition", ModuleDebug.DebugCollection(
            placementPositions.map { pos -> ModuleDebug.DebuggedPoint(pos.toCenterPos(), Color4b.RED.alpha(100)) }
        ))

        for (position in placementPositions) {
            // todo: prevent using the bed as place face
            val searchOptions = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                ItemStack(Items.SANDSTONE),
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                playerPosition,
                playerPose
            )

            val placementTarget = findBestBlockPlacementTarget(position, searchOptions)
                ?: continue

            // Check if we can reach the target
            if (raytraceTarget(placementTarget.interactedBlockPos, placementTarget.rotation) == null) {
                continue
            }

            ModuleDebug.debugGeometry(this, "PlacementTarget",
                ModuleDebug.DebuggedPoint(position.toCenterPos(), Color4b.GREEN.alpha(100)))

            if (placementTarget.interactedBlockPos.getBlock() is BedBlock) {
                it.movementEvent.sneaking = true
            }

            RotationManager.aimAt(
                placementTarget.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                provider = this@ModuleBedDefender,
                priority = Priority.NOT_IMPORTANT
            )
            this.placementTarget = placementTarget
            break
        }
    }

    @Suppress("unused")
    private val repeatable = repeatable {
        val target = placementTarget ?: return@repeatable
        val blockPos = target.interactedBlockPos

        // Check if we have a block in our hands
        val itemStackInHand = player.mainHandStack
        val itemStackInOffHand = player.offHandStack
        if (ScaffoldBlockItemSelection.isBlockUnfavourable(itemStackInHand)
            && ScaffoldBlockItemSelection.isBlockUnfavourable(itemStackInOffHand)) {
            val slot = slotsToUse.maxByOrNull { slot -> slot.itemStack.count } ?: return@repeatable
            SilentHotbar.selectSlotSilently(this, slot.hotbarSlot)
        }

        // Check if we are facing the target
        val blockHitResult = raytraceTarget(blockPos)
            ?: return@repeatable

        // Place block
        doPlacement(blockHitResult)
        waitTicks(placeCooldown)
    }

    private fun raytraceTarget(blockPos: BlockPos, providedRotation: Rotation? = null): BlockHitResult? {
        val blockState = blockPos.getState() ?: return null

        // Raytrace with collision
        val raycast = raycast(
            range = range.toDouble(),
            rotation = providedRotation ?: RotationManager.serverRotation,
        )
        if (raycast != null && raycast.type == HitResult.Type.BLOCK && raycast.blockPos == blockPos) {
            return raycast
        }

        // Raytrace through walls
        if (wallRange <= 0f) {
            return null
        }

        val blockHitResult = raytraceBlock(
            range = wallRange.toDouble(),
            rotation = providedRotation ?: RotationManager.serverRotation,
            pos = blockPos,
            state = blockState
        )
        if (blockHitResult != null && blockHitResult.type == HitResult.Type.BLOCK
            && blockHitResult.blockPos == blockPos) {
            return blockHitResult
        }

        return null
    }

    private fun getPlacementPositions(pos: BlockPos, state: BlockState): List<BlockPos> {
        val pos2 = pos.offset(BedBlock.getOppositePartDirection(state))
        if (pos2.getState()?.block !in BED_BLOCKS) {
            return emptyList()
        }

        // todo: filter out player position
        return (pos.getPlacementPositionsAround() + pos2.getPlacementPositionsAround())
            .distinct()
    }

    private fun BlockPos.getPlacementPositionsAround(): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()

        for (offsetX in x - maxLayers..x + maxLayers) {
            for (offsetZ in z - maxLayers..z + maxLayers) {
                for (offsetY in y..y + maxLayers) {
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
