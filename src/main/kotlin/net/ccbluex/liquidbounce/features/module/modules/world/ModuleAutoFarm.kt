/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.minecraft.block.*
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.abs

/**
 * AutoFarm module
 *
 * Automatically farms stuff for you.
 */
object ModuleAutoFarm : Module("AutoFarm", Category.WORLD) {
    // TODO Fix this entire module-
    private val range by float("Range", 5F, 1F..6F)
    private val throughWalls by boolean("ThroughWalls", false)


    private object replaceCrops : ToggleableConfigurable(this, "ReplaceCrops", true) {
        val delay by intRange("Delay", 1..2, 0..20)
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20)

    }
    private object autoPlaceCrops : ToggleableConfigurable(this, "AutoPlaceCrops", true) {
        val delay by intRange("Delay", 0..0, 0..20)
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20)
    }

    private val rotations = RotationsConfigurable()

    init {
        tree(replaceCrops)
        tree(autoPlaceCrops)
        tree(rotations)
    }

    // Rotation

    private var currentTarget: BlockPos? = null

    val networkTickHandler = repeatable { _ ->
        if (mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }
        updateTarget()

        val rotation = RotationManager.currentRotation ?: return@repeatable

        val rayTraceResult = raycast(4.5, rotation) ?: return@repeatable

        if(replaceCrops.enabled && cropToReplace?.getState()?.isAir == true && rayTraceResult.blockPos.offset(rayTraceResult.side) == cropToReplace){
            val item = findClosestItem(arrayOf(Items.WHEAT_SEEDS))
            if(item != null){
                wait(replaceCrops.delay.random())
                SilentHotbar.selectSlotSilently(this, item, replaceCrops.swapBackDelay.random())
                placeCrop(rayTraceResult)
            }
        }

        if (ModuleBlink.enabled) {
            return@repeatable
        }

//        val curr = currentTarget ?: return@repeatable



        if (rayTraceResult?.type != HitResult.Type.BLOCK
        ) {
            return@repeatable
        }
        val blockPos = rayTraceResult.blockPos
        if(isTargeted(
                rayTraceResult.blockPos.getState()!!,
                rayTraceResult.blockPos
            )){

            if (!blockPos.getState()!!.isAir) {
                val direction = rayTraceResult.side
                if (mc.interactionManager!!.updateBlockBreakingProgress(blockPos, direction)) {
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
        } else if(isFarmBlock(
                rayTraceResult.blockPos.getState()!!,
                rayTraceResult.blockPos)){
            val item = findClosestItem(arrayOf(Items.WHEAT_SEEDS))
            if(item != null){
                val delay = autoPlaceCrops.delay.random()
                SilentHotbar.selectSlotSilently(this, item, autoPlaceCrops.swapBackDelay.random() + delay)
                if(delay != 0){
                    wait(autoPlaceCrops.delay.random())
                }
                placeCrop(rayTraceResult)
            }
        }


    }
    private var cropToReplace: BlockPos? = null

    private fun findClosestItem(items: Array<Item>) = (0..8).filter { player.inventory.getStack(it).item in items }
        .minByOrNull { abs(player.inventory.selectedSlot - it) }

    private fun placeCrop(rayTraceResult: BlockHitResult){
        val stack = player.mainHandStack
        val count = stack.count
        val interactBlock = interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult)

        if (interactBlock.isAccepted) {
            if (interactBlock.shouldSwingHand()) {
                player.swingHand(Hand.MAIN_HAND)

                if (!stack.isEmpty && (stack.count != count || interaction.hasCreativeInventory())) {
                    mc.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND)
                }
            }

            return
        } else if (interactBlock == ActionResult.FAIL) {
            return
        }
    }

    private fun updateTarget() {
        this.currentTarget = null

        val radius = range + 1
        val radiusSquared = radius * radius
        val eyesPos = mc.player!!.eyes

        val blockToProcess = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            !state.isAir && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared && isTargeted(state, pos)
        }.minByOrNull { it.first.getCenterDistanceSquared() }
            ?: if(autoPlaceCrops.enabled) {searchBlocksInCuboid(radius.toInt()) { pos, state ->
            !state.isAir && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared && isFarmBlock(state, pos)
        }.minByOrNull { it.first.getCenterDistanceSquared() } ?: return} else return

        val (pos, state) = blockToProcess

        val rt = raytraceBlock(
            player.eyes,
            pos,
            state,
            range = range.toDouble(),
            wallsRange = if (throughWalls) range.toDouble() else 0.0
        )

        // We got a free angle at the block? Cool.
        if (rt != null) {
            val (rotation, _) = rt
            RotationManager.aimAt(rotation, configurable = rotations)

            this.currentTarget = pos
            if(state.block is CropBlock && replaceCrops.enabled){
                cropToReplace = pos
            }
            return
        }

        val raytraceResult = mc.world?.raycast(
            RaycastContext(
                player.eyes,
                Vec3d.of(pos).add(0.5, 0.5, 0.5),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        // Failsafe. Should not trigger
        if (raytraceResult.type != HitResult.Type.BLOCK) return

        this.currentTarget = raytraceResult.blockPos
    }

    private fun isTargeted(state: BlockState, pos: BlockPos): Boolean {
        val block = state.block

        return when (block) {
            is GourdBlock -> true
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
    private fun isFarmBlock(state: BlockState, pos: BlockPos): Boolean {
        val block = state.block

        return when (block) {
            is FarmlandBlock -> pos.up().getState()?.isAir == true
            else -> false
        }
    }

    private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
        return pos.down().getBlock() is T && pos.down(2).getBlock() !is T
    }


}
