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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.inventory.hasItem
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.BlockState
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * AutoFarm module
 *
 * Automatically farms stuff for you.
 */
object ModuleAutoFarm : ClientModule("AutoFarm", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(it, range)
    }

    // The ticks to wait after interacting with something
    private val interactDelay by intRange("InteractDelay", 2..3, 1..15, "ticks")

    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableConfigurable(this, "AutoPlace", true) {
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20, "ticks")
    }

    internal object AutoUseBoneMeal : ToggleableConfigurable(this, "AutoUseBoneMeal", false) {
        // TODO Use delay, Use filter (wheat/potato/...)
    }

    private val fortune by boolean("UseFortune", true)

    init {
        tree(AutoFarmAutoWalk)
        tree(AutoPlaceCrops)
        tree(AutoUseBoneMeal)
        tree(AutoFarmVisualizer)
    }

    internal val rotations = tree(RotationsConfigurable(this))

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
        if (mc.currentScreen is HandledScreen<*>) {
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

        val rayTraceResult = raycast(
            range = range.toDouble(),
            start = player.eyePos,
            direction = (RotationManager.currentRotation ?: player.rotation).directionVector,
            entity = player,
        )
        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return@tickHandler
        }

        val blockPos = rayTraceResult.blockPos

        val state = blockPos.getState() ?: return@tickHandler
        if (blockPos.readyForHarvest(state)) {
            swapToSlotWithFortune()

            doBreak(rayTraceResult)

            if (interaction.blockBreakingProgress == -1) {
                // Only wait if the block is completely broken
                waitTicks(interactDelay.random())
            }
        } else if (AutoUseBoneMeal.enabled && blockPos.canUseBoneMeal(state)) {
            val boneMealSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.BONE_MEAL) ?: return@tickHandler

            SilentHotbar.selectSlotSilently(this, boneMealSlot, AutoPlaceCrops.swapBackDelay.random())
            doPlacement(rayTraceResult, hand = boneMealSlot.useHand)
            waitTicks(interactDelay.random())
        } else {
            val pos = blockPos.offset(rayTraceResult.side).down()
            val blockState = pos.getState() ?: return@tickHandler

            if (isFarmBlockWithAir(blockState, pos)) {
                val slot = getAvailableSlotForBlock(blockState) ?: return@tickHandler

                SilentHotbar.selectSlotSilently(this, slot, AutoPlaceCrops.swapBackDelay.random())
                doPlacement(rayTraceResult, hand = slot.useHand)

                waitTicks(interactDelay.random())
            }
        }
    }

    private fun updateTarget(possible: Sequence<Pair<BlockPos, BlockState>>): Boolean {
        for ((pos, state) in possible) {
            val (rotation, _) = raytraceBlockRotation(
                player.eyePos,
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
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = this@ModuleAutoFarm
            )

            return true // We got a free angle at the block? No need to see more of them.
        }
        return false
    }

    // Searches for any blocks within the radius that need to be destroyed, such as crops.
    private fun updateTargetToBreakable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        val blocksToBreak = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            !state.isAir && pos.readyForHarvest(state) &&
                    getNearestPoint(eyesPos, Box(pos)).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        return updateTarget(blocksToBreak)
    }

    // Searches for any blocks suitable for placing crops or nether wart on
    // returns ture if it found a target
    private fun updateTargetToPlaceable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        val hotbarItems = Slots.OffhandWithHotbar.items

        val allowFarmland = hotbarItems.any { it in itemsForFarmland }
        val allowSoulsand = hotbarItems.any { it in itemsForSoulSand }

        if (!allowFarmland && !allowSoulsand) return false

        val blocksToPlace =
            eyesPos.searchBlocksInCuboid(radius) { pos, state ->
                !state.isAir && isFarmBlockWithAir(state, pos, allowFarmland, allowSoulsand)
                        && getNearestPoint(eyesPos, Box(pos)).squaredDistanceTo(eyesPos) <= radiusSquared
            }.map { it.first }.sortedBy { it.getCenterDistanceSquared() }

        for (pos in blocksToPlace) {
            // We can only plant on the upper side
            val (rotation, _) = raytraceUpperBlockSide(
                player.eyePos,
                range = range.toDouble() - 0.1,
                wallsRange = wallRange.toDouble() - 0.1,
                pos
            ) ?: continue // We don't have a free angle at the block? Well, let me see the next.

            // set currentTarget to the new target
            currentTarget = pos
            // aim at target
            RotationManager.setRotationTarget(
                rotation,
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = this@ModuleAutoFarm
            )

            return true // We got a free angle at the block? No need to see more of them.
        }
        return false
    }

    private fun updateTargetToFertilizable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        if (!Slots.OffhandWithHotbar.hasItem(Items.BONE_MEAL)) {
            return false
        }

        val blocksToFertile = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            !state.isAir && pos.canUseBoneMeal(state) &&
                getNearestPoint(eyesPos, Box(pos)).squaredDistanceTo(eyesPos) <= radiusSquared
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
        val eyesPos = player.eyePos

        // Can we find a breakable target?
        if (updateTargetToBreakable(radius, radiusSquared, eyesPos)) {
            return
        }

        // Can we find a placeable target?
        if (AutoPlaceCrops.enabled && updateTargetToPlaceable(radius, radiusSquared, eyesPos)) {
            return
        }

        if (AutoUseBoneMeal.enabled && updateTargetToFertilizable(radius, radiusSquared, eyesPos)) {
            return
        }
    }

    /**
     * checks if the block is either a farmland or soulsand block and has air above it
     */
    private fun isFarmBlockWithAir(
        state: BlockState,
        pos: BlockPos,
        allowFarmland: Boolean = true,
        allowSoulsand: Boolean = true
    ): Boolean {
        return isFarmBlock(state, allowFarmland, allowSoulsand) && pos.up().getState()?.isAir == true
    }

    private fun isFarmBlock(state: BlockState, allowFarmland: Boolean, allowSoulsand: Boolean): Boolean {
        return when (state.block) {
            is FarmlandBlock -> allowFarmland
            is SoulSandBlock -> allowSoulsand
            else -> false
        }
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(AutoFarmBlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(AutoFarmBlockTracker)
        currentTarget = null
    }

}
