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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.item.findBlocksEndingWith
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.RaycastContext
import kotlin.math.max

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : Module("Fucker", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }
    private val surroundings by boolean("Surroundings", true)
    private val targets by blocks("Target", findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet())
    private val delay by int("Delay", 0, 0..20)
    private val action by enumChoice("Action", DestroyAction.DESTROY, DestroyAction.values())
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private var currentTarget: DestroyerTarget? = null

    val moduleRepeatable = repeatable {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }

        val wasTarget = currentTarget

        updateTarget()

        if (wasTarget != null && currentTarget == null) {
            interaction.cancelBlockBreaking()
        }

        // Delay if the target changed - this also includes when introducing a new target from null.
        if (wasTarget != currentTarget) {
            waitTicks(delay)
        }

        // Check if blink is enabled - if so, we don't want to do anything.
        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val destroyerTarget = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.serverRotation

        // Check if we are already looking at the block
        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@repeatable
        ) ?: return@repeatable

        val raytracePos = rayTraceResult.blockPos

        // Check if the raytrace result includes a block, if not we don't want to deal with it.
        if (rayTraceResult.type != HitResult.Type.BLOCK ||
            raytracePos.getState()?.isAir == true || raytracePos != destroyerTarget.pos) {
            return@repeatable
        }

        // Use action should be used if the block is the same as the current target and the action is set to use.
        if (destroyerTarget.action == DestroyAction.USE) {
            if (interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            waitTicks(delay)
        } else {
            doBreak(rayTraceResult, immediate = forceImmediateBreak)
        }
    }

    private fun updateTarget() {
        this.currentTarget = null

        val radius = range + 1
        val radiusSquared = radius * radius
        val eyesPos = player.eyes

        val blockToProcess = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            targets.contains(state.block) && getNearestPoint(
                eyesPos, Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.minByOrNull { it.first.getCenterDistanceSquared() } ?: return

        val (pos, state) = blockToProcess

        val raytrace = raytraceBlock(
            eyesPos, pos, state, range = range.toDouble(), wallsRange = wallRange.toDouble()
        )

        // Check if we got a free angle to the block
        if (raytrace != null) {
            val (rotation, _) = raytrace
            RotationManager.aimAt(rotation, considerInventory = !ignoreOpenInventory, configurable = rotations)

            this.currentTarget = DestroyerTarget(pos, this.action)
            return
        }

        // Is there any block in the way?
        if (surroundings) {
            updateSurroundings(pos, state)
        }
    }

    private fun updateSurroundings(initialPosition: BlockPos, state: BlockState) {
        val raytraceResult = world.raycast(
            RaycastContext(
                player.eyes,
                initialPosition.toCenterPos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        if (raytraceResult.type != HitResult.Type.BLOCK) {
            return
        }

        val raytrace = raytraceBlock(
            player.eyes, raytraceResult.blockPos, state, range = range.toDouble(), wallsRange = wallRange.toDouble()
        ) ?: return

        val (rotation, _) = raytrace
        RotationManager.aimAt(rotation, considerInventory = !ignoreOpenInventory, configurable = rotations)

        this.currentTarget = DestroyerTarget(raytraceResult.blockPos, this.action)
    }

    data class DestroyerTarget(val pos: BlockPos, val action: DestroyAction)

    enum class DestroyAction(override val choiceName: String) : NamedChoice {
        DESTROY("Destroy"), USE("Use")
    }
}
