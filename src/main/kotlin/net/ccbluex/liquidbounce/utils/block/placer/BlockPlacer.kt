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
package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleBedDefender.mc
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleBedDefender.player
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.render.PlacementRenderer
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

// TODO sneaking leads to multiple placements?
class BlockPlacer(
    name: String,
    val module: Module,
    val priority: Priority,
    val slotFinder: () -> HotbarItemSlot?
) : Configurable(name), Listenable {

    val range by float("Range", 4.5f, 1f..6f)
    val wallRange by float("WallRange", 4.5f, 1f..6f)
    val cooldown by int("Cooldown", 1, 0..10, "ticks")
    val swingMode by enumChoice("Swing", PlacementSwingMode.DO_NOT_HIDE)

    /**
     * Defines how long the player should sneak when placing on an interactable block.
     * This can make placing multiple blocks seem smoother.
     */
    val sneak by int("Sneak", 1, 0..10, "ticks")

    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    val ignoreUsingItem by boolean("IgnoreUsingItem", true)
    val rotations = tree(RotationsConfigurable(this))

    /**
     * Renders all tracked positions that are queued to be placed.
     */
    val targetRenderer = tree(PlacementRenderer("TargetRendering", false, module))

    /**
     * Renders all placements.
     */
    val placedRenderer = tree(PlacementRenderer(
        "PlacedRendering",
        true,
        module,
        keep = false,
        clump = false
    ))

    var currentTarget: BlockPlacementTarget? = null
    private val blocks = linkedSetOf<BlockPos>()
    private var sneakTimes = 0

    @Suppress("unused")
    private val repeatable = repeatable {
        val target = currentTarget ?: return@repeatable
        val blockPos = target.interactedBlockPos

        // Choose block to place
        val slot = slotFinder() ?: return@repeatable
        SilentHotbar.selectSlotSilently(this, slot.hotbarSlot)

        // Check if we are facing the target
        val blockHitResult = raytraceTarget(blockPos, currentTarget!!)
            ?: return@repeatable

        // Place block
        doPlacement(blockHitResult, placementSwingMode = swingMode)
        val pos = currentTarget!!.placedBlock
        targetRenderer.removeBlock(pos)
        placedRenderer.addBlock(pos)
        currentTarget = null

        waitTicks(cooldown)
    }

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent>(priority = -100) {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        if (!ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        if (sneakTimes > 0) {
            sneakTimes--
            it.movementEvent.sneaking = true
        }

        val itemStack = /*slotFinder().itemStack ?:*/ ItemStack(Items.SANDSTONE)

        val iterator = blocks.iterator()
        while (iterator.hasNext()) { // TODO skip blocks that are blocked by entities
            val position = iterator.next()

            val searchOptions = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                itemStack,
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                if (wallRange == 0f) player.pos else position.toVec3d(),
                player.pose
            )

            val placementTarget = findBestBlockPlacementTarget(position, searchOptions) // TODO prioritize faces where sneaking is not required
                ?: continue

            // Check if we can reach the target
            if (raytraceTarget(placementTarget.interactedBlockPos, placementTarget, placementTarget.rotation) == null) {
                continue
            }

            ModuleDebug.debugGeometry(this, "PlacementTarget",
                ModuleDebug.DebuggedPoint(position.toCenterPos(), Color4b.GREEN.alpha(100)))

            // sneak when placing on interactable block to not trigger their action
            if (placementTarget.interactedBlockPos.getBlock().isInteractable(
                    placementTarget.interactedBlockPos.getState())
                ) {
                sneakTimes = sneak - 1
                it.movementEvent.sneaking = true
            }

            var rotation = placementTarget.rotation

            // the utils don't allow rotations trough walls, so this hacky work around is needed
            if (wallRange > 0) {
                rotation = RotationManager.makeRotation(placementTarget.interactedBlockPos.toCenterPos().add(Vec3d.of(placementTarget.direction.vector).multiply(0.5)), player.eyePos)
            }

            RotationManager.aimAt(
                rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                provider = module,
                priority = priority
            )
            currentTarget = placementTarget
            iterator.remove()
            break
        }
    }

    private fun raytraceTarget(blockPos: BlockPos, target: BlockPlacementTarget, providedRotation: Rotation? = null): BlockHitResult? {
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
        if (blockPos.toCenterPos().squaredDistanceTo(player.eyePos) > wallRange * wallRange) {
            return null
        }

        return target.blockHitResult
    }

    /**
     * Removes all positions that are not in [positions] and adds all that are not in the queue.
     */
    fun update(positions: Set<BlockPos>) {
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val position = iterator.next()
            if (position !in positions) {
                targetRenderer.removeBlock(position)
                iterator.remove()
            }
        }

        if (currentTarget != null && currentTarget!!.placedBlock !in positions) {
            targetRenderer.removeBlock(currentTarget!!.placedBlock)
            currentTarget = null
        }

        positions.forEach { addToQueue(it, false) }
        targetRenderer.updateAll()
    }

    /**
     * Adds a block to be placed.
     *
     * @param update Whether the renderer should update the culling.
     */
    fun addToQueue(pos: BlockPos, update: Boolean = true) {
        if (blocks.contains(pos)) {
            return
        }

        blocks.add(pos)
        targetRenderer.addBlock(pos, update)
    }

    /**
     * Removes a block from the queue.
     */
    fun removeFromQueue(pos: BlockPos) {
        blocks.remove(pos)
        targetRenderer.removeBlock(pos)
    }

    /**
     * Discards all blocks.
     */
    fun clear() {
        blocks.forEach { targetRenderer.removeBlock(it) }
        blocks.clear()
    }

    /**
     * THis should be called when the module using this placer is disabled.
     */
    fun disable() {
        reset()
        targetRenderer.clearSilently()
        placedRenderer.clearSilently()
    }

    fun isDone(): Boolean {
        return currentTarget == null && blocks.isEmpty()
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private fun reset() {
        sneakTimes = 0
        blocks.clear()
    }

    override fun handleEvents(): Boolean {
        return module.handleEvents()
    }

}
