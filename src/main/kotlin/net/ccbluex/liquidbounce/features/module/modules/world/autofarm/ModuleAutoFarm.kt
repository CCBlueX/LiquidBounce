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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationEngine
import net.ccbluex.liquidbounce.utils.aiming.RotationObserver
import net.ccbluex.liquidbounce.utils.aiming.tracking.RotationTracker
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raytracePlaceBlock
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.Hotbar
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
object ModuleAutoFarm : Module("AutoFarm", Category.WORLD) {
    // TODO Fix this entire module-
    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        if (it > range) {
            range
        } else {
            it
        }
    }

    // The ticks to wait after interacting with something
    private val interactDelay by intRange("InteractDelay", 2..3, 1..15, "ticks")

//    private val extraSearchRange by float("extraSearchRange", 0F, 0F..3F)

    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableConfigurable(this, "AutoPlace", true) {
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20, "ticks")
    }

    private val fortune by boolean("fortune", true)

    private val autoWalk = tree(AutoFarmAutoWalk)

    init {
        tree(AutoPlaceCrops)
        tree(AutoFarmVisualizer)
    }

    val rotationEngine = tree(RotationEngine(this))


    val itemsForFarmland = arrayOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO)
    val itemsForSoulsand = arrayOf(Items.NETHER_WART)

    private val itemForFarmland
        get() = Hotbar.findClosestItem(itemsForFarmland)
    private val itemForSoulSand
        get() = Hotbar.findClosestItem(itemsForFarmland)

    var currentTarget: BlockPos? = null

    val repeatable = repeatable { _ ->
        // Return if the user is inside a screen like the inventory
        if (mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }

        updateTarget()

        // Return if the blink module is enabled
        if (ModuleBlink.enabled) {
            return@repeatable
        }

        // Disable the module and return if the inventory is full, and the setting for disabling the module is enabled
        if (disableOnFullInventory && !hasInventorySpace()) {
            notification("Inventory is Full", "AutoFarm has been disabled", NotificationEvent.Severity.ERROR)
            disable()
            enabled = false
            return@repeatable
        }

        // If there is no currentTarget (a block close enough to be interacted with) walk if wanted
        currentTarget ?: run {
            autoWalk.updateWalkTarget()

            return@repeatable
        }

        autoWalk.stopWalk() // Stop walking if we found a target close enough to interact with it

        val currentRotation = RotationObserver.serverOrientation

        val rayTraceResult = world.raycast(
            RaycastContext(
                player.eyes,
                player.eyes.add(currentRotation.polar3d.multiply(range.toDouble())),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return@repeatable
        }

        val blockPos = rayTraceResult.blockPos

        var state = rayTraceResult.blockPos.getState() ?: return@repeatable
        if (isTargeted(state, blockPos)) {
            if (fortune) {
                Hotbar.findBestItem(1) { _, itemStack -> itemStack.getEnchantment(Enchantments.FORTUNE) }
                    ?.let { (slot, _) ->
                        SilentHotbar.selectSlotSilently(this, slot, 2)
                    } // Swap to a fortune item to increase drops
            }
            val direction = rayTraceResult.side

            if (interaction.updateBlockBreakingProgress(blockPos, direction)) {
                player.swingHand(Hand.MAIN_HAND)
            }
            if (interaction.blockBreakingProgress == -1) {
                // Only wait if the block is completely broken
                waitTicks(interactDelay.random())
            }
            return@repeatable

        } else {
            val pos = blockPos.offset(rayTraceResult.side).down()

            state = pos.getState() ?: return@repeatable

            if (isFarmBlockWithAir(state, pos)) {
                val item = if (state.block is FarmlandBlock) {
                    itemForFarmland
                } else {
                    itemForSoulSand
                }

                item ?: return@repeatable

                SilentHotbar.selectSlotSilently(this, item.hotbarSlotForServer, AutoPlaceCrops.swapBackDelay.random())
                doPlacement(rayTraceResult)

                waitTicks(interactDelay.random())
            }
        }
    }

    // Searches for any blocks within the radius that need to be destroyed, such as crops.
    private fun updateTargetToBreakable(radius: Float, radiusSquared: Float, eyesPos: Vec3d): Boolean {
        val blocksToBreak = searchBlocksInCuboid(radius, eyesPos) { pos, state ->
            !state.isAir && isTargeted(state, pos) && getNearestPoint(
                eyesPos,
                Box.enclosing(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }


        for ((pos, state) in blocksToBreak) {
            val angleLine = raytraceBlock(
                player.eyes,
                pos,
                state,
                range = range.toDouble() - 0.1,
                wallsRange = wallRange.toDouble() - 0.1
            ) ?: continue // We don't have a free angle at the block? Well, let me see the next.

            // set currentTarget to the new target
            currentTarget = pos
            // aim at target
            RotationManager.aimAt(
                RotationTracker.withFixedAngleLine(rotationEngine, angleLine),
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
        val hotbarItems = Hotbar.items

        val allowFarmland = hotbarItems.any { it in itemsForFarmland }
        val allowSoulsand = hotbarItems.any { it in itemsForSoulsand }

        if (!allowFarmland && !allowSoulsand) return false

        val blocksToPlace =
            searchBlocksInCuboid(radius, eyesPos) { pos, state ->
                !state.isAir && isFarmBlockWithAir(state, pos, allowFarmland, allowSoulsand)
                        && getNearestPoint(
                    eyesPos,
                    Box.enclosing(pos, pos.add(1, 1, 1))
                ).squaredDistanceTo(eyesPos) <= radiusSquared
            }.sortedBy { it.first.getCenterDistanceSquared() }

        for ((pos, _) in blocksToPlace) {
            val angleLine = raytracePlaceBlock(
                player.eyes,
                pos.up(),
                range = range.toDouble() - 0.1,
                wallsRange = wallRange.toDouble() - 0.1
            ) ?: continue // We don't have a free angle at the block? Well, let me see the next.

            // set currentTarget to the new target
            currentTarget = pos
            // aim at target
            RotationManager.aimAt(
                RotationTracker.withFixedAngleLine(rotationEngine, angleLine),
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
        val eyesPos = player.eyes

        // Can we find a breakable target?
        if (updateTargetToBreakable(radius, radiusSquared, eyesPos))
            return

        if (!AutoPlaceCrops.enabled)
            return

        // Can we find a placeable target?
        updateTargetToPlaceable(radius, radiusSquared, eyesPos)
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

    override fun enable() {
        ChunkScanner.subscribe(AutoFarmBlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(AutoFarmBlockTracker)
    }

}
