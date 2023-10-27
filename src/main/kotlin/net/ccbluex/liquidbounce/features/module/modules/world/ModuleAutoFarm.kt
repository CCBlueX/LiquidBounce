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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.minecraft.block.*
import net.minecraft.client.gui.screen.ingame.HandledScreen
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
    private val throughWalls by boolean("ThroughWalls", false)

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private var currentTarget: BlockPos? = null

    val networkTickHandler = repeatable { event ->
        if (mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }

        updateTarget()

        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val curr = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.currentRotation ?: return@repeatable

        val rayTraceResult = mc.world?.raycast(
            RaycastContext(
                player.eyes,
                player.eyes.add(currentRotation.rotationVec.multiply(range.toDouble())),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        )

        if (rayTraceResult?.type != HitResult.Type.BLOCK || !isTargeted(
                rayTraceResult.blockPos.getState()!!,
                rayTraceResult.blockPos
            )
        ) {
            return@repeatable
        }

        val blockPos = rayTraceResult.blockPos

        if (!blockPos.getState()!!.isAir) {
            val direction = rayTraceResult.side

            if (mc.interactionManager!!.updateBlockBreakingProgress(blockPos, direction)) {
                player.swingHand(Hand.MAIN_HAND)
            }
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
        }.minByOrNull { it.first.getCenterDistanceSquared() } ?: return

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
            return
        }

        val raytraceResult = mc.world?.raycast(
            RaycastContext(
                player.eyes,
                Vec3d.of(pos).add(0.5, 0.5, 0.5),
                RaycastContext.ShapeType.COLLIDER,
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

    private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
        return pos.down().getBlock() is T && pos.down(2).getBlock() !is T
    }

}
