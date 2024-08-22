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
package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.utils.aiming.RotationObserver
import net.ccbluex.liquidbounce.utils.block.forEachBlockPosBetween
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.input.Input
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.EntityPose
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean,
) {
    constructor(input: Input) : this(input.pressingForward, input.pressingBack, input.pressingLeft, input.pressingRight)

    override fun equals(other: Any?): Boolean {
        return other is DirectionalInput &&
                forwards == other.forwards &&
                backwards == other.backwards &&
                left == other.left &&
                right == other.right
    }

    override fun hashCode(): Int {
        var result = forwards.hashCode()
        result = 31 * result + backwards.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    fun isMoving(): Boolean {
        return forwards || backwards || left || right
    }

    companion object {
        val NONE = DirectionalInput(forwards = false, backwards = false, left = false, right = false)
        val FORWARDS = DirectionalInput(forwards = true, backwards = false, left = false, right = false)
        val BACKWARDS = DirectionalInput(forwards = false, backwards = true, left = false, right = false)
        val LEFT = DirectionalInput(forwards = false, backwards = false, left = true, right = false)
        val RIGHT = DirectionalInput(forwards = false, backwards = false, left = false, right = true)
    }
}

/**
 * Returns the yaw difference the position is from the player position
 *
 * @param positionRelativeToPlayer relative position to player
 */
fun getDegreesRelativeToView(
    positionRelativeToPlayer: Vec3d,
    yaw: Float = RotationObserver.currentOrientation?.yaw ?: mc.player!!.yaw,
): Float {
    val optimalYaw =
        atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z).toFloat()
    val currentYaw = MathHelper.wrapDegrees(yaw).toRadians()

    return MathHelper.wrapDegrees((optimalYaw - currentYaw).toDegrees())
}

fun getDirectionalInputForDegrees(
    directionalInput: DirectionalInput,
    dgs: Float,
    deadAngle: Float = 20.0F,
): DirectionalInput {
    var forwards = directionalInput.forwards
    var backwards = directionalInput.backwards
    var left = directionalInput.left
    var right = directionalInput.right

    if (dgs in -90.0F + deadAngle..90.0F - deadAngle) {
        forwards = true
    } else if (dgs < -90.0 - deadAngle || dgs > 90.0 + deadAngle) {
        backwards = true
    }

    if (dgs in 0.0F + deadAngle..180.0F - deadAngle) {
        right = true
    } else if (dgs in -180.0F + deadAngle..0.0F - deadAngle) {
        left = true
    }

    return DirectionalInput(forwards, backwards, left, right)
}

fun findEdgeCollision(
    from: Vec3d,
    to: Vec3d,
    allowedDropDown: Float = 0.5F,
): Vec3d? {
    val boundingBoxes = collectCollisionBoundingBoxes(from, to, allowedDropDown)

    var currentFrom = from

    val lineVec = to.subtract(from)
    val extendedFrom = from - lineVec * 1000.0
    val extendedTo = to + lineVec * 1000.0

    while (true) {
        val boxesContainingFrom = boundingBoxes.filter { it.contains(currentFrom) }

        // If there is no bounding box containing from, we would fall off
        if (boxesContainingFrom.isEmpty()) {
            return currentFrom
        }

        // If there is a bounding box that contains from and to, we won't collide with an edge
        if (boxesContainingFrom.any { it.contains(to) }) {
            return null
        }

        currentFrom =
            boxesContainingFrom.map {
                val res = it.raycast(extendedTo, extendedFrom)

                // This ray-cast should never fail.
                res.orElseThrow { IllegalArgumentException("Raycast failed. This should be impossible.") }
            }.minBy { it.squaredDistanceTo(to) }

        boundingBoxes.removeAll(boxesContainingFrom)
    }
}

private fun collectCollisionBoundingBoxes(
    from: Vec3d,
    to: Vec3d,
    allowedDropDown: Float,
): ArrayList<Box> {
    val playerDims = mc.player!!.getDimensions(EntityPose.STANDING)

    val fromBox: Box = playerDims.getBoxAt(from)
    val toBox: Box = playerDims.getBoxAt(to)

    val unionBox = fromBox.union(toBox)

    val fromBlockPos =
        BlockPos.ofFloored(
            unionBox.minX - 0.3 - 1.0E-7,
            unionBox.minY - allowedDropDown - 1.0E-7,
            unionBox.minZ - 0.3 - 1.0E-7,
        )
    val toBlockPos =
        BlockPos.ofFloored(
            unionBox.maxX + 0.3 + 1.0E-7,
            unionBox.minY + 1.0E-7,
            unionBox.maxZ + 0.3 + 1.0E-7,
        )

    val lineVec = to.subtract(from)
    val extendedFrom = from - lineVec * 1000.0
    val extendedTo = to + lineVec * 1000.0

    val foundBoxes = ArrayList<Box>()

    val world = mc.world!!

    forEachBlockPosBetween(fromBlockPos, toBlockPos) { pos ->
        val state = world.getBlockState(pos)

        val collisionShape = state.getCollisionShape(world, pos)

        for (boundingBox in collisionShape.boundingBoxes) {
            val adjustedBox =
                Box(
                    boundingBox.minX - 0.3,
                    boundingBox.minY - 1.0,
                    boundingBox.minZ - 0.3,
                    boundingBox.maxX + 0.3,
                    boundingBox.maxY + allowedDropDown + 0.05,
                    boundingBox.maxZ + 0.3,
                ).offset(pos)

            if (adjustedBox.raycast(extendedFrom, extendedTo) == null) {
                continue
            }

            foundBoxes.add(adjustedBox)
        }
    }

    return foundBoxes
}

fun ClientPlayerEntity.zeroXZ() {
    this.velocity.x = 0.0
    this.velocity.z = 0.0
}
