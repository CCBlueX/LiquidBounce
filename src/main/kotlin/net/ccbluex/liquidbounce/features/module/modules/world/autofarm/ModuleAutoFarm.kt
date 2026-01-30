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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.utils.asRefreshable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockSide
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.entity.getNearestPointOnSide
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPoint
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

/**
 * AutoFarm module
 *
 * Automatically farms stuff for you.
 */
object ModuleAutoFarm : ClientModule("AutoFarm", ModuleCategories.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(it, range)
    }

    // The ticks to wait after interacting with something
    private val interactDelay by intRange("InteractDelay", 2..3, 1..15, "ticks")

    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableValueGroup(this, "AutoPlant", true, aliases = listOf("AutoPlace")) {
        val swapBackDelay by intRange("SwapBackDelay", 1..2, 1..20, "ticks")
    }

    internal object AutoUseBoneMeal : ToggleableValueGroup(this, "AutoUseBoneMeal", false) {
        private val chronometer = Chronometer()
        // TODO Use filter (wheat/potato/...)
        private val useDelay = intRange("UseDelay", 20..200, 0..20000, "ms").asRefreshable()
        val swapBackDelay by intRange("SwapBackDelay", 1..2, 1..20, "ticks")

        val isReady get() = chronometer.hasElapsed(useDelay.current.toLong())

        fun reset() {
            chronometer.reset()
            useDelay.refresh()
        }
    }

    private val fortune by boolean("UseFortune", true)

    init {
        tree(AutoFarmAutoWalk)
        tree(AutoPlaceCrops)
        tree(AutoUseBoneMeal)
        tree(AutoFarmVisualizer)
    }

    internal val rotations = tree(RotationsValueGroup(this))

    private fun swapToSlotWithFortune() {
        if (!fortune) {
            return
        }
        // Swap to a fortune item to increase drops
        Slots.Hotbar.maxByOrNull { it.itemStack.getEnchantment(Enchantments.FORTUNE) }
            ?.takeIf { it.itemStack.getEnchantment(Enchantments.FORTUNE) >= 1 }
            ?.let {
                SilentHotbar.selectSlotSilently(this, it, 2)
            }
    }

    var currentTarget: BlockPos? = null
        private set

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // Return if the user is inside a screen like the inventory
        if (mc.screen is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        updateTarget()

        // Return if the blink module is enabled
        if (ModuleBlink.running) {
            return@tickHandler
        }

        // Disable the module and return if the inventory is full, and the setting for disabling the module is enabled
        if (disableOnFullInventory && !hasInventorySpace()) {
            notification("Inventory is Full", "AutoFarm has been disabled", NotificationEvent.Severity.ERROR)
            onDisabled()
            enabled = false
            return@tickHandler
        }

        // Return if we don't have a target
        currentTarget ?: return@tickHandler

        val rayTraceResult = traceFromPoint(
            range = range.toDouble(),
            start = player.eyePosition,
            direction = (RotationManager.currentRotation ?: player.rotation).directionVector,
            entity = player,
        )
        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return@tickHandler
        }

        val blockPos = rayTraceResult.blockPos

        val state = blockPos.getState() ?: return@tickHandler
        if (blockPos.readyForHarvest(state)) {
            when (state.block.harvestAction) {
                HarvestAction.BREAK -> {
                    swapToSlotWithFortune()
                    doBreak(rayTraceResult)
                }
                HarvestAction.USE -> {
                    doPlacement(rayTraceResult)
                }
                null -> return@tickHandler
            }

            if (interaction.destroyStage == -1) {
                // Only wait if the block is completely broken
                waitTicks(interactDelay.random())
            }
        } else if (AutoUseBoneMeal.enabled && AutoUseBoneMeal.isReady && blockPos.canUseBoneMeal(state)) {
            val boneMealSlot = Slots.OffhandWithHotbar.findClosestSlot { it.item is BoneMealItem } ?: return@tickHandler

            SilentHotbar.selectSlotSilently(this, boneMealSlot, AutoUseBoneMeal.swapBackDelay.random())
            doPlacement(rayTraceResult, hand = boneMealSlot.useHand)
            AutoUseBoneMeal.reset()
            waitTicks(interactDelay.random())
        } else {
            val blockState = world.getBlockState(blockPos)

            debugGeometry("RayTraceResult") {
                ModuleDebug.DebuggedPoint(rayTraceResult.location, Color4b.RED.alpha(150))
            }
            debugGeometry("PlantablePos") {
                ModuleDebug.DebuggedBox(AABB(blockPos), Color4b.GREEN.alpha(100))
            }

            val sides = AutoFarmTrackedState.Plantable.entries.findPlantableSides(blockPos, blockState)
            if (sides.isNotEmpty()) {
                val slot = AutoFarmTrackedState.Plantable.entries.firstNotNullOfOrNull {
                    if (it.isBlockMatches(blockState)) {
                        Slots.OffhandWithHotbar.findClosestSlot(it.items)
                    } else {
                        null
                    }
                } ?: return@tickHandler

                SilentHotbar.selectSlotSilently(this, slot, AutoPlaceCrops.swapBackDelay.random())
                doPlacement(rayTraceResult, hand = slot.useHand)

                waitTicks(interactDelay.random())
            }
        }
    }

    private fun updateTarget(possible: Sequence<Pair<BlockPos, BlockState>>): Boolean {
        for ((pos, state) in possible) {
            val (rotation, _) = raytraceBlockRotation(
                player.eyePosition,
                pos,
                state,
                range = range.toDouble() - 0.1,
                wallsRange = wallRange.toDouble() - 0.1
            ) ?: continue // We don't have a free angle at the block? Well, let me see the next.

            // set currentTarget to the new target
            currentTarget = pos
            // aim at target
            RotationManager.setRotationTarget(
                rotation,
                valueGroup = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = this@ModuleAutoFarm
            )

            return true // We got a free angle at the block? No need to see more of them.
        }
        return false
    }

    /** Searches for any blocks within the radius that need to be destroyed, such as crops. */
    private fun updateTargetToHarvest(radius: Float, radiusSquared: Float, eyesPos: Vec3): Boolean {
        val blocksToBreak = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            !state.isAir && pos.readyForHarvest(state) &&
                AABB(pos).getNearestPoint(eyesPos).distanceToSqr(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        return updateTarget(blocksToBreak)
    }

    // Searches for any blocks suitable for placing crops or nether wart on
    // returns ture if it found a target
    private fun updateTargetToPlantable(radius: Float, radiusSquared: Float, eyesPos: Vec3): Boolean {
        val hotbarItems = Slots.OffhandWithHotbar.items

        val allowedTypes = AutoFarmTrackedState.Plantable.entries.filter { type ->
            hotbarItems.any { it in type.items }
        }

        if (allowedTypes.isEmpty()) return false

        val blocksToPlace =
            eyesPos.searchBlocksInCuboid(radius) { _, state ->
                !state.isAir && allowedTypes.any { it.isBlockMatches(state) }
            }.mapNotNullTo(mutableListOf()) { (pos, state) ->
                val sides =
                    allowedTypes.findPlantableSides(pos, state).takeUnless { it.isEmpty() } ?: return@mapNotNullTo null
                sides.removeIf { side ->
                    getNearestPointOnSide(eyesPos, AABB(pos), side)
                        .distanceToSqr(eyesPos) > radiusSquared
                }
                if (sides.isEmpty()) return@mapNotNullTo null
                pos to sides
            }.sortedBy { it.first.getCenterDistanceSquared() }

        val collisionContext = CollisionContext.of(player)
        for ((pos, sides) in blocksToPlace) {
            val (rotation, _) = sides.firstNotNullOfOrNull { side ->
                raytraceBlockSide(
                    side,
                    pos,
                    player.eyePosition,
                    rangeSquared = range.sq().toDouble() - 0.1,
                    wallsRangeSquared = wallRange.sq().toDouble() - 0.1,
                    collisionContext,
                )
            } ?: continue // We don't have a free angle at the block? Well, let me see the next.

            // set currentTarget to the new target
            currentTarget = pos
            // aim at target
            RotationManager.setRotationTarget(
                rotation,
                valueGroup = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = this@ModuleAutoFarm
            )

            return true // We got a free angle at the block? No need to see more of them.
        }
        return false
    }

    private fun updateTargetToFertilizable(radius: Float, radiusSquared: Float, eyesPos: Vec3): Boolean {
        if (Slots.OffhandWithHotbar.none { it.itemStack.item is BoneMealItem }) {
            return false
        }

        val blocksToFertile = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            !state.isAir && pos.canUseBoneMeal(state) &&
                AABB(pos).getNearestPoint(eyesPos).distanceToSqr(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        return updateTarget(blocksToFertile)
    }

    // Finds either a breakable target (such as crops, cactus, etc.)
    // or a placeable target (such as a farmblock or soulsand with air above).
    // It will prefer a breakable target
    private fun updateTarget() {
        currentTarget = null

        val radius = range
        val radiusSquared = radius * radius
        val eyesPos = player.eyePosition

        // Can we find a breakable target?
        if (updateTargetToHarvest(radius, radiusSquared, eyesPos)) {
            return
        }

        // Can we find a placeable target?
        if (AutoPlaceCrops.enabled && updateTargetToPlantable(radius, radiusSquared, eyesPos)) {
            return
        }

        if (AutoUseBoneMeal.enabled && updateTargetToFertilizable(radius, radiusSquared, eyesPos)) {
            return
        }
    }

    /**
     * Find plantable sides of the block for all types in the iterable
     */
    private fun Iterable<AutoFarmTrackedState.Plantable>.findPlantableSides(
        pos: BlockPos,
        state: BlockState,
    ) = flatMapTo(enumSetOf()) { it.findPlantableSides(pos, state) }

    override fun onEnabled() {
        ChunkScanner.subscribe(AutoFarmBlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(AutoFarmBlockTracker)
        currentTarget = null
        AutoUseBoneMeal.reset()
    }

}
