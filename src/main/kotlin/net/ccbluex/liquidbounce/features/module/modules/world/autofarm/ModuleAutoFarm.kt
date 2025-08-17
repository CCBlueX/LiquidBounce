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
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.*
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

/**
 * AutoFarm module
 *
 * Automatically farms stuff for you.
 */
object ModuleAutoFarm : ClientModule("AutoFarm", Category.WORLD) {
    // TODO Fix this entire module-
    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(it, range)
    }

    // The ticks to wait after interacting with something
    private val interactDelay by intRange("InteractDelay", 2..3, 1..15, "ticks")

//    private val extraSearchRange by float("extraSearchRange", 0F, 0F..3F)

    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableConfigurable(this, "AutoPlace", true) {
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20, "ticks")
    }

    private val fortune by boolean("UseFortune", true)

    private val autoWalk = tree(AutoFarmAutoWalk)

    init {
        tree(AutoPlaceCrops)
        tree(AutoFarmVisualizer)
    }

    val rotations = tree(RotationsConfigurable(this))

    val itemsForFarmland = arrayOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO)
    val itemsForSoulsand = arrayOf(Items.NETHER_WART)

    private val itemForFarmland
        get() = Slots.Hotbar.findClosestSlot(items = itemsForFarmland)
    private val itemForSoulSand
        get() = Slots.Hotbar.findClosestSlot(items = itemsForFarmland)

    var currentTarget: BlockPos? = null

    val repeatable = tickHandler {
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

        // If there is no currentTarget (a block close enough to be interacted with) walk if wanted
        currentTarget ?: run {
            autoWalk.updateWalkTarget()
            return@tickHandler
        }

        autoWalk.stopWalk() // Stop walking if we found a target close enough to interact with it

        val currentRotation = RotationManager.serverRotation

        val rayTraceResult = world.raycast(
            RaycastContext(
                player.eyePos,
                player.eyePos.add(currentRotation.directionVector.multiply(range.toDouble())),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return@tickHandler

        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return@tickHandler
        }

        val blockPos = rayTraceResult.blockPos

        var state = blockPos.getState() ?: return@tickHandler
        if (isTargeted(state, blockPos)) {
            if (fortune) {
                // Swap to a fortune item to increase drops
                Slots.Hotbar.maxByOrNull { it.itemStack.getEnchantment(Enchantments.FORTUNE) }
                    ?.takeIf { it.itemStack.getEnchantment(Enchantments.FORTUNE) >= 1 }
                    ?.let {
                        SilentHotbar.selectSlotSilently(this, it, 2)
                    }
            }

            val direction = rayTraceResult.side

            if (interaction.updateBlockBreakingProgress(blockPos, direction)) {
                player.swingHand(Hand.MAIN_HAND)
            }

            if (interaction.blockBreakingProgress == -1) {
                // Only wait if the block is completely broken
                waitTicks(interactDelay.random())
            }
        } else {
            val pos = blockPos.offset(rayTraceResult.side).down()

            state = pos.getState() ?: return@tickHandler

            if (isFarmBlockWithAir(state, pos)) {
                val item = if (state.block is FarmlandBlock) {
                    itemForFarmland
                } else {
                    itemForSoulSand
                }

                item ?: return@tickHandler

                SilentHotbar.selectSlotSilently(this, item, AutoPlaceCrops.swapBackDelay.random())
                doPlacement(rayTraceResult)

                waitTicks(interactDelay.random())
            }
        }
    }

    // Searches for any blocks within the radius that need to be destroyed, such as crops.
    private fun updateTargetToBreakable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        val blocksToBreak = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            !state.isAir && isTargeted(state, pos) &&
                    getNearestPoint(eyesPos, Box(pos)).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        for ((pos, state) in blocksToBreak) {
            val (rotation, _) = raytraceBlock(
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

    // Searches for any blocks suitable for placing crops or nether wart on
    // returns ture if it found a target
    private fun updateTargetToPlaceable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        val hotbarItems = Slots.Hotbar.items

        val allowFarmland = hotbarItems.any { it in itemsForFarmland }
        val allowSoulsand = hotbarItems.any { it in itemsForSoulsand }

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
    }

    fun isTargeted(state: BlockState, pos: BlockPos): Boolean {
        return when (val block = state.block) {
            is PumpkinBlock -> true
            Blocks.MELON -> true
            is CropBlock -> block.isMature(state)
            is NetherWartBlock -> state.get(NetherWartBlock.AGE) >= 3
            is CocoaBlock -> state.get(CocoaBlock.AGE) >= 2
            is SugarCaneBlock -> isAboveLast<SugarCaneBlock>(pos)
            is CactusBlock -> isAboveLast<CactusBlock>(pos)
            is KelpPlantBlock -> isAboveLast<KelpPlantBlock>(pos)
            is BambooBlock -> isAboveLast<BambooBlock>(pos)
            else -> false
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
        return isFarmBlock(state, allowFarmland, allowSoulsand) && hasAirAbove(pos)
    }

    fun hasAirAbove(pos: BlockPos) = pos.up().getState()?.isAir == true

    private fun isFarmBlock(state: BlockState, allowFarmland: Boolean, allowSoulsand: Boolean): Boolean {
        return when (state.block) {
            is FarmlandBlock -> allowFarmland
            is SoulSandBlock -> allowSoulsand
            else -> false
        }
    }

    private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
        return pos.down().getBlock() is T && pos.down(2).getBlock() !is T
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(AutoFarmBlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(AutoFarmBlockTracker)
        currentTarget = null
    }

}
